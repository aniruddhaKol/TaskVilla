-- TaskVilla backend v1.2: local-first sync and accepted-list sharing.
-- All public mutations enter through authenticated SECURITY DEFINER RPCs.

create schema if not exists taskvilla;

create table taskvilla.entities (
  entity text not null check (entity in ('list', 'task', 'activity')),
  entity_id uuid not null,
  owner_id uuid not null references auth.users(id) on delete cascade,
  list_id uuid,
  revision bigint not null check (revision > 0),
  operation text not null check (operation in ('upsert', 'delete')),
  payload jsonb,
  change_id uuid not null,
  device_id uuid not null,
  modified_at timestamptz not null default clock_timestamp(),
  primary key (entity, entity_id),
  check ((operation = 'delete' and payload is null) or
         (operation = 'upsert' and jsonb_typeof(payload) = 'object')),
  check ((entity = 'list' and list_id = entity_id) or
         (entity = 'task' and list_id is not null) or
         (entity = 'activity' and list_id is null))
);
create index entities_owner_modified_idx on taskvilla.entities(owner_id, modified_at desc);
create index entities_list_modified_idx on taskvilla.entities(list_id, modified_at desc)
  where list_id is not null;

create table taskvilla.change_events (
  sequence bigint generated always as identity primary key,
  owner_id uuid not null references auth.users(id) on delete cascade,
  list_id uuid,
  entity text not null check (entity in ('list', 'task', 'activity')),
  entity_id uuid not null,
  revision bigint not null check (revision > 0),
  operation text not null check (operation in ('upsert', 'delete')),
  payload jsonb,
  modified_at timestamptz not null default clock_timestamp(),
  recipient_user_id uuid references auth.users(id) on delete cascade
);
create index change_events_list_sequence_idx on taskvilla.change_events(list_id, sequence);
create index change_events_owner_sequence_idx on taskvilla.change_events(owner_id, sequence);
create index change_events_recipient_sequence_idx on taskvilla.change_events(recipient_user_id, sequence)
  where recipient_user_id is not null;

create table taskvilla.batch_receipts (
  user_id uuid not null references auth.users(id) on delete cascade,
  device_id uuid not null,
  batch_id uuid not null,
  request_hash text not null,
  response jsonb not null,
  created_at timestamptz not null default clock_timestamp(),
  primary key (user_id, device_id, batch_id)
);
create table taskvilla.change_receipts (
  change_id uuid primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  device_id uuid not null,
  batch_id uuid not null,
  created_at timestamptz not null default clock_timestamp()
);
create index change_receipts_user_idx on taskvilla.change_receipts(user_id, created_at desc);

create table taskvilla.list_members (
  invitation_id uuid primary key default gen_random_uuid(),
  list_id uuid not null,
  owner_user_id uuid not null references auth.users(id) on delete cascade,
  invited_email text not null,
  member_user_id uuid references auth.users(id) on delete cascade,
  permission text not null check (permission in ('viewer', 'editor')),
  status text not null default 'pending' check (status in ('pending', 'accepted', 'revoked', 'declined')),
  created_at timestamptz not null default clock_timestamp(),
  accepted_at timestamptz,
  ended_at timestamptz,
  check (invited_email = lower(btrim(invited_email))),
  check ((status = 'accepted' and member_user_id is not null and accepted_at is not null) or
         (status <> 'accepted'))
);
create unique index list_members_one_open_invite_idx
  on taskvilla.list_members(list_id, invited_email) where status = 'pending';
create unique index list_members_one_active_member_idx
  on taskvilla.list_members(list_id, member_user_id) where status = 'accepted';
create index list_members_inbox_idx on taskvilla.list_members(invited_email, created_at desc)
  where status = 'pending';
create index list_members_user_idx on taskvilla.list_members(member_user_id, list_id)
  where status = 'accepted';

create table taskvilla.sync_cursors (
  cursor_id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  device_id uuid not null,
  last_sequence bigint not null default 0 check (last_sequence >= 0),
  created_at timestamptz not null default clock_timestamp(),
  expires_at timestamptz not null default (clock_timestamp() + interval '90 days')
);
create index sync_cursors_owner_device_idx on taskvilla.sync_cursors(user_id, device_id, created_at desc);

