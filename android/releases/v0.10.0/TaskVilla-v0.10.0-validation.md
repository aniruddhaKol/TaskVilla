# TaskVilla v0.10.0 validation

Author: Aniruddha DuttaChowdhury · ADChowdhury

- Built/signed app.adc.taskvilla, versionCode 10, with existing review key. SQLite schema remains 3; no dependencies added.
- Headless browser checks passed: Wood/Stone wall/Brick wall previews, applied selection and bundled assets; Choose an icon wording; pale ash Back/Cancel; bottom-centred deletion toast; list-only colour band; Built by developer icon and supplied LinkedIn link. Responsive widths 320/390/412 passed without resource/JavaScript errors.
- Inspected theme, deletion-popup and toast screenshots. Added dark author backing for contrast against light wall textures.
- Background Android API 36 emulator upgrade passed. Selected Stone and Brick saved in SQLite. Brick persisted through navigation and force-stop/restart. Original empty baseline restored after tests.
- Native device screenshot verified Android status bar #573d32 (neutral Brick theme) while app list banner was #284B63 (blue list colour). List colour does not extend into phone system bars.
- Live OTP/sharing tests remain deferred until requested. Refresh remains SQLite-only. Architecture is Android WebView + native SQLite; physical-device testing is pending.
