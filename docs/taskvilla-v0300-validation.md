# TaskVilla 0.30.0 validation

PASS — Headless Edge: task creation/editing and list creation/options each tested for unchanged Back, changed Back, Discard, cross, Escape and Save. Evaluated the exact JavaScript handler used by MainActivity.onBackPressed to check Android Back routing and prompt dismissal. This is browser execution of that handler, not a new physical Android Back/IME test.

PASS — Required-field validation, red focus and draft retention; simulated persistence failures roll back attempted task/list changes without navigating or losing drafts; successful create/edit Save; normal new-list Save; task star, notes and attachment-only changes; list appearance-only changes; invalid email on list options; appearance reset after Discard; no JavaScript errors. Synthetic browser fixtures only.

PASS — APK compiled and signed with the existing review certificate (v2/v3 verified). Packaged UI matches active preview files. SQLite schema remains 3; no destructive migration. No installation, backend, list-sharing or live attachment recipient-handoff tests were performed in this build.