alter table taskvilla.entities enable row level security;
alter table taskvilla.change_events enable row level security;
alter table taskvilla.batch_receipts enable row level security;
alter table taskvilla.change_receipts enable row level security;
alter table taskvilla.list_members enable row level security;
alter table taskvilla.sync_cursors enable row level security;

revoke all on all tables in schema taskvilla from public, anon, authenticated, service_role;
revoke all on all sequences in schema taskvilla from public, anon, authenticated, service_role;
grant usage on schema taskvilla to anon, authenticated;
grant usage on schema taskvilla to service_role;
grant usage, select on all sequences in schema taskvilla to postgres;

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
  v_client_time timestamptz;
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
begin
  if v_user is null then
    raise sqlstate 'PT401' using message = 'Authentication required';
  end if;
  if "deviceId" is null or "batchId" is null or jsonb_typeof(changes) is distinct from 'array' then
    raise sqlstate 'PT400' using message = 'deviceId, batchId and changes array are required';
  end if;
  if jsonb_array_length(changes) > 200 then
    raise sqlstate 'PT413' using message = 'A batch may contain at most 200 changes';
  end if;
  if exists (
    select 1 from jsonb_array_elements(changes) x
    where jsonb_typeof(x) <> 'object'
       or not (x ?& array['changeId','entity','entityId','operation','baseRevision','clientModifiedAt'])
  ) then
    raise sqlstate 'PT400' using message = 'Each change is missing required fields';
  end if;

  v_hash := md5(jsonb_build_object('deviceId', "deviceId", 'batchId', "batchId", 'changes', changes)::text);
  select * into v_prior from taskvilla.batch_receipts
   where user_id = v_user and device_id = "deviceId" and batch_id = "batchId" for update;
  if found then
    if v_prior.request_hash <> v_hash then
      raise sqlstate 'PT409' using message = 'Batch id was already used with a different request';
    end if;
    return v_prior.response;
  end if;

  for v_item in select value from jsonb_array_elements(changes) loop
    v_change_id := (v_item->>'changeId')::uuid;
    v_entity := v_item->>'entity';
    v_entity_id := (v_item->>'entityId')::uuid;
    v_operation := v_item->>'operation';
    v_base_revision := case when v_item->'baseRevision' = 'null'::jsonb then null else (v_item->>'baseRevision')::bigint end;
    v_client_time := (v_item->>'clientModifiedAt')::timestamptz;
    v_payload := v_item->'payload';
    if v_entity not in ('list','task','activity') or v_operation not in ('upsert','delete') then
      raise sqlstate 'PT400' using message = 'Unsupported entity or operation';
    end if;
    if v_operation = 'upsert' and jsonb_typeof(v_payload) is distinct from 'object' then
      raise sqlstate 'PT400' using message = 'Upserts require an object payload';
    end if;
    if v_operation = 'delete' and v_payload is not null and v_payload <> 'null'::jsonb then
      raise sqlstate 'PT400' using message = 'Delete payload must be null';
    end if;

    select * into v_seen from taskvilla.change_receipts where change_id = v_change_id;
    if found then
      if v_seen.user_id <> v_user or v_seen.device_id <> "deviceId" then
        raise sqlstate 'PT409' using message = 'changeId was already used';
      end if;
      v_accepted := v_accepted || to_jsonb(v_change_id);
      continue;
    end if;

    select * into v_existing from taskvilla.entities where entity = v_entity and entity_id = v_entity_id for update;
    v_list_id := null;
    v_owner := v_user;
    v_role := null;
    if v_entity = 'list' then
      v_list_id := v_entity_id;
      if found then v_owner := v_existing.owner_id; end if;
      if v_existing.entity_id is not null and v_existing.owner_id <> v_user then
        v_role := null;
      else
        v_role := 'owner';
      end if;
    elsif v_entity = 'task' then
      if v_operation = 'upsert' then
        if nullif(v_payload->>'listId','') is null then
          raise sqlstate 'PT400' using message = 'Task upsert payload must include listId';
        end if;
        v_list_id := (v_payload->>'listId')::uuid;
      elsif v_existing.entity_id is not null then
        v_list_id := v_existing.list_id;
      end if;
      if v_list_id is not null then
        select e.owner_id into v_owner from taskvilla.entities e
         where e.entity = 'list' and e.entity_id = v_list_id and e.operation = 'upsert';
        if found then
          if v_owner = v_user then
            v_role := 'owner';
          else
            select m.permission into v_role from taskvilla.list_members m
             where m.list_id = v_list_id and m.member_user_id = v_user and m.status = 'accepted';
          end if;
        end if;
      end if;
      if v_existing.entity_id is not null and v_existing.list_id <> v_list_id then
        v_role := null;
      end if;
    else
      v_list_id := null;
      v_owner := v_user;
      v_role := 'owner';
    end if;

    if v_role is null or (v_entity = 'task' and v_role not in ('owner','editor')) then
      v_conflicts := v_conflicts || jsonb_build_array(jsonb_build_object(
        'changeId',v_change_id,'entityId',v_entity_id,'reason','permission_denied'));
      continue;
    end if;
    if v_existing.entity_id is not null then
      if v_existing.operation = 'delete' then
        v_conflicts := v_conflicts || jsonb_build_array(jsonb_build_object(
          'changeId',v_change_id,'entityId',v_entity_id,'serverRevision',v_existing.revision,
          'serverRecord',null,'reason','deleted_on_server'));
        continue;
      end if;
      if v_base_revision is null or v_base_revision <> v_existing.revision then
        v_conflicts := v_conflicts || jsonb_build_array(jsonb_build_object(
          'changeId',v_change_id,'entityId',v_entity_id,'serverRevision',v_existing.revision,
          'serverRecord',jsonb_build_object('entity',v_entity,'entityId',v_entity_id,
            'revision',v_existing.revision,'operation',v_existing.operation,'payload',v_existing.payload,
            'serverModifiedAt',v_existing.modified_at),'reason','stale_revision'));
        continue;
      end if;
      v_revision := v_existing.revision + 1;
    else
      if v_operation = 'delete' then
        v_conflicts := v_conflicts || jsonb_build_array(jsonb_build_object(
          'changeId',v_change_id,'entityId',v_entity_id,'serverRevision',0,
          'serverRecord',null,'reason','deleted_on_server'));
        continue;
      end if;
      if v_base_revision not in (null,0) then
        v_conflicts := v_conflicts || jsonb_build_array(jsonb_build_object(
          'changeId',v_change_id,'entityId',v_entity_id,'serverRevision',0,
          'serverRecord',null,'reason','stale_revision'));
        continue;
      end if;
      v_revision := 1;
    end if;

    insert into taskvilla.entities(entity,entity_id,owner_id,list_id,revision,operation,payload,change_id,device_id,modified_at)
    values(v_entity,v_entity_id,v_owner,v_list_id,v_revision,v_operation,
      case when v_operation = 'delete' then null else v_payload end,v_change_id,"deviceId",v_now)
    on conflict(entity,entity_id) do update set owner_id=excluded.owner_id,list_id=excluded.list_id,
      revision=excluded.revision,operation=excluded.operation,payload=excluded.payload,
      change_id=excluded.change_id,device_id=excluded.device_id,modified_at=excluded.modified_at;
    insert into taskvilla.change_events(owner_id,list_id,entity,entity_id,revision,operation,payload,modified_at)
    values(v_owner,v_list_id,v_entity,v_entity_id,v_revision,v_operation,
      case when v_operation='delete' then null else v_payload end,v_now);
    insert into taskvilla.change_receipts(change_id,user_id,device_id,batch_id)
    values(v_change_id,v_user,"deviceId","batchId");
    v_accepted := v_accepted || to_jsonb(v_change_id);

    if v_entity = 'list' and v_operation = 'delete' then
      for v_existing in select * from taskvilla.entities
        where entity='task' and list_id=v_entity_id and operation='upsert' for update
      loop
        update taskvilla.entities set operation='delete',payload=null,revision=v_existing.revision+1,
          modified_at=v_now,change_id=v_change_id,device_id="deviceId"
          where entity='task' and entity_id=v_existing.entity_id;
        insert into taskvilla.change_events(owner_id,list_id,entity,entity_id,revision,operation,payload,modified_at)
        values(v_existing.owner_id,v_entity_id,'task',v_existing.entity_id,v_existing.revision+1,'delete',null,v_now);
      end loop;
    end if;
  end loop;

  v_response := jsonb_build_object('batchId',"batchId",'accepted',v_accepted,'conflicts',v_conflicts,'serverTime',v_now);
  insert into taskvilla.batch_receipts(user_id,device_id,batch_id,request_hash,response)
    values(v_user,"deviceId","batchId",v_hash,v_response);
  return v_response;
