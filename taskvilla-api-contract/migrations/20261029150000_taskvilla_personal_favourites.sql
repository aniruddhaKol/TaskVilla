create table taskvilla.task_favourites (
 user_id uuid not null references auth.users(id) on delete cascade,
 task_id uuid not null,
 starred boolean not null default false,
 updated_at timestamptz not null default now(),
 primary key(user_id,task_id)
);
alter table taskvilla.task_favourites enable row level security;
create policy personal_favourites on taskvilla.task_favourites using (user_id=auth.uid()) with check(user_id=auth.uid());
revoke all on taskvilla.task_favourites from public,anon,authenticated;
create function taskvilla.sync_favourites(changes jsonb default '[]'::jsonb)
returns jsonb language plpgsql security definer set search_path=pg_catalog,taskvilla as $f$
declare actor uuid:=auth.uid(); item jsonb; target uuid; result jsonb;
begin
 if actor is null then raise sqlstate 'PT401' using message='Authentication required'; end if;
 if jsonb_typeof(changes) is distinct from 'array' or jsonb_array_length(changes)>500 then raise sqlstate 'PT400' using message='Invalid favourites'; end if;
 for item in select value from jsonb_array_elements(changes) loop
  target:=(item->>'taskId')::uuid;
  if jsonb_typeof(item->'starred') is distinct from 'boolean' then raise sqlstate 'PT400' using message='Invalid favourite'; end if;
  if exists(select 1 from taskvilla.entities e where e.entity='task' and e.entity_id=target and e.operation='upsert'
    and (e.owner_id=actor or exists(select 1 from taskvilla.list_members m where m.list_id=e.list_id and m.member_user_id=actor and m.status='accepted'))) then
    insert into taskvilla.task_favourites(user_id,task_id,starred) values(actor,target,(item->>'starred')::boolean)
    on conflict(user_id,task_id) do update set starred=excluded.starred,updated_at=clock_timestamp();
  end if;
 end loop;
 select coalesce(jsonb_agg(jsonb_build_object('taskId',f.task_id,'starred',f.starred)),'[]'::jsonb) into result
 from taskvilla.task_favourites f join taskvilla.entities e on e.entity='task' and e.entity_id=f.task_id and e.operation='upsert'
 where f.user_id=actor and (e.owner_id=actor or exists(select 1 from taskvilla.list_members m where m.list_id=e.list_id and m.member_user_id=actor and m.status='accepted'));
 return jsonb_build_object('favourites',result);
end $f$;
revoke all on function taskvilla.sync_favourites(jsonb) from public,anon;
grant execute on function taskvilla.sync_favourites(jsonb) to authenticated;
