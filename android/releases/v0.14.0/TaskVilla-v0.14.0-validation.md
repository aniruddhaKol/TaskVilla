# TaskVilla v0.14.0 validation

Author: Aniruddha DuttaChowdhury · ADChowdhury

- Signed app.adc.taskvilla versionCode 14 with existing review certificate; SQLite schema unchanged at 3. No dependencies added.
- Browser passed dd-MMM-yyyy displays on task rows/details/planned headings; field edits update display, ISO date storage remains intact and leap day 29-Feb-2028 saves correctly.
- Subtask controls absent in new/edit forms; saving an existing task preserves its hidden subtask array.
- Measured 30px Back box and 26px arrow; top-right task delete uses the same red colour as list deletion and a flat pale-red background.
- Navigation slide/fade verified; reduced-motion disables it. Same-screen updates do not trigger screen transitions. No JavaScript failures or horizontal overflow at 320/390/412 widths.
- Icon edited with imagegen: larger three-person group, light central figure and dark outlines, house moved up/left to use spare space, rounded alpha corners and original canvas retained. Full art and small in-app rendering inspected; earlier icon versions preserved.
- Background Android API 36 update passed; task due date displayed 05-Oct-2026, subtask controls were absent, and saving notes preserved the existing subtask array in SQLite. Original test baseline restored; APK contains no sample records.
- Physical device and live sharing tests pending; OTP/sharing end-to-end tests stay deferred until requested.
