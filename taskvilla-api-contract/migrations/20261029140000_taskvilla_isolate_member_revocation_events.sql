do $repair$
declare d text;
begin
 select pg_get_functiondef('taskvilla.sync_pull(uuid,text,integer)'::regprocedure) into d;
 if position('e.owner_id=v_user or e.recipient_user_id=v_user or exists (' in d)=0 then raise exception 'Unexpected sync_pull definition'; end if;
 d := replace(d,
 $old$and (e.owner_id=v_user or e.recipient_user_id=v_user or exists (
         select 1 from taskvilla.list_members m where m.list_id=e.list_id
          and m.member_user_id=v_user and m.status='accepted'))$old$,
 $new$and (e.recipient_user_id=v_user or (e.recipient_user_id is null and (e.owner_id=v_user or exists (
         select 1 from taskvilla.list_members m where m.list_id=e.list_id
          and m.member_user_id=v_user and m.status='accepted'))))$new$);
 if position('e.recipient_user_id is null' in d)=0 then raise exception 'Recipient isolation replacement failed'; end if;
 execute d;
end $repair$;
insert into taskvilla.change_events(owner_id,list_id,entity,entity_id,revision,operation,payload,modified_at)
select e.owner_id,e.list_id,e.entity,e.entity_id,e.revision,e.operation,e.payload,clock_timestamp()
from taskvilla.entities e where e.operation='upsert'
and exists(select 1 from taskvilla.change_events r where r.list_id=coalesce(e.list_id,e.entity_id) and r.recipient_user_id is not null and r.operation='delete')
order by case e.entity when 'list' then 0 else 1 end,e.entity_id;
