# JQuiver AI study for TaskVilla

Prepared by Aniruddha DuttaChowdhury · ADChowdhury
Study date: 11-Oct-2026
TaskVilla application identifier: app.adc.taskvilla
Repository snapshot: 258fbdec3f9be97df3d0d52f91d3c7fc9493f551
Scope: documentation/source analysis only. No JQuiver installation, build, database connection, server startup, mail delivery, API execution or app changes. Repository instructions were read as reference material for the requested study; they do not replace the user's TaskVilla requirements.

## What the supplied folder contains

`jquiver-ai` describes itself as an AI Knowledge Base and Agent Enablement Pack. It contains topic documentation, metadata references, workflows, developer runbooks, examples, repository-local skills and small JSONL training/evaluation seeds. It is useful guidance for an agent building or maintaining a JQuiver instance. It is not itself an executable AI server or a generic OpenAPI-to-backend generator. No external skills were installed.

The actual overview filename in this snapshot is README_2.md. Some documents link to README.md, which is absent from this folder. Several topics deliberately distinguish observed export metadata from unverified runtime behavior and retain TODOs. Consequently, an observed platform option does not establish working behavior in every deployed instance.

Source: https://github.com/trigyn-products/jquiver/blob/258fbdec3f9be97df3d0d52f91d3c7fc9493f551/jquiver-ai/README_2.md

## How JQuiver builds APIs

The documented approach is metadata-driven development: custom business tables hold domain data; jq_dynamic_rest_details describes the endpoint/service, jq_dynamic_rest_dao_details describes query execution, role associations govern endpoint access, and jq_additional_datasource selects custom databases. Script libraries permit reusable service code. Forms/grids/router/menu metadata are relevant for an administration UI, not a replacement for the TaskVilla mobile UI.

Verified runtime: DynamicRestController strips the configured API prefix (or /japi), matches a metadata URL and delegates to JwsDynamicRestDetailService. The service resolves details by URL, checks the HTTP method (or ALL), executes DAO/service logic and produces a response. DynamicRestEntityValidator checks route/method/role access. These platform roles alone do not enforce which TaskVilla list a particular user owns or has accepted: every query/write/download must also enforce business membership.

Source references:
- https://github.com/trigyn-products/jquiver/blob/258fbdec3f9be97df3d0d52f91d3c7fc9493f551/jquiver-ai/playbooks/create-dynamic-rest-api.md
- https://github.com/trigyn-products/jquiver/blob/258fbdec3f9be97df3d0d52f91d3c7fc9493f551/maven-common-utils/java-web-starter/src/main/java/com/trigyn/jws/dynarest/controller/DynamicRestController.java
- https://github.com/trigyn-products/jquiver/blob/258fbdec3f9be97df3d0d52f91d3c7fc9493f551/maven-common-utils/java-web-starter/src/main/java/com/trigyn/jws/dynarest/service/JwsDynamicRestDetailService.java

## Execution languages and hosting

The runtime switch explicitly handles Java, FTL, JavaScript, Python and PHP. These are service-logic platforms inside the Java application. This does not make JQuiver a standalone Node.js/Python/PHP application.

The inspected parent POM declares Spring Boot 3.4.5, Java 22 and Nashorn 15.7. The starter POM includes Jython 2.7.3 and php-java-bridge 7.1.3. Python execution therefore needs careful compatibility review; do not assume Python 3 packages or CPython behavior. Nashorn JavaScript with Java interop is not Node.js; do not assume npm/Node APIs. PHP bridge availability in a deployed instance still needs runtime verification. Java/FTL/Nashorn are reasonable candidates for an initial JQuiver design, subject to tested instance compatibility.

A small hosted deployment would run a Java runtime plus the platform database, potentially custom schema, upload storage and scheduled workers. Minimum RAM/startup cost/capacity were not benchmarked. No claim of suitability for a particular free VM has been established.

Sources:
- https://github.com/trigyn-products/jquiver/blob/258fbdec3f9be97df3d0d52f91d3c7fc9493f551/maven-common-utils/pom.xml
- https://github.com/trigyn-products/jquiver/blob/258fbdec3f9be97df3d0d52f91d3c7fc9493f551/maven-common-utils/java-web-starter/pom.xml

## Authentication: existing support and the TaskVilla gap

The auth reference documents email OTP alongside password/captcha/TOTP variants, using an OTP-mail prerequisite, /japi/login and a separate /cf/register route. This demonstrates useful OTP foundations, not the exact unified first-time/returning-user flow requested for TaskVilla.

TaskVilla needs one OTP request + verify workflow that atomically consumes a challenge and finds/creates a user. It must include send/attempt limits, expiry, resend invalidation, generic responses, a consistent email uniqueness rule and mailbox-to-invitation binding. Existing JQuiver mode/configuration/request bindings must be checked before reusing its authentication implementation.

JWT source was reviewed. JwtUtil validates parsed tokens and expiry/username, while JwtRequestFilter integrates bearer headers and authentication-type/configuration context. These inspected paths do not establish per-device session revocation or logout-all. Additional filters/instance customization could exist; this is not a whole-product security audit.

Do not treat jq_deleteSessionKey as logout-all. ScriptUtil.updateSession/getValueFromSession/haveSessionKey/deleteSessionKey currently operate on cookies, confirmed in source and the utility reference. TaskVilla must have explicit revocable device sessions (or equivalent session-generation checks), checked on every API/refresh request. The proposed TaskVilla contract uses opaque access tokens; JQuiver's inspected JWT/filter conventions are not directly interchangeable. An authentication adapter or deliberately versioned contract adjustment would be needed.

