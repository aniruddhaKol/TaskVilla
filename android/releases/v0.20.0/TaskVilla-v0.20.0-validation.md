# TaskVilla v0.20.0 validation

Author: Aniruddha DuttaChowdhury · ADChowdhury

- Signed app.adc.taskvilla versionCode 20 with the existing review certificate; v2/v3 verification passed. SQLite schema 3 retained, earlier releases and signing key preserved.
- Browser passed list icon/title shared-element travel, inert ghosts and cleanup, task slide/fade and reverse Back direction, bottom-up creation, task-name autofocus and popup animation.
- Rapid navigation cancels previous motion and restores header visibility. Reduced-motion bypasses screen and dialog animations. Layouts at 320/390/412px passed without overflow, JavaScript errors or missing resources.
- Background Android API 36 passed shared elements and cleanup, task slide/fade, form rise with autofocus and popup rise. Emulator animation completion was delayed; fallback cleanup was added and verified. Original SQLite baseline restored after checks.
- JSON/XML backup version metadata now reports 0.20.0. Full emoji catalogue and prior UI features retained. No dependencies installed, backend changes or live sharing tests.