end;
$function$;

create or replace function taskvilla.sync_pull("deviceId" uuid, "cursor" text, "limit" integer default 200)
returns jsonb
language plpgsql
security definer
set search_path = pg_catalog, taskvilla
as $function$
declare
  v_user uuid := auth.uid();
  v_cursor taskvilla.sync_cursors%rowtype;
  v_start bigint := 0;
  v_watermark bigint;
  v_position bigint;
  v_new_cursor uuid := gen_random_uuid();
  v_changes jsonb := '[]'::jsonb;
  v_row record;
  v_count integer := 0;
  v_more boolean := false;
begin
  if v_user is null then raise sqlstate 'PT401' using message='Authentication required'; end if;
  if "deviceId" is null or "limit" is null or "limit" < 1 or "limit" > 500 then
    raise sqlstate 'PT400' using message='deviceId and limit from 1 to 500 are required';
  end if;
  if "cursor" is not null then
    begin
      select * into v_cursor from taskvilla.sync_cursors where cursor_id="cursor"::uuid and user_id=v_user;
    exception when invalid_text_representation then
      raise sqlstate 'PT400' using message='Invalid sync cursor';
    end;
    if not found then raise sqlstate 'PT410' using message='Cursor expired or unknown; perform a safe full resync'; end if;
    if v_cursor.expires_at < clock_timestamp() then
      raise sqlstate 'PT410' using message='Cursor expired; perform a safe full resync';
    end if;
    if v_cursor.device_id <> "deviceId" then raise sqlstate 'PT403' using message='Cursor belongs to another device'; end if;
    v_start := v_cursor.last_sequence;
  end if;
  select coalesce(max(sequence),0) into v_watermark from taskvilla.change_events;
  for v_row in
    select e.* from taskvilla.change_events e
     where e.sequence > v_start and e.sequence <= v_watermark
       and (e.owner_id=v_user or e.recipient_user_id=v_user or exists (
         select 1 from taskvilla.list_members m where m.list_id=e.list_id
          and m.member_user_id=v_user and m.status='accepted'))
     order by e.sequence limit "limit" + 1
  loop
    if v_count = "limit" then v_more := true; exit; end if;
    v_count := v_count + 1;
    v_position := v_row.sequence;
    v_changes := v_changes || jsonb_build_array(jsonb_strip_nulls(jsonb_build_object(
      'entity',v_row.entity,'entityId',v_row.entity_id,'revision',v_row.revision,
      'operation',v_row.operation,'payload',v_row.payload,'serverModifiedAt',v_row.modified_at)));
  end loop;
  if not v_more then v_position := v_watermark; end if;
  insert into taskvilla.sync_cursors(cursor_id,user_id,device_id,last_sequence)
  values(v_new_cursor,v_user,"deviceId",coalesce(v_position, v_start));
  return jsonb_build_object('changes',v_changes,'nextCursor',v_new_cursor::text,'hasMore',v_more);
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
  v_inv taskvilla.list_members%rowtype;
  v_list_owner uuid;
  v_result jsonb;
  v_members jsonb := '[]'::jsonb;
  v_invites jsonb := '[]'::jsonb;
  v_target uuid;
