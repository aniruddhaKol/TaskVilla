create or replace function taskvilla.enforce_task_attachment_limit()
returns trigger
language plpgsql
security definer
set search_path = pg_catalog, taskvilla
as $$
declare
  active_count integer;
begin
  perform pg_advisory_xact_lock(hashtextextended(new.task_id::text, 0));
  select count(*) into active_count
  from taskvilla.task_attachments
  where task_id = new.task_id and status <> 'deleted';
  if active_count >= 10 then
    raise exception using errcode = '23514', message = 'Maximum 10 attachments per task';
  end if;
  return new;
end;
$$;

revoke all on function taskvilla.enforce_task_attachment_limit() from public, anon, authenticated;
drop trigger if exists taskvilla_enforce_attachment_limit on taskvilla.task_attachments;
create trigger taskvilla_enforce_attachment_limit
before insert on taskvilla.task_attachments
for each row execute function taskvilla.enforce_task_attachment_limit();
