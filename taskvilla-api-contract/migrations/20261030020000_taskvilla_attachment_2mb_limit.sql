alter table taskvilla.task_attachments drop constraint if exists task_attachments_size_bytes_check;
alter table taskvilla.task_attachments add constraint task_attachments_size_bytes_check check (size_bytes between 0 and 2000000);