begin
  if v_user is null then raise sqlstate 'PT401' using message='Authentication required'; end if;
  if action not in ('invite','accept','revoke','decline','list') then
    raise sqlstate 'PT400' using message='Unsupported sharing action';
  end if;
  select lower(email) into v_email from auth.users where id=v_user and email_confirmed_at is not null;
  if v_email is null then raise sqlstate 'PT403' using message='A verified email account is required'; end if;

  if action='invite' then
    if "listId" is null or email is null or permission not in ('viewer','editor') then
      raise sqlstate 'PT400' using message='listId, email and viewer/editor permission are required';
    end if;
    if lower(btrim(email)) !~ '^[^[:space:]@]+@[^[:space:]@]+\.[^[:space:]@]+$' then
      raise sqlstate 'PT400' using message='A valid email address is required';
    end if;
    select owner_id into v_list_owner from taskvilla.entities
      where entity='list' and entity_id="listId" and operation='upsert';
    if not found then raise sqlstate 'PT404' using message='List not found'; end if;
    if v_list_owner<>v_user then raise sqlstate 'PT403' using message='Only the list owner can invite people'; end if;
    if lower(btrim(email))=v_email then raise sqlstate 'PT400' using message='You cannot invite your own email'; end if;
    select * into v_inv from taskvilla.list_members where list_id="listId"
      and invited_email=lower(btrim(email)) and status='pending' for update;
    if not found then
      if exists(select 1 from taskvilla.list_members where list_id="listId"
          and invited_email=lower(btrim(email)) and status='accepted') then
        raise sqlstate 'PT409' using message='That person already has access';
      end if;
      insert into taskvilla.list_members(list_id,owner_user_id,invited_email,permission)
        values("listId",v_user,lower(btrim(email)),permission) returning * into v_inv;
    else
      update taskvilla.list_members set permission=manage_sharing.permission,owner_user_id=v_user
        where invitation_id=v_inv.invitation_id returning * into v_inv;
    end if;
    return jsonb_build_object('status','invited','invitationId',v_inv.invitation_id);
  elsif action in ('accept','decline') then
    if "inviteToken" is null then raise sqlstate 'PT400' using message='inviteToken is required'; end if;
    begin
      select * into v_inv from taskvilla.list_members where invitation_id=("inviteToken")::uuid for update;
    exception when invalid_text_representation then
      raise sqlstate 'PT404' using message='Invitation not found';
    end;
    if not found or v_inv.status<>'pending' then raise sqlstate 'PT404' using message='Invitation not found or no longer pending'; end if;
    if v_inv.invited_email<>v_email then raise sqlstate 'PT403' using message='Invitation belongs to another email address'; end if;
    if action='decline' then
      update taskvilla.list_members set status='declined',ended_at=clock_timestamp()
        where invitation_id=v_inv.invitation_id;
      return jsonb_build_object('status','declined','invitationId',v_inv.invitation_id);
    end if;
    update taskvilla.list_members set status='accepted',member_user_id=v_user,accepted_at=clock_timestamp()
      where invitation_id=v_inv.invitation_id;
    -- Publish an initial snapshot after acceptance, so an existing device cursor sees the list.
    insert into taskvilla.change_events(owner_id,list_id,entity,entity_id,revision,operation,payload,modified_at)
      select e.owner_id,e.list_id,e.entity,e.entity_id,e.revision,e.operation,e.payload,clock_timestamp()
        from taskvilla.entities e where (e.entity='list' and e.entity_id=v_inv.list_id)
          or (e.entity='task' and e.list_id=v_inv.list_id)
        order by case when e.entity='list' then 0 else 1 end,e.modified_at,e.entity_id;
    return jsonb_build_object('status','accepted','invitationId',v_inv.invitation_id);
  elsif action='revoke' then
    if "listId" is null then raise sqlstate 'PT400' using message='listId is required'; end if;
    select owner_id into v_list_owner from taskvilla.entities
      where entity='list' and entity_id="listId";
    if not found then raise sqlstate 'PT404' using message='List not found'; end if;
    if v_list_owner<>v_user and "memberUserId"<>v_user then
      raise sqlstate 'PT403' using message='Only the owner may remove another member';
    end if;
    if "memberUserId" is not null and "memberUserId"<>v_user then
      update taskvilla.list_members set status='revoked',ended_at=clock_timestamp()
       where list_id="listId" and member_user_id="memberUserId" and status='accepted'
       returning * into v_inv;
    elsif "inviteToken" is not null and v_list_owner=v_user then
      update taskvilla.list_members set status='revoked',ended_at=clock_timestamp()
       where list_id="listId" and invitation_id=("inviteToken")::uuid and status='pending'
       returning * into v_inv;
    else
      update taskvilla.list_members set status='revoked',ended_at=clock_timestamp()
       where list_id="listId" and member_user_id=v_user and status='accepted'
       returning * into v_inv;
    end if;
    if not found then raise sqlstate 'PT404' using message='Member or invitation not found'; end if;
    if v_inv.member_user_id is not null then
      insert into taskvilla.change_events(owner_id,list_id,entity,entity_id,revision,operation,payload,modified_at,recipient_user_id)
       select e.owner_id,e.list_id,e.entity,e.entity_id,e.revision,'delete',null,clock_timestamp(),v_inv.member_user_id
        from taskvilla.entities e where (e.entity='list' and e.entity_id="listId")
          or (e.entity='task' and e.list_id="listId") order by e.entity,e.entity_id;
    end if;
    return jsonb_build_object('status','revoked','invitationId',v_inv.invitation_id);
  else
    if "listId" is null then
      select coalesce(jsonb_agg(jsonb_build_object('invitationId',m.invitation_id,
        'listId',m.list_id,'inviterUserId',m.owner_user_id,'permission',m.permission,'createdAt',m.created_at)
        order by m.created_at desc),'[]'::jsonb) into v_invites
        from taskvilla.list_members m where m.invited_email=v_email and m.status='pending';
      v_result := jsonb_build_object('status','listed','members',v_members,'invitations',v_invites);
      return v_result;
    end if;
    select owner_id into v_list_owner from taskvilla.entities
      where entity='list' and entity_id="listId";
    if not found then raise sqlstate 'PT404' using message='List not found'; end if;
    if v_list_owner<>v_user and not exists(select 1 from taskvilla.list_members
        where list_id="listId" and member_user_id=v_user and status='accepted') then
      raise sqlstate 'PT403' using message='List is not shared with this account';
    end if;
    select jsonb_agg(jsonb_build_object('userId',z.user_id,'permission',z.permission)
      order by case when z.permission='owner' then 0 else 1 end,z.user_id) into v_members
      from (select v_list_owner as user_id,'owner'::text as permission
            union all select m.member_user_id,m.permission from taskvilla.list_members m
             where m.list_id="listId" and m.status='accepted') z;
    if v_list_owner=v_user then
      select coalesce(jsonb_agg(jsonb_build_object('invitationId',m.invitation_id,
        'listId',m.list_id,'inviterUserId',m.owner_user_id,'permission',m.permission,'createdAt',m.created_at)
        order by m.created_at desc),'[]'::jsonb) into v_invites
        from taskvilla.list_members m where m.list_id="listId" and m.status='pending';
    end if;
    return jsonb_build_object('status','listed','members',coalesce(v_members,'[]'::jsonb),'invitations',v_invites);
  end if;
end;
$function$;

revoke all on function taskvilla.sync_push(uuid,uuid,jsonb) from public, anon;
revoke all on function taskvilla.sync_pull(uuid,text,integer) from public, anon;
revoke all on function taskvilla.manage_sharing(text,uuid,text,text,uuid,text) from public, anon;
grant execute on function taskvilla.sync_push(uuid,uuid,jsonb) to authenticated;
grant execute on function taskvilla.sync_pull(uuid,text,integer) to authenticated;
grant execute on function taskvilla.manage_sharing(text,uuid,text,text,uuid,text) to authenticated;

alter default privileges for role postgres in schema taskvilla
  revoke all on tables from public, anon, authenticated, service_role;
alter default privileges for role postgres in schema taskvilla
  revoke execute on functions from public, anon, authenticated, service_role;

notify pgrst, 'reload schema';
