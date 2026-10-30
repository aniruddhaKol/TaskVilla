-- TaskVilla direct list sharing: verified-email membership, no invite/accept step.
-- Members may only change task completion and append comments.

update taskvilla.list_members set permission='editor' where permission='viewer';

create or replace function taskvilla.sync_push("deviceId" uuid, "batchId" uuid, changes jsonb)
returns jsonb
language plpgsql
security definer
set search_path = pg_catalog, taskvilla
as $function$
declare
  v_user uuid := auth.uid();
  v_hash text;
  v_prior taskvilla.batch_receipts%rowtype;
  v_item jsonb;
  v_change_id uuid;
  v_entity text;
  v_entity_id uuid;
  v_operation text;
  v_base_revision bigint;
  v_payload jsonb;
  v_list_id uuid;
  v_owner uuid;
  v_role text;
  v_existing taskvilla.entities%rowtype;
  v_seen taskvilla.change_receipts%rowtype;
  v_accepted jsonb := '[]'::jsonb;
  v_conflicts jsonb := '[]'::jsonb;
  v_response jsonb;
  v_revision bigint;
  v_now timestamptz := clock_timestamp();
  v_stale_member_change boolean;
  v_comments jsonb;
  v_added_comments jsonb;
  v_actor_email text;
begin
  if v_user is null then raise sqlstate 'PT401' using message = 'Authentication required'; end if;
  select lower(email) into v_actor_email from auth.users where id=v_user;
  if "deviceId" is null or "batchId" is null or jsonb_typeof(changes) is distinct from 'array' then
    raise sqlstate 'PT400' using message = 'deviceId, batchId and changes array are required';
  end if;
  if jsonb_array_length(changes) > 200 then raise sqlstate 'PT413' using message = 'A batch may contain at most 200 changes'; end if;
  if exists (select 1 from jsonb_array_elements(changes) x
      where jsonb_typeof(x) <> 'object'
         or not (x ?& array['changeId','entity','entityId','operation','baseRevision','clientModifiedAt'])) then
    raise sqlstate 'PT400' using message = 'Each change is missing required fields';
  end if;

  v_hash := md5(jsonb_build_object('deviceId', "deviceId", 'batchId', "batchId", 'changes', changes)::text);
  perform pg_catalog.pg_advisory_xact_lock(pg_catalog.hashtextextended('taskvilla:batch:' || v_user::text || ':' || "deviceId"::text || ':' || "batchId"::text, 0));
  select * into v_prior from taskvilla.batch_receipts
   where user_id = v_user and device_id = "deviceId" and batch_id = "batchId" for update;
  if found then
    if v_prior.request_hash <> v_hash then raise sqlstate 'PT409' using message = 'Batch id was already used with a different request'; end if;
    return v_prior.response;
  end if;

  for v_item in select value from jsonb_array_elements(changes) loop
    v_change_id := (v_item->>'changeId')::uuid;
    v_entity := v_item->>'entity';
    v_entity_id := (v_item->>'entityId')::uuid;
    v_operation := v_item->>'operation';
    v_base_revision := case when v_item->'baseRevision' = 'null'::jsonb then null else (v_item->>'baseRevision')::bigint end;
    v_payload := v_item->'payload';
    if v_entity not in ('list','task','activity') or v_operation not in ('upsert','delete') then raise sqlstate 'PT400' using message = 'Unsupported entity or operation'; end if;
    if v_operation = 'upsert' and jsonb_typeof(v_payload) is distinct from 'object' then raise sqlstate 'PT400' using message = 'Upserts require an object payload'; end if;
    if v_operation = 'delete' and v_payload is not null and v_payload <> 'null'::jsonb then raise sqlstate 'PT400' using message = 'Delete payload must be null'; end if;

    perform pg_catalog.pg_advisory_xact_lock(pg_catalog.hashtextextended('taskvilla:change:' || v_change_id::text, 0));
    select * into v_seen from taskvilla.change_receipts where change_id = v_change_id;
    if found then
      if v_seen.user_id <> v_user or v_seen.device_id <> "deviceId" then raise sqlstate 'PT409' using message = 'changeId was already used'; end if;
      v_accepted := v_accepted || to_jsonb(v_change_id);
      continue;
    end if;

    select * into v_existing from taskvilla.entities where entity = v_entity and entity_id = v_entity_id for update;
    v_list_id := null; v_owner := v_user; v_role := null;
    if v_entity = 'list' then
      v_list_id := v_entity_id;
      if found then
        v_owner := v_existing.owner_id;
        if v_existing.owner_id = v_user then v_role := 'owner'; end if;
      else v_role := 'owner';
      end if;
    elsif v_entity = 'task' then
      if v_operation = 'upsert' then
        if nullif(v_payload->>'listId','') is null then raise sqlstate 'PT400' using message = 'Task upsert payload must include listId'; end if;
        v_list_id := (v_payload->>'listId')::uuid;
      elsif v_existing.entity_id is not null then v_list_id := v_existing.list_id;
      end if;
      if v_existing.entity_id is not null and v_existing.list_id is distinct from v_list_id then v_role := null;
      elsif v_list_id is not null then
        select e.owner_id into v_owner from taskvilla.entities e
         where e.entity = 'list' and e.entity_id = v_list_id and e.operation = 'upsert';
        if found then
          if v_owner = v_user then v_role := 'owner';
          elsif exists(select 1 from taskvilla.list_members m where m.list_id=v_list_id and m.member_user_id=v_user and m.status='accepted') then v_role := 'member';
          end if;
        end if;
      end if;
    else
      v_list_id := null; v_owner := v_user; v_role := 'owner';
    end if;

    if v_role is null
       or (v_entity <> 'task' and v_role <> 'owner')
       or (v_role = 'member' and (v_entity <> 'task' or v_operation <> 'upsert' or v_existing.entity_id is null)) then
      v_conflicts := v_conflicts || jsonb_build_array(jsonb_build_object('changeId',v_change_id,'entityId',v_entity_id,'reason','permission_denied'));
      continue;
    end if;

    v_stale_member_change := false;
    if v_existing.entity_id is not null then
      if v_existing.operation = 'delete' then
        v_conflicts := v_conflicts || jsonb_build_array(jsonb_build_object('changeId',v_change_id,'entityId',v_entity_id,'serverRevision',v_existing.revision,'serverRecord',null,'reason','deleted_on_server'));
        continue;
      end if;
      if v_base_revision is null or v_base_revision <> v_existing.revision then
        if v_role='member' and (v_payload - 'done' - 'comments') = (v_existing.payload - 'done' - 'comments') then
          v_stale_member_change := true;
        else
          v_conflicts := v_conflicts || jsonb_build_array(jsonb_build_object('changeId',v_change_id,'entityId',v_entity_id,'serverRevision',v_existing.revision,
            'serverRecord',jsonb_build_object('entity',v_entity,'entityId',v_entity_id,'revision',v_existing.revision,'operation',v_existing.operation,'payload',v_existing.payload,'serverModifiedAt',v_existing.modified_at),'reason','stale_revision'));
          continue;
        end if;
      end if;
      v_revision := v_existing.revision + 1;
    else
      if v_operation = 'delete' then
        v_conflicts := v_conflicts || jsonb_build_array(jsonb_build_object('changeId',v_change_id,'entityId',v_entity_id,'serverRevision',0,'serverRecord',null,'reason','deleted_on_server'));
        continue;
      end if;
      if v_base_revision not in (null,0) then
        v_conflicts := v_conflicts || jsonb_build_array(jsonb_build_object('changeId',v_change_id,'entityId',v_entity_id,'serverRevision',0,'serverRecord',null,'reason','stale_revision'));
        continue;
      end if;
      v_revision := 1;
    end if;

    if v_role='member' then
      if (v_payload - 'done' - 'comments') is distinct from (v_existing.payload - 'done' - 'comments')
         or jsonb_typeof(coalesce(v_payload->'comments','[]'::jsonb)) <> 'array'
         or exists(select 1 from jsonb_array_elements(coalesce(v_payload->'comments','[]'::jsonb)) c(value)
                   where jsonb_typeof(c.value)<>'object' or nullif(c.value->>'id','') is null or nullif(c.value->>'text','') is null) then
        v_conflicts := v_conflicts || jsonb_build_array(jsonb_build_object('changeId',v_change_id,'entityId',v_entity_id,'reason','permission_denied'));
        continue;
      end if;
      v_comments := coalesce(v_existing.payload->'comments','[]'::jsonb);
      select coalesce(jsonb_agg(jsonb_set(jsonb_set(c.value,'{authorUserId}',to_jsonb(v_user::text),true),'{authorEmail}',to_jsonb(v_actor_email),true) order by c.ordinality),'[]'::jsonb)
        into v_added_comments
        from jsonb_array_elements(coalesce(v_payload->'comments','[]'::jsonb)) with ordinality c(value,ordinality)
       where nullif(c.value->>'id','') is not null
         and not exists(select 1 from jsonb_array_elements(v_comments) oldc(value) where oldc.value->>'id'=c.value->>'id');
      v_payload := jsonb_set(v_payload,'{comments}',v_comments || v_added_comments,true);
    end if;

    insert into taskvilla.entities(entity,entity_id,owner_id,list_id,revision,operation,payload,change_id,device_id,modified_at)
    values(v_entity,v_entity_id,v_owner,v_list_id,v_revision,v_operation,case when v_operation='delete' then null else v_payload end,v_change_id,"deviceId",v_now)
    on conflict(entity,entity_id) do update set owner_id=excluded.owner_id,list_id=excluded.list_id,revision=excluded.revision,operation=excluded.operation,payload=excluded.payload,change_id=excluded.change_id,device_id=excluded.device_id,modified_at=excluded.modified_at;
    insert into taskvilla.change_events(owner_id,list_id,entity,entity_id,revision,operation,payload,modified_at)
    values(v_owner,v_list_id,v_entity,v_entity_id,v_revision,v_operation,case when v_operation='delete' then null else v_payload end,v_now);
    insert into taskvilla.change_receipts(change_id,user_id,device_id,batch_id) values(v_change_id,v_user,"deviceId","batchId");
    v_accepted := v_accepted || to_jsonb(v_change_id);

    if v_entity='list' and v_operation='delete' then
      for v_existing in select * from taskvilla.entities where entity='task' and list_id=v_entity_id and operation='upsert' for update loop
        update taskvilla.entities set operation='delete',payload=null,revision=v_existing.revision+1,modified_at=v_now,change_id=v_change_id,device_id="deviceId" where entity='task' and entity_id=v_existing.entity_id;
        insert into taskvilla.change_events(owner_id,list_id,entity,entity_id,revision,operation,payload,modified_at) values(v_existing.owner_id,v_entity_id,'task',v_existing.entity_id,v_existing.revision+1,'delete',null,v_now);
      end loop;
    end if;
  end loop;

  v_response := jsonb_build_object('batchId',"batchId",'accepted',v_accepted,'conflicts',v_conflicts,'serverTime',v_now);
  insert into taskvilla.batch_receipts(user_id,device_id,batch_id,request_hash,response) values(v_user,"deviceId","batchId",v_hash,v_response);
  return v_response;
