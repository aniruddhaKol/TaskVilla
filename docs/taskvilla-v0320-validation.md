# TaskVilla 0.32.0 validation — 17 October 2026

PASS — Headless Edge functional checks: per-list top/bottom insertion and preference Save/Discard; legacy-note thread conversion, newest-first comments, edit/delete/cancel/discard and saved thread retention; 100/280 limits including rejected programmatic over-length input; extensive copy has exactly Task Name/Due Date/Tags and excludes private notes/files/subtasks. Task creation/edit validation and draft mechanics retained.

PASS — Tracker: empty initial data, all 110 catalogue suggestions, known/custom autocomplete and keyboard selection, custom suggestion retention, entries and integer-paise amounts, calendar-day next-date calculation, zero interval, history editing, cancellation and confirmed entry/activity deletion (including final-entry cleanup), charts and spending totals, required/date/amount/interval/length validation, unsaved changes and simulated persistence failures. Browser persistence through a simulated native bridge survives reload; JSON/XML backup structures include tracker/comments and schema 4. Main tracker layouts checked in all four themes at 320/390/412px; narrow activity form rendered and visually inspected. Captures use synthetic fixtures only.

PASS — Real Android SQLite instrumentation, isolated test database: schema 3-to-4 migration preserves original list/task/legacy note/subtask/attachment references/theme; default preference false and empty tracker; list preference/comments/entries/custom types survive close/reopen; foreign-key failure rolls back the whole save; private fixture bytes unchanged. No user database was used.

PASS — Actual native DatePickerDialog opens through the bridge for edit-due, task-due and activity-due using the supplied initial date; Cancel does not apply changes. Verification ran in a headless emulator. Temporary instrumentation packages removed.

PASS — Final APK builds and v2/v3 signature verification match the original signing identity. Active UI/scripts/catalogue/icon match APK assets byte for byte. Java sources include tracker in the Android exit-save path so it is retained on app exit.

LIMITS — This is not complete real-IME/touch visual coverage or email/attachment recipient handoff. Live server list-sharing remains deferred. No phone was connected or used, and no Git publication was performed.
