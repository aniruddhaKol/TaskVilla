# TaskVilla backend contract and Supabase deployment

The single logical contract is [`application.xml`](application.xml) with provider-neutral request/response schemas under [`schemas/`](schemas/). It contains no provider profiles or environment variants. The current deployed wire mapping for Supabase is documented in [`docs/supabase-deployment.md`](docs/supabase-deployment.md).

## Contents

- `application.xml` — local-first behavior, logical operations and response codes.
- `schemas/` — JSON request/response contracts; sharing includes the minimum invitation inbox and decline extension.
- `migrations/` — applied PostgreSQL migrations for project `app1`.
- `TaskVilla.postman_collection.json` and `TaskVilla.postman_environment.json` — deployed endpoint examples and positive/negative assertions.

## Supabase app1

Project ID: `mbyyesfvbpdttztiopfd`  
API URL: `https://mbyyesfvbpdttztiopfd.supabase.co`  
Schema: `taskvilla`

Use the Supabase publishable key in mobile clients; never package a secret/service-role key. Native Auth handles OTP and session refresh. PostgreSQL RPCs handle sync and sharing. See the deployment mapping for native wire formats and status-code differences.

## Offline-first requirements

1. Task/list/activity reads and writes stay in SQLite; network failures never block local use.
2. Commit local changes and outbox events in one SQLite transaction. Acknowledge outbox records only after server confirmation.
3. Retain queued changes through offline state, auth expiry, errors and process restarts.
4. Sync manually, after selected actions, and when network returns; do not add a periodic scheduler.
5. Retry the same `batchId` and `changeId`. Preserve conflicts for resolution rather than overwriting or endlessly retrying.
6. Apply pulled changes and the cursor atomically. Honor server revisions and tombstones.

## Security and ownership

All RPC calls require an authenticated email-verified Supabase user. Private tables have RLS enabled and deny client table access; the API surface is restricted to guarded RPC functions. Never place database passwords or service-role keys in application assets or Postman environments.

Author: Aniruddha DuttaChowdhury · ADChowdhury
