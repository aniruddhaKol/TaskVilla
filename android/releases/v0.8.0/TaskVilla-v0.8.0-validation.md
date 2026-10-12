# TaskVilla v0.8.0 validation

Author: Aniruddha DuttaChowdhury · ADChowdhury

- APK built successfully: app.adc.taskvilla, TaskVilla, versionCode 8. Signature verified with the existing review certificate, allowing updates from earlier TaskVilla builds. No new dependencies installed.
- Headless browser checks passed: actual drag and keyboard reorder; required-field red borders/focus and clearing; confirmed task/list deletion toasts; pull-to-refresh with honest local-only status; View Tasks reference checklist and group-of-people sharing icon. No JavaScript/resource failures. No horizontal overflow at 320, 390 and 412 CSS pixels.
- Screenshots inspected for wood background, flat task rows, 5-pixel corners, contrasting list banner and consistently beveled buttons with a stronger lower border.
- Background Android API 36 emulator: upgraded schema 2 to 3, retaining three existing tasks. Actual touch drag reordered First/Second/Third to Second/Third/First. Actual touch pull-to-refresh reloaded SQLite and preserved order. Force-stop/restart retained the saved order.
- Restored the original empty emulator baseline after verification. No sample lists/tasks/shares ship with the APK.
- Email OTP and Harry/Tom invite/accept/ignore/decline/remove tests were not run, as requested. These requirements are recorded in taskvilla-sharing-flow.md for the server phase.

Limits: browser and emulator testing, not a physical device. Pull-to-refresh currently reloads local data only. Email OTP, invitation delivery and actual shared synchronization require the later server API. Architecture remains Android WebView with native SQLite.