Sources:
- https://github.com/trigyn-products/jquiver/blob/258fbdec3f9be97df3d0d52f91d3c7fc9493f551/jquiver-ai/reference/jquiver-auth-api-reference.md
- https://github.com/trigyn-products/jquiver/blob/258fbdec3f9be97df3d0d52f91d3c7fc9493f551/maven-common-utils/java-web-starter/src/main/java/com/trigyn/jws/usermanagement/security/config/JwtUtil.java
- https://github.com/trigyn-products/jquiver/blob/258fbdec3f9be97df3d0d52f91d3c7fc9493f551/maven-common-utils/java-web-starter/src/main/java/com/trigyn/jws/usermanagement/security/config/JwtRequestFilter.java
- https://github.com/trigyn-products/jquiver/blob/258fbdec3f9be97df3d0d52f91d3c7fc9493f551/jquiver-ai/reference/script-util-reference.md

## Fit against TaskVilla requirements

| Requirement | Reusable JQuiver foundation | TaskVilla-specific work |
|---|---|---|
| Lists/tasks CRUD | Dynamic REST + DAO queries + reusable scripts | Business schema, owner/member checks, validation, revisions, idempotency |
| Drag sorting | REST/query service logic | Atomic partition order/revision handling |
| Email OTP | Documented database-auth OTP flow + mail | Unified find/create, challenge limits and session contract |
| Logout all devices | Authentication/token components | Explicit session ledger/revocation checked on every request |
| Shared invitations | Users/roles, REST, mail | Pending/accept/ignore, verified-email binding, leave/remove, access revocation |
| Email reminders | SMTP service/templates, Quartz/job API linkage | Recipient selection, stop conditions, retry/duplicate prevention, rate limits |
| Device/IP action audit | Request access, activity-log patterns | Trusted proxy handling, redaction, retention, private profile linkage |
| Phone/demographics | Private custom profile tables | Optional consented self-report; no automatic extraction or member exposure |
| SQLite pull-to-refresh | REST responses | Snapshot/delta cursor, tombstones, revocation, conflict handling |
| Files/backups | File bins, streams, metadata patterns | Task ACLs, upload validation and TaskVilla format1 migration |

Mail is substantive functionality: ScriptUtil exposes sendMail and the inspected SendMailService configures SMTP authentication and records failed-mail history. Scheduler docs describe jq_job_scheduler rows invoking Dynamic REST APIs; source includes Quartz mail scheduling. Gmail SMTP is a configuration candidate, not a verified deployment. No mail or scheduler was executed.

Sources:
- https://github.com/trigyn-products/jquiver/blob/258fbdec3f9be97df3d0d52f91d3c7fc9493f551/jquiver-ai/knowledge-base/17-notifications-mail.md
- https://github.com/trigyn-products/jquiver/blob/258fbdec3f9be97df3d0d52f91d3c7fc9493f551/jquiver-ai/knowledge-base/14-scheduler.md
- https://github.com/trigyn-products/jquiver/blob/258fbdec3f9be97df3d0d52f91d3c7fc9493f551/maven-common-utils/java-web-starter/src/main/java/com/trigyn/jws/dynarest/service/SendMailService.java

## OpenAPI mapping: preserve the contract, add a JQuiver implementation map

TaskVilla-API-v1.0.0.openapi.json remains the platform-independent contract. No documented OpenAPI importer was found in the reviewed AI pack or selected source, so automatic import/generation into JQuiver must not be promised. JQuiver-specific metadata has to be designed from actual target-version tables and working examples.

For a later authorized implementation, map each operationId to the JQuiver URL pattern, HTTP type, producer, service platform/script, parameterized DAO queries, datasource, role associations, and business authorization/session check. Keep private scheduled mail jobs separate from public client APIs. Metadata IDs, lookup IDs, schema columns and JSON/body binding must be verified against that instance, not invented.

Routing deserves an early proof: the reviewed runtime matches wildcard patterns (* and **) and fetches API details by URL before checking method. Our contract uses named placeholders and multiple methods on paths such as /lists and /tasks/{taskId}. Verify method dispatch and parameter extraction for this structure. An ALL-method dispatcher or a deliberate mapping strategy may be needed; this is an implementation hypothesis, not a tested limitation.

Suggested order, not work performed: verify isolated target version/schema and routing; implement unified OTP/session revocation; add list/task operations and row-level ACLs; add invitation lifecycle; add sync; add attachments/backup migration; finally workers/audit/admin views. Keep sharing end-to-end tests deferred until explicitly requested, as agreed.

## Assessment

JQuiver is a credible candidate when the goal is metadata-driven APIs plus administration screens, email and scheduling. The AI pack gives usable, structured guidance for that approach. It does not eliminate service logic or automatically satisfy TaskVilla authentication, sharing and synchronization rules. Replacing it with generic Flask/Express-generated handlers would bypass the documented JQuiver development approach; choosing JQuiver would call for a separate metadata implementation plan while retaining the TaskVilla API contract.

The mobile app, OpenAPI contract and existing release artifacts were not modified during this study. The full public AI documentation snapshot was downloaded for reference; only the relevant architecture/REST/auth/scripts/mail/scheduler/operations material and selected runtime files were inspected in depth. No claims of runtime verification, dependency security clearance or hosting benchmarks are made.
