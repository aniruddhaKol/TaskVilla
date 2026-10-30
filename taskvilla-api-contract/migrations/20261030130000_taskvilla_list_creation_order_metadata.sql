do $repair$
declare d text;
begin
 select pg_get_functiondef('taskvilla.sync_pull(uuid,text,integer)'::regprocedure) into d;
 if position('''operation'',v_row.operation,''payload'',v_row.payload' in d)=0 then raise exception 'Unexpected sync pull payload expression'; end if;
 d:=replace(d,
 $old$'operation',v_row.operation,'payload',v_row.payload$old$,
 $new$'operation',v_row.operation,'payload',case when v_row.entity='list' and v_row.operation='upsert' then v_row.payload || jsonb_build_object('createdAt',coalesce(nullif(v_row.payload->>'createdAt',''),(select to_char(min(c.modified_at) at time zone 'UTC','YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') from taskvilla.change_events c where c.entity='list' and c.entity_id=v_row.entity_id and c.operation='upsert'))) else v_row.payload end$new$);
 execute d;
end $repair$;
