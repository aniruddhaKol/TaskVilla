do $repair$
declare d text;
begin
 select pg_get_functiondef('taskvilla.sync_pull(uuid,text,integer)'::regprocedure) into d;
 if position('e.recipient_user_id=v_user or (e.recipient_user_id is null' in d)=0 then raise exception 'Unexpected sync pull visibility rule'; end if;
 d:=replace(d,
 $old$and (e.recipient_user_id=v_user or (e.recipient_user_id is null and (e.owner_id=v_user or exists (
         select 1 from taskvilla.list_members m where m.list_id=e.list_id
          and m.member_user_id=v_user and m.status='accepted'))))$old$,
 $new$and ((e.recipient_user_id=v_user and e.operation='delete')
         or ((e.recipient_user_id is null or e.recipient_user_id=v_user)
           and (e.owner_id=v_user or exists (
             select 1 from taskvilla.list_members m where m.list_id=e.list_id
              and m.member_user_id=v_user and m.status='accepted'))))$new$);
 if position('e.recipient_user_id=v_user and e.operation=''delete''' in d)=0 then raise exception 'Visibility replacement failed'; end if;
 execute d;
end $repair$;
