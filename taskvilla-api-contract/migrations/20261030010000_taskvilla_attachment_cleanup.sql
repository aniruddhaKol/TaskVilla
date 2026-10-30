-- Queue provider cleanup only after an accepted task/list tombstone is stored.
create or replace function taskvilla.queue_deleted_task_attachments()
returns trigger
language plpgsql
set search_path = taskvilla, pg_temp
as $$
begin
  if new.operation = 'delete' and new.entity = 'task' then
    update taskvilla.task_attachments set status='delete_pending', updated_at=clock_timestamp()
      where task_id=new.entity_id and status <> 'deleted';
  elsif new.operation = 'delete' and new.entity = 'list' then
    update taskvilla.task_attachments set status='delete_pending', updated_at=clock_timestamp()
      where list_id=new.entity_id and status <> 'deleted';
  end if;
  return new;
end;
$$;
revoke all on function taskvilla.queue_deleted_task_attachments() from public, anon, authenticated;
drop trigger if exists taskvilla_queue_deleted_attachments on taskvilla.entities;
create trigger taskvilla_queue_deleted_attachments
after insert or update on taskvilla.entities
for each row execute function taskvilla.queue_deleted_task_attachments();
