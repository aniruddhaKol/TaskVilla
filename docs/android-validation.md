# Android SQLite build validation — 0.1.0

Author: Aniruddha DuttaChowdhury · ADChowdhury  
Date: 9 October 2026  
Package: `app.adc.wunderlistmobileapp`

Validated by installing the signed APK on a Pixel 8 Android API 36 emulator. No physical phone was available in this session.

- PASS: Java/resource/DEX build and APK signing verification (v2 and v3).
- PASS: package `app.adc.wunderlistmobileapp`, versionCode 1, versionName 0.1.0; author/vendor metadata and signing certificate identify Aniruddha DuttaChowdhury / ADChowdhury.
- PASS: app launches with its real Android system bars; desktop reviewer controls are absent.
- PASS: all 21 screen layouts render without horizontal overflow.
- PASS: Roboto Mono font loads from bundled local assets.
- PASS: list-context task creation, editing, deletion, completion, starring, due date, notes, tags and subtasks (browser regression plus actual Android create/edit/complete/star tests).
- PASS: Android SQLite stores created lists, tasks, all tested task fields, reminder preference and joined demo list state.
- PASS: force-stop and relaunch retains saved data, without duplicate seed records.
- PASS: invalid foreign-key save fails and rolls back; the previous dataset remains unchanged.
- PASS: copied actual SQLite file passes `PRAGMA integrity_check`; `PRAGMA foreign_key_check` reports no violations.
- PASS: recipient join demo creates one local joined list; its tasks save to that list.
- PASS: actual Android keyboard resizes the viewport; Add task remains above the keyboard and fields scroll.
- PASS: no application crash found in AndroidRuntime logs.
- PASS: manifest contains no Internet permission.

The emulator briefly displayed a System UI not-responding dialog; choosing Wait resolved it. This was an emulator/system process event, not a recorded app crash. Keyboard and screen checks then completed normally.

Attachment durability, real sharing, account authentication and notification delivery are not implemented. Those remain later phases. The database test artifacts are retained only under the chat's work directory; the APK contains initial representative data, not test-created user records.
