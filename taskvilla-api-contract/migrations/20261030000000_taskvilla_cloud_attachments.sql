-- TaskVilla provider-backed attachments. File bytes remain in the user's provider account.
create table if not exists taskvilla.user_cloud_connections (
  connection_id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  provider text not null check (provider in ('google_drive','dropbox','onedrive')),
  provider_account_id text not null,
  display_name text not null default '',
  account_email text not null default '',
  credential_ciphertext text not null,
  folder_id text not null default '',
  status text not null default 'connected' check (status in ('connected','disconnected','reauthorization_required')),
  is_default boolean not null default false,
  quota_total_bytes bigint,
  quota_used_bytes bigint,
  created_at timestamptz not null default clock_timestamp(),
  updated_at timestamptz not null default clock_timestamp(),
  unique (user_id, provider)
);
create unique index if not exists user_cloud_connections_one_default
  on taskvilla.user_cloud_connections(user_id) where is_default;

create table if not exists taskvilla.task_attachments (
  attachment_id uuid primary key default gen_random_uuid(),
  task_id uuid not null,
  list_id uuid not null,
  uploader_id uuid not null references auth.users(id) on delete restrict,
  connection_id uuid not null references taskvilla.user_cloud_connections(connection_id) on delete restrict,
  provider text not null check (provider in ('google_drive','dropbox','onedrive')),
  provider_file_id text not null,
  client_attachment_id uuid not null,
  original_name text not null,
  mime_type text not null default 'application/octet-stream',
  size_bytes bigint not null check (size_bytes between 0 and 2000000),
  sha256 text not null check (sha256 ~ '^[0-9a-f]{64}$'),
  status text not null default 'available' check (status in ('available','unavailable','delete_pending','deleted')),
  created_at timestamptz not null default clock_timestamp(),
  updated_at timestamptz not null default clock_timestamp(),
  unique (uploader_id, client_attachment_id)
);
create index if not exists task_attachments_task_active on taskvilla.task_attachments(task_id, created_at desc) where status <> 'deleted';
create index if not exists task_attachments_connection on taskvilla.task_attachments(connection_id, status);
create index if not exists task_attachments_task_hash on taskvilla.task_attachments(task_id, sha256) where status <> 'deleted';
create unique index if not exists task_attachments_no_task_duplicate on taskvilla.task_attachments(task_id, sha256) where status <> 'deleted';

create table if not exists taskvilla.cloud_oauth_states (
  state_id uuid primary key default gen_random_uuid(),
  state_hash text not null unique,
  user_id uuid not null references auth.users(id) on delete cascade,
  provider text not null check (provider in ('google_drive','dropbox','onedrive')),
  code_verifier text not null,
  expires_at timestamptz not null,
  created_at timestamptz not null default clock_timestamp()
);
create index if not exists cloud_oauth_states_expiry on taskvilla.cloud_oauth_states(expires_at);

alter table taskvilla.user_cloud_connections enable row level security;
alter table taskvilla.task_attachments enable row level security;
alter table taskvilla.cloud_oauth_states enable row level security;
revoke all on taskvilla.user_cloud_connections, taskvilla.task_attachments, taskvilla.cloud_oauth_states from public, anon, authenticated;
grant all on taskvilla.user_cloud_connections, taskvilla.task_attachments, taskvilla.cloud_oauth_states to service_role;
