-- Let PostgREST resolve these RPCs for anonymous requests so the functions can
-- return the contract's explicit HTTP 401. Each function checks auth.uid()
-- before reading or changing any application data.
grant execute on function taskvilla.sync_push(uuid,uuid,jsonb) to anon;
grant execute on function taskvilla.sync_pull(uuid,text,integer) to anon;
grant execute on function taskvilla.manage_sharing(text,uuid,text,text,uuid,text) to anon;
notify pgrst, 'reload schema';
