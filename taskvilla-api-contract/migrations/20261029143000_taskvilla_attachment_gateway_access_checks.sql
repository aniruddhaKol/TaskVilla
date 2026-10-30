-- Only the server-side attachment gateway needs direct reads for task/member authorization.
grant select on taskvilla.entities, taskvilla.list_members to service_role;