end;
$function$;

create or replace function taskvilla.manage_sharing(
  action text, "listId" uuid default null, email text default null,
  "inviteToken" text default null, "memberUserId" uuid default null, permission text default null)
returns jsonb
language plpgsql
security definer
set search_path = pg_catalog, taskvilla
as $function$
declare
  v_user uuid := auth.uid();
  v_email text;
  v_target_email text;
  v_target uuid;
  v_inv taskvilla.list_members%rowtype;
  v_list_owner uuid;
  v_members jsonb := '[]'::jsonb;
  v_shares jsonb := '[]'::jsonb;
  v_member_email text;
  v_task taskvilla.entities%rowtype;
  v_payload jsonb;
  v_now timestamptz;
  v_new_access boolean := false;
begin
  if v_user is null then raise sqlstate 'PT401' using message='Authentication required'; end if;
  if action not in ('share','invite','revoke','list') then raise sqlstate 'PT400' using message='Unsupported sharing action'; end if;
  select lower(u.email) into v_email from auth.users u where u.id=v_user and u.email_confirmed_at is not null;
  if v_email is null then raise sqlstate 'PT403' using message='A verified email account is required'; end if;

  if action in ('share','invite') then
    v_target_email := lower(btrim(email));
    if "listId" is null or v_target_email is null or v_target_email !~ '^[^[:space:]@]+@[^[:space:]@]+\.[^[:space:]@]+$' then
      raise sqlstate 'PT400' using message='A list and valid email address are required';
    end if;
    select owner_id into v_list_owner from taskvilla.entities where entity='list' and entity_id="listId" and operation='upsert';
    if not found then raise sqlstate 'PT404' using message='List not found'; end if;
    if v_list_owner<>v_user then raise sqlstate 'PT403' using message='Only the list owner can share it'; end if;
    if v_target_email=v_email then raise sqlstate 'PT400' using message='You cannot share a list with yourself'; end if;
    select u.id into v_target from auth.users u where lower(u.email)=v_target_email and u.email_confirmed_at is not null order by u.created_at limit 1;
    if v_target is not null then
      select * into v_inv from taskvilla.list_members where list_id="listId" and member_user_id=v_target and status='accepted' for update;
      if found then return jsonb_build_object('status','shared','email',v_target_email,'memberUserId',v_target); end if;
      select * into v_inv from taskvilla.list_members where list_id="listId" and invited_email=v_target_email and status='pending' for update;
      if found then
        update taskvilla.list_members set owner_user_id=v_user,member_user_id=v_target,status='accepted',permission='editor',accepted_at=clock_timestamp(),ended_at=null where invitation_id=v_inv.invitation_id returning * into v_inv;
      else
        insert into taskvilla.list_members(list_id,owner_user_id,invited_email,member_user_id,permission,status,accepted_at)
        values("listId",v_user,v_target_email,v_target,'editor','accepted',clock_timestamp()) returning * into v_inv;
      end if;
      v_new_access := true;
    else
      select * into v_inv from taskvilla.list_members where list_id="listId" and invited_email=v_target_email and status='pending' for update;
      if found then
        update taskvilla.list_members set owner_user_id=v_user,permission='editor' where invitation_id=v_inv.invitation_id returning * into v_inv;
      else
        insert into taskvilla.list_members(list_id,owner_user_id,invited_email,permission,status)
        values("listId",v_user,v_target_email,'editor','pending') returning * into v_inv;
      end if;
      return jsonb_build_object('status','pending','email',v_target_email,'shareId',v_inv.invitation_id);
    end if;
    if v_new_access then
      insert into taskvilla.change_events(owner_id,list_id,entity,entity_id,revision,operation,payload,modified_at)
      select e.owner_id,e.list_id,e.entity,e.entity_id,e.revision,e.operation,e.payload,clock_timestamp()
        from taskvilla.entities e where (e.entity='list' and e.entity_id="listId" and e.operation='upsert')
          or (e.entity='task' and e.list_id="listId" and e.operation='upsert')
        order by case when e.entity='list' then 0 else 1 end,e.modified_at,e.entity_id;
    end if;
    return jsonb_build_object('status','shared','email',v_target_email,'memberUserId',v_target);
  elsif action='revoke' then
    if "listId" is null then raise sqlstate 'PT400' using message='listId is required'; end if;
    select owner_id into v_list_owner from taskvilla.entities where entity='list' and entity_id="listId";
    if not found then raise sqlstate 'PT404' using message='List not found'; end if;
    if v_list_owner<>v_user then raise sqlstate 'PT403' using message='Only the list owner can remove a member'; end if;
    if "memberUserId" is not null then
      update taskvilla.list_members set status='revoked',ended_at=clock_timestamp()
       where list_id="listId" and member_user_id="memberUserId" and status='accepted' returning * into v_inv;
    elsif "inviteToken" is not null then
      update taskvilla.list_members set status='revoked',ended_at=clock_timestamp()
       where list_id="listId" and invitation_id="inviteToken"::uuid and status='pending' returning * into v_inv;
    elsif email is not null then
      update taskvilla.list_members set status='revoked',ended_at=clock_timestamp()
       where list_id="listId" and invited_email=lower(btrim(email)) and status='pending' returning * into v_inv;
    else raise sqlstate 'PT400' using message='memberUserId or shareId is required';
    end if;
    if not found then raise sqlstate 'PT404' using message='Shared person not found'; end if;
    v_member_email := v_inv.invited_email;
    if v_inv.member_user_id is not null then
      select lower(u.email) into v_member_email from auth.users u where u.id=v_inv.member_user_id;
      if v_member_email is not null then
        for v_task in select * from taskvilla.entities e where e.entity='task' and e.list_id="listId" and e.operation='upsert'
          and lower(coalesce(e.payload->>'who',''))=v_member_email for update loop
          v_payload := jsonb_set(v_task.payload,'{who}',to_jsonb(''::text),true);
          v_now := clock_timestamp();
          update taskvilla.entities set payload=v_payload,revision=v_task.revision+1,modified_at=v_now,change_id=gen_random_uuid()
           where entity='task' and entity_id=v_task.entity_id;
          insert into taskvilla.change_events(owner_id,list_id,entity,entity_id,revision,operation,payload,modified_at)
          values(v_task.owner_id,"listId",'task',v_task.entity_id,v_task.revision+1,'upsert',v_payload,v_now);
        end loop;
      end if;
      insert into taskvilla.change_events(owner_id,list_id,entity,entity_id,revision,operation,payload,modified_at,recipient_user_id)
      select e.owner_id,e.list_id,e.entity,e.entity_id,e.revision,'delete',null,clock_timestamp(),v_inv.member_user_id
        from taskvilla.entities e where (e.entity='list' and e.entity_id="listId") or (e.entity='task' and e.list_id="listId")
       order by e.entity,e.entity_id;
    end if;
    return jsonb_build_object('status','revoked','email',v_member_email,'memberUserId',v_inv.member_user_id,'shareId',v_inv.invitation_id);
  else
    -- A signed-in user automatically receives any shares sent to their verified email.
    for v_inv in select * from taskvilla.list_members m where m.invited_email=v_email and m.status='pending' for update loop
      update taskvilla.list_members set status='accepted',member_user_id=v_user,accepted_at=clock_timestamp(),ended_at=null
       where invitation_id=v_inv.invitation_id;
      insert into taskvilla.change_events(owner_id,list_id,entity,entity_id,revision,operation,payload,modified_at)
      select e.owner_id,e.list_id,e.entity,e.entity_id,e.revision,e.operation,e.payload,clock_timestamp()
        from taskvilla.entities e where (e.entity='list' and e.entity_id=v_inv.list_id and e.operation='upsert')
          or (e.entity='task' and e.list_id=v_inv.list_id and e.operation='upsert')
        order by case when e.entity='list' then 0 else 1 end,e.modified_at,e.entity_id;
    end loop;
    if "listId" is null then
      -- An existing member may have advanced their incremental cursor before the share
      -- was created. Re-emit the current snapshot once per member/entity revision so
      -- that their next incremental pull can discover the list and its tasks.
      insert into taskvilla.change_events(owner_id,list_id,entity,entity_id,revision,operation,payload,modified_at,recipient_user_id)
      select e.owner_id,e.list_id,e.entity,e.entity_id,e.revision,e.operation,e.payload,clock_timestamp(),v_user
        from taskvilla.entities e
        join taskvilla.list_members m on m.list_id=e.list_id and m.member_user_id=v_user and m.status='accepted'
       where e.operation='upsert'
         and not exists (
           select 1 from taskvilla.change_events ce
            where ce.recipient_user_id=v_user and ce.entity=e.entity
              and ce.entity_id=e.entity_id and ce.revision=e.revision and ce.operation='upsert'
         )
       order by case when e.entity='list' then 0 else 1 end,e.modified_at,e.entity_id;
      select coalesce(jsonb_agg(jsonb_build_object('listId',m.list_id,'email',m.invited_email,'permission',m.permission) order by m.created_at desc),'[]'::jsonb)
        into v_shares from taskvilla.list_members m where m.member_user_id=v_user and m.status='accepted';
      return jsonb_build_object('status','listed','sharedLists',v_shares,'members','[]'::jsonb,'shares','[]'::jsonb);
    end if;
    select owner_id into v_list_owner from taskvilla.entities where entity='list' and entity_id="listId" and operation='upsert';
    if not found then raise sqlstate 'PT404' using message='List not found'; end if;
    if v_list_owner<>v_user and not exists(select 1 from taskvilla.list_members where list_id="listId" and member_user_id=v_user and status='accepted') then
      raise sqlstate 'PT403' using message='List is not shared with this account';
    end if;
    select coalesce(jsonb_agg(jsonb_build_object('userId',z.user_id,'email',z.email,'permission',z.permission) order by case when z.permission='owner' then 0 else 1 end,z.email),'[]'::jsonb)
      into v_members from (
        select v_list_owner as user_id,lower(u.email) as email,'owner'::text as permission from auth.users u where u.id=v_list_owner
        union all
        select m.member_user_id,lower(u.email),m.permission from taskvilla.list_members m join auth.users u on u.id=m.member_user_id
         where m.list_id="listId" and m.status='accepted'
      ) z;
      select coalesce(jsonb_agg(jsonb_build_object('shareId',m.invitation_id,'email',m.invited_email,'permission',m.permission,'createdAt',m.created_at) order by m.created_at desc),'[]'::jsonb)
      into v_shares from taskvilla.list_members m where m.list_id="listId" and m.status='pending';
    return jsonb_build_object('status','listed','members',v_members,'shares',v_shares);
  end if;
end;
$function$;

revoke all on function taskvilla.sync_push(uuid,uuid,jsonb) from public, anon;
revoke all on function taskvilla.manage_sharing(text,uuid,text,text,uuid,text) from public, anon;
grant execute on function taskvilla.sync_push(uuid,uuid,jsonb) to authenticated;
grant execute on function taskvilla.manage_sharing(text,uuid,text,text,uuid,text) to authenticated;
