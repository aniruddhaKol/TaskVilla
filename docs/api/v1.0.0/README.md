# TaskVilla API contract v1.0.0

Author: Aniruddha DuttaChowdhury · ADChowdhury
Application identifier: app.adc.taskvilla

Open TaskVilla-API-v1.0.0.html directly in a browser. It embeds the same contract as TaskVilla-API-v1.0.0.openapi.json and needs no server, CDN or dependencies. The HTML is a Swagger-style reference, not a live API client. JSON is the source contract for compatible OpenAPI 3.1 code generators for Python/PHP/Node.

41 operations, 33 paths, 65 schemas; 13 synthetic request examples. Registration/sign-in are unified email OTP request + verify. All-device logout invalidates every access/refresh session immediately via mandatory server-side session checks. Optional phone/demographics reside in the private user profile; per-action audit links the user/session without copying sensitive profile values into every log. IP is derived server-side; client metadata and offline event timestamps are untrusted. Pending invitation access, accept/ignore, leave/removal and revocation rules preserve the previously agreed sharing model.

Validation completed: local reference resolution, path parameter/operation ID uniqueness, required fields, authentication/status mappings, basic example constraints and HTML/JSON equality. Browser passed offline rendering, search/filter/expand/collapse, examples, actual downloaded JSON equality, 390/768/1440 widths and no script/network errors. This is a custom structural audit, not full validation by an external OpenAPI validator. Production generator compatibility and business logic must be validated during implementation. No live API/authentication/sharing tests run. Existing app v0.22.0 and its released signing key/assets remain unchanged.

Proposed defaults (TTL, editor permissions, retention and upload limits) are reviewable contract decisions. Deploy no server until OTP delivery/rate limits, authorization, session revocation, transactional persistence, synchronization, secure attachment handling, SMTP worker and privacy/retention policy are implemented. Export/import supports existing backup format1 and local schema3 via explicit UUID mappings; imports never activate shares or overwrite current data.

References:
- https://spec.openapis.org/oas/v3.1.0.html
- https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html
- https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html
