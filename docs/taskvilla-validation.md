# TaskVilla v0.26.0 validation

Author: Aniruddha DuttaChowdhury · ADChowdhury

## Changes

- Removed bulk Share files button; Attach files is the only top-level attachment action, right aligned. Per-file share icons remain.
- Live Files (n/10) heading starts at zero and tracks additions, confirmed removals and restored drafts.
- Latest additions appear at the top, including the order within a multi-selection batch. Legacy saved arrays are reversed once on upgrade; the attachmentOrderVersion preference prevents repeated reversal.
- Attachment rows alternate neutral colours, without divider lines. Removal uses a pale-red trash button and the standard Cancel/Remove confirmation before staging a removal. Save commits; Discard retains originals. Task/list deletion and people removal retain existing confirmation dialogs.
- Disclaimer uses the shared yellow warning icon and contains only: Max 10 files of 10 MB each.
- Task rows with saved attachment references show a paperclip immediately before the star.
- Due date now uses an explicit button, Android DatePickerDialog and an ISO-value callback rather than a transparent clickable input. Preview invokes the browser date picker. Date formatting remains dd-MMM-yyyy; selection, Clear and Save participate in task draft handling.

## Passed

- Browser integration: bulk-share absence, zero/count updates, new batches/newest-first order, alternating row colours, trash icon, Cancel/Remove confirmation, removal Discard, saved metadata, paperclip placement, short disclaimer and narrow 320/412px layouts. Screenshots inspected.
- Date tests: edit/create bridge request with current date, callback display and clear, Save persistence; real Chromium showPicker invoked in the button click user gesture. Native bridge mocked in browser tests.
- Migration/restart simulation: older file arrays reverse once, preferences persist the marker, next load preserves order and all attachment references.
- Android API 36 instrumentation: actual native DatePickerDialog opens for edit-due and task-due with year/month/day matching the supplied date; Back dismisses it. No synthetic task/list records were inserted. Temporary verification APK removed.
- Android APK v2/v3 signature verification passed with the original certificate. Package app.adc.taskvilla; versionCode 26; SQLite schema 3 and backup format 2 retained. All five active UI assets match the APK.

## Limits

- Interactive attachment document-picker to external recipient-app handoff remains unverified end to end. File copy/provider/share-intent behavior was tested with native instrumentation in v0.25.0; that implementation is unchanged.
- Real IME coverage beyond the earlier browser viewport checks remains incomplete. No live shared-list tests, backend, new dependencies or external messages.
- JSON/XML backups still include attachment metadata, not file contents; legacy filename-only entries require reattachment for sharing. These details remain in documentation, not the shortened on-screen disclaimer.
