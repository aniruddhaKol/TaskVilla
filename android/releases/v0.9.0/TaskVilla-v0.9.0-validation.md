# TaskVilla v0.9.0 validation

Author: Aniruddha DuttaChowdhury · ADChowdhury

- Signed app.adc.taskvilla APK, versionCode 9, verified with the existing TaskVilla review certificate. Updated the background Android API 36 emulator successfully. SQLite schema stays at 3; no new dependencies.
- Headless browser passed: list-colour count badge with inverted foreground, removal of enter-list icon, one-pixel extra row spacing, floppy Save icons in creation forms, pale green add/save/email-plus and pale red delete actions, centred toast, visible lifted drag card and persisted reorder.
- Pull/release status has bounce and soft transitions; reduced-motion override is included. Actual Android touch drag and pull-to-refresh passed. SQLite retained order through refresh and app restart. This remains local refresh, with server sync explicitly unavailable.
- Author credit appears at Settings bottom; LinkedIn URL matches the supplied address. Android click dispatched ACTION_VIEW to Chrome, verified in ActivityTaskManager logs.
- Inspected home, task list, drag preview, options, toast, refresh and Settings screenshots. Responsive checks passed at 320/390/412 CSS pixels, with no JavaScript or resource errors.
- Original empty emulator data restored after testing. Test screenshots use temporary fixtures; they are not bundled app data.
- OTP/live-sharing end-to-end tests remain deferred until the user requests them. The future rules remain in taskvilla-sharing-flow.md.

Limits: emulator/browser verification, not physical-device testing. App remains Android WebView + native SQLite; server APIs and authentication are pending.
