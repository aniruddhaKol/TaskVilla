-- Expose only TaskVilla's RPC schema; retain the project's current API schemas.
alter role authenticator set pgrst.db_schemas = 'public,graphql_public,taskvilla';
notify pgrst, 'reload config';

-- Defense in depth: client roles have no direct table path even after schema exposure.
create policy deny_client_access on taskvilla.entities
  for all to anon, authenticated using (false) with check (false);
create policy deny_client_access on taskvilla.change_events
  for all to anon, authenticated using (false) with check (false);
create policy deny_client_access on taskvilla.batch_receipts
  for all to anon, authenticated using (false) with check (false);
create policy deny_client_access on taskvilla.change_receipts
  for all to anon, authenticated using (false) with check (false);
create policy deny_client_access on taskvilla.list_members
  for all to anon, authenticated using (false) with check (false);
create policy deny_client_access on taskvilla.sync_cursors
  for all to anon, authenticated using (false) with check (false);

create index list_members_owner_idx on taskvilla.list_members(owner_user_id, created_at desc);
