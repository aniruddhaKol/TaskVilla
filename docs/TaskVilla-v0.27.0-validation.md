# TaskVilla 0.27.0 validation

PASS — Headless browser: known CRC32 vector and empty file; renamed/existing/batch duplicate rejection; atomic rejection without form reset; equal-size different-content files; same filename different-content files; removal/reattach and Discard; cross-task attachment scope; no JavaScript errors.

PASS — Android instrumentation in a headless emulator: native CRC32 known vector; renamed/batch rejection and staged-copy rollback; CRC backfill for older attachments; equal-size different CRC accepted; identical bytes accepted for another task. Real isolated SQLite saves and real private files verified task deletion, list deletion, retention of unrelated files, active-draft protection and draft-discard cleanup. User database and attachments were not modified by test fixtures. Temporary instrumentation package removed after checking.

PASS — APK built and signed with the existing review certificate. Active packaged UI files match preview sources. Native test used storage/save components directly; this is not a new end-to-end visual UI/keyboard or external recipient-handoff test. Server list-sharing tests remain deferred.
