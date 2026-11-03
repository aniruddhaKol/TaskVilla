-- Give each sync/support table a UUID primary key while keeping natural idempotency keys unique.
alter table taskvilla.entities add column id uuid not null default gen_random_uuid();
alter table taskvilla.entities drop constraint entities_pkey;
alter table taskvilla.entities add constraint entities_pkey primary key (id);
alter table taskvilla.entities add constraint entities_entity_identity_key unique (entity, entity_id);

alter table taskvilla.change_events add column id uuid not null default gen_random_uuid();
alter table taskvilla.change_events drop constraint change_events_pkey;
alter table taskvilla.change_events add constraint change_events_pkey primary key (id);
alter table taskvilla.change_events add constraint change_events_sequence_key unique (sequence);

alter table taskvilla.batch_receipts add column id uuid not null default gen_random_uuid();
alter table taskvilla.batch_receipts drop constraint batch_receipts_pkey;
alter table taskvilla.batch_receipts add constraint batch_receipts_pkey primary key (id);
alter table taskvilla.batch_receipts add constraint batch_receipts_idempotency_key
  unique (user_id, device_id, batch_id);

alter table taskvilla.change_receipts add column id uuid not null default gen_random_uuid();
alter table taskvilla.change_receipts drop constraint change_receipts_pkey;
alter table taskvilla.change_receipts add constraint change_receipts_pkey primary key (id);
alter table taskvilla.change_receipts add constraint change_receipts_change_id_key unique (change_id);
