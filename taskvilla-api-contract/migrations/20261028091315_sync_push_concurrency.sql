-- Serialize concurrent retries for the same batch and change IDs.
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
  perform pg_catalog.pg_advisory_xact_lock(pg_catalog.hashtextextended('taskvilla:batch:' || v_user::text || ':' || "deviceId"::text || ':' || "batchId"::text, 0));
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

    perform pg_catalog.pg_advisory_xact_lock(pg_catalog.hashtextextended('taskvilla:change:' || v_change_id::text, 0));
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

