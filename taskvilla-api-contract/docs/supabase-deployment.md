# TaskVilla Supabase backend

Deployed to project `app1` (`mbyyesfvbpdttztiopfd`), region `ap-south-1`, schema `taskvilla`. The application remains local-first: SQLite remains authoritative while offline and no RPC is called without an authenticated session.

## Endpoints

Base URL: `https://mbyyesfvbpdttztiopfd.supabase.co`

| Logical operation | HTTP endpoint | Auth |
| --- | --- | --- |
| OTP request | `POST /auth/v1/otp` | Publishable key |
| OTP verification | `POST /auth/v1/verify` | Publishable key |
| Refresh session | `POST /auth/v1/token?grant_type=refresh_token` | Publishable key |
| Push changes | `POST /rest/v1/rpc/sync_push` | User access token; `Content-Profile: taskvilla` |
| Pull changes | `POST /rest/v1/rpc/sync_pull` | User access token; `Content-Profile: taskvilla` |
| Sharing | `POST /rest/v1/rpc/manage_sharing` | User access token; `Content-Profile: taskvilla` |

Send `apikey: <publishable key>` and `Authorization: Bearer <user access token>` on API calls. Auth requests use the publishable key as the bearer value. Never ship a secret/service-role key in the app or Postman environment.

## Supabase Auth mapping

Auth is native; no Edge Function or custom OTP table is used. The logical camelCase contract is adapted to GoTrue's wire format:

- `otpRequest { email, createUser }` → `/auth/v1/otp` body `{ email, create_user }`. Supabase returns HTTP 200; the adapter reports `delivery: "email"`. Supabase does not provide a stable request ID or expiry in the public response, so a client-generated UUID may be used for `requestId`, while `expiresInSeconds` remains omitted.
- `otpVerify { email, code }` → `/auth/v1/verify` body `{ type: "email", email, token: code }`. Map `access_token`, `refresh_token`, `expires_in`, `token_type`, and `user.id` to the contract's camelCase session fields; normalize token type to `Bearer`.
- `tokenRefresh { refreshToken }` → `/auth/v1/token?grant_type=refresh_token` body `{ refresh_token }`; map the returned session as above.
- Native Auth errors use `{ code, error_code, msg }`; map to the contract's nested `error` object. Native status differences are reflected in `application.xml`: invalid OTP is 403, invalid refresh is 400, and disabled OTP signup is 422.

Email delivery still depends on the project's Auth email provider/SMTP and rate limits. The app must handle rate limiting and must not treat a request response as proof that a mailbox received the message.
The current Auth email template sent signup/magic-link URLs during verification. The numeric-code mapping above uses the same native Auth verification endpoint; if the mobile UI should accept a numeric code rather than a deep link, the Supabase Auth email template must include its `.Token` value. Auth templates/settings were not changed as part of the database deployment.

## Sync and sharing semantics

- `sync_push` accepts at most 200 changes. `changeId` receipts deduplicate mutations; `(user, device, batchId)` caches the exact result and rejects reuse with a different request (409). New records use `baseRevision: null` or `0`; updates/deletes require the current revision. Stale changes are returned in `conflicts` and do not overwrite current data.
- Task upsert payloads must include `listId` as a UUID. Activities are personal and are never shared. List owners can modify lists and all task fields; shared members can only mark tasks complete or append comments. Only owners can modify list records.
- Deletes are retained as tombstones. Deleting a list also tombstones its tasks. A pull cursor is an opaque, random, per-user/per-device UUID, valid for 90 days. Expired/unknown cursors return 410; the client must full-resync transactionally without dropping its local outbox.
- `manage_sharing` supports `share`, `list`, and `revoke`. Sharing uses the recipient's verified email; no message, code, or acceptance is required. A pending share is claimed automatically when that email signs in and syncs, then the server publishes a list/task snapshot. A removed member loses access on their next sync; the server emits recipient-only tombstones. Task owners can edit full task records; members can only change completion and append comments. File attachments remain local and are excluded from sync.
- `list` without a `listId` claims pending shares for the signed-in verified email and returns the user's shared-list metadata. With a `listId`, it returns the owner and current members plus pending email shares when called by the owner.
- Native PostgREST errors use `{ code, message, details, hint }`; RPC custom SQLSTATEs map to the documented HTTP statuses (PT400, PT401, PT403, PT404, PT409, PT410, PT413). Clients should normalize this format to the contract error envelope.

## Database security

All six tables have RLS enabled and explicit deny policies for `anon` and `authenticated`; no table or sequence privileges are granted to API clients. RPC functions run as the migration owner, set a fixed search path, require `auth.uid()`, validate ownership and accepted membership, and are executable only by `anon` (for the explicit 401 response) and `authenticated`. Anonymous execution exits before any data access or mutation. The `taskvilla` schema was added to PostgREST's exposed schemas while retaining the existing `public` and `graphql_public` entries. `smsanalyzer` was not changed.

## Migrations

- `20261028085354_taskvilla_backend_v1_2` — sync, sharing, tables, RLS, indexes and RPCs.
- `20261028085454_taskvilla_api_exposure_and_denies` — PostgREST exposure and explicit deny policies.
- `20261028085709_taskvilla_unauthenticated_rpc_errors` — anonymous RPC execute grants so unauthenticated calls return 401.
- `20261028090148_taskvilla_uuid_primary_keys` — UUID primary keys and retained unique idempotency constraints.
- `20261028090736_fix_taskvilla_sharing_email` — qualifies the Auth email column in sharing logic.
- `20261028090857_add_invite_email_to_listing` — includes the pending email address in invitation inbox results.
- `20261028091315_sync_push_concurrency` — serializes concurrent idempotent batch/change retries.

## Verification (28 Oct 2026)

- Applied all seven TaskVilla migrations and verified the three RPC signatures, schema exposure, UUID primary keys, RLS, fixed function search paths and client table-deny grants.
- Live Auth: OTP request returned 200; the supplied magic link verified and returned a session; token refresh returned 200. Invalid verification returned 403 and invalid refresh returned 400.
- Live REST: push, identical retry, batch mismatch (409), stale revision conflict, pull/cursor, expired cursor (410), anonymous sync/sharing (401), and direct table denial passed. Smoke-test lists were tombstoned; no active smoke entities or pending invitations remain.
- Sharing function-level checks passed for owner invite, listing and pending-invite revoke. The second-account OTP request was rate-limited (429), so accepted-member snapshot, decline and recipient revocation were not live-tested; the Postman collection contains those two-account scenarios.
- Security Advisor reported only that leaked-password protection is disabled; TaskVilla uses email OTP and does not enable password login. Performance Advisor's unused-index notices are expected for a newly deployed schema.

See `TaskVilla.postman_collection.json` and `TaskVilla.postman_environment.json` for request templates and assertions. Fill in `testEmail` and OTP from a mailbox you control; sharing acceptance/revocation additionally needs a second verified account in `memberAccessToken`.
