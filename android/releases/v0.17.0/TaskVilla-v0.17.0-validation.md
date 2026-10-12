# TaskVilla v0.17.0 validation

Author: Aniruddha DuttaChowdhury · ADChowdhury

- Signed app.adc.taskvilla versionCode 17 with the existing review certificate; schema remains 3. No dependencies added; earlier releases preserved.
- Removed theme-dependent JavaScript/native system-bar bridge. Android status/navigation bars explicitly use black; window background is white.
- Browser checks passed table caption, ten column headers, all user-facing stored task fields, dd-MMM-yyyy dates, special-character escaping, multiline notes, subtasks and non-image filenames. Completed tasks remain excluded. Plain name copying remains unchanged; extensive text fallback is a headered TSV table.
- Browser checks passed home-list/task-row feedback, task navigation, bevel depression and release cleanup, reduced-motion and responsive widths 320/390/412. No JavaScript/resource errors.
- Background Android clipboard calls passed for extensive HTML and headered fallback; image filename excluded. Baseline restored after testing.
- Screenshots across Wood/Stone/Brick showed identical black system-bar backgrounds. Emulator System UI showed a not-responding dialog during capture; the app WebView and clipboard bridge remained responsive. This is recorded as an emulator limitation rather than a clean full-device pass.
- Actual Excel/email destination paste and physical-device checks remain pending. Live sharing tests remain deferred until requested. No server API added.
