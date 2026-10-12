# TaskVilla v0.18.0 validation

Author: Aniruddha DuttaChowdhury · ADChowdhury

- Signed app.adc.taskvilla versionCode 18 with the existing review certificate. SQLite schema remains 3; no dependencies added; earlier versions/signing keys preserved.
- Home icon measured 40px; transition measured 380ms with 36px slide/fade and scale. Reduced-motion disables screen animation.
- Storage row Export opens accessible popup with same-row JSON/XML buttons; Escape closes popup. Browser downloaded both formats successfully.
- Backups include all lists/tasks, completed records, IDs/references/order, preferences, hidden subtasks, attachment filenames and app/version/package/author/timestamp/timezone/schema/format metadata.
- Typed XML decoded to identical JSON data, preserving Unicode, booleans, numeric IDs, arrays and multiline notes. Responsive widths 320/390/412 passed without resource/JavaScript errors or horizontal overflow.
- Background Android API 36 saved actual JSON and XML files via the system document picker. Files parsed to identical data. A SQLite restore simulation retained task titles/notes/completion/subtasks/filenames. Test baseline restored afterwards.
- Export contains filenames only because this review app does not store attachment bytes. Restore UI remains a future feature. Physical-device export is untested; live sharing tests remain deferred until requested.
