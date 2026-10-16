# TaskVilla v0.25.0 validation

Author: Aniruddha DuttaChowdhury · ADChowdhury

## Implemented

- Unsaved-change guard for task edit and list options, including toolbar and Android Back. Unchanged forms leave without prompting. Discard is secondary/left and Save primary/right; the cross and Escape dismiss the dialog and keep form values. Save validates and commits before leaving; failed saves roll back model changes and retain the draft.
- Task title, notes, dates, reminders, tags, completion, importance and attachment edits remain drafts. List name, appearance, pending people and an entered email are included in dirty detection. Existing confirmation behavior for removing people remains.
- Android multi-file selection with a maximum of 10 files per task and 10,000,000 bytes per file. Private file copies, SQLite JSON metadata, bounded background streams, batch rejection and cleanup. SQLite schema 3 is preserved; old filename strings remain intact and are labelled unavailable until reattached.
- Single-file or all-file Android sharing uses a non-exported, read-only ContentProvider with transient URI grants. No messages or files are sent automatically.
- Neutral disclaimer box and the shared warning artwork tinted yellow explain limits and metadata-only JSON/XML backups. Backup format 2 represents metadata objects; file bytes are excluded. Browser preview file bytes last only for the current page session.
- Standing navigation preference applied: no right navigation chevrons are rendered. Existing Left Back controls remain.

## Passed checks

- Browser tests: unchanged Back, task/list Discard, Save and close/Escape; required-name and email validation; completion/star drafts; list appearance and people; retained typed values; failed-save rollback.
- Browser attachments: multiple selection, exact 10 MB accepted, 10 MB + 1 byte rejected, over-count rejection, additions across selections, removal/Discard, legacy entries, picker/share bridge callbacks and metadata persistence. Browser-native bridge calls were mocked.
- Keyboard regression checks at 320/390/412 px: inset title focus, simulated keyboard viewport, notes/list/picker visibility, switching fields, creation autofocus, retained edits and restored scroll. Real IME validation from v0.24 remains incomplete.
- Browser screenshots inspected for the Discard/Save dialog, attachment actions and yellow-icon disclaimer.
- Native Android API 36 instrumentation (using real filesystem and ContentResolver): imported-file bytes round-trip through AttachmentProvider; exact 10 MB accepted; oversized stream rejected with partial cleanup; staging tracked; provider write mode rejected; single/multiple share intents contain read grants and ClipData; >10 share entries rejected; mixed legacy/metadata references parsed. Existing SQLite records were not modified. Temporary instrumentation APK was removed afterward.
- APK built and verified with the original certificate using signature schemes v2/v3; package app.adc.taskvilla, versionCode 25. Packaged UI matches preview source.

## Boundaries

- Interactive Android document-picker UI and final recipient-app handoff were not end-to-end exercised. Native import, provider reading and share-intent construction were exercised directly by instrumentation.
- Android hardware-Back dispatch is wired to the same guarded go() path; dirty-form outcomes were exercised in the browser. No claim of complete native UI/IME testing is made because the emulator System UI remains unstable.
- JSON/XML exports do not embed attachment contents. There is no restore UI or server/list-sharing service. No end-to-end shared-list tests or outbound messages were performed.
