# TaskVilla v0.13.0 validation

Author: Aniruddha DuttaChowdhury · ADChowdhury

- Signed app.adc.taskvilla versionCode 13 with existing review certificate; background emulator update passed. SQLite schema stays at 3.
- Icon edited with imagegen to restore rounded squircle/alpha corners and three-person group, retaining wooden palette and two checklist rows. Master matches original 1254×1254 canvas; derived PNGs preserve aspect ratio and transparency. Previous art retained. Adaptive inset restored to 16%.
- Browser measured equal 36×36 header Back/More boxes and aligned author icon/label centres on one row. Screenshots inspected. No JavaScript failures or overflow at 320/390/412 pixels.
- Deleting list abcd with 4 incomplete and 300 completed records produced exactly: List “abcd” deleted: 4 pending tasks, 300 completed tasks. Browser confirmed unrelated list/task preservation.
- Background Android verified the same 4/300 toast and SQLite removal; the screenshot shows the empty home after fixture deletion. Emulator disconnected after this screenshot, before the separate Settings capture. Header/author dimensions were measured in the browser against the same packaged UI. No test records ship in the APK.
- Physical-device testing pending. OTP/live-sharing tests remain deferred until requested.
