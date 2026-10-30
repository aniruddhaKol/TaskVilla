do $repair$
declare d text;
begin
 select pg_get_functiondef('taskvilla.sync_push(uuid,uuid,jsonb)'::regprocedure) into d;
 if position('v_list_id := null; v_owner := v_user; v_role := ''owner'';' in d)=0 then raise exception 'Unexpected activity branch'; end if;
 d:=replace(d,'v_list_id := null; v_owner := v_user; v_role := ''owner'';',
 'v_list_id := null; v_owner := v_user; v_role := ''owner''; if v_existing.entity_id is not null and v_existing.owner_id <> v_user then v_role := null; end if;');
 execute d;
end $repair$;
