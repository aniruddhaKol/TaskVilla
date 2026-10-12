# TaskVilla v0.7.0 validation

Author: Aniruddha DuttaChowdhury · ADChowdhury

- Signed APK: app.adc.taskvilla, versionCode 7, display name TaskVilla; same certificate as preceding versions. APK v2/v3 signatures verified.
- Browser checks passed at 320/390/412-pixel widths with no JavaScript errors. Task name received focus after dynamic navigation; SVG star was vertically centred within 1.5 pixels of header centre.
- Flat navigation/task/appearance rows, 5-pixel corners, button bevels and removal of private-list caption/sharing checkbox verified. Inspected representative screenshots.
- Owned shared lists stayed under My Lists with right-side share markers. Incoming/joined test lists appeared only in Shared with me; no incoming records are shipped.
- Removal confirmation Cancel preserved emails; Confirm then Save removed emails in SQLite and cleared the marker when no people remained.
- Task/list deletion Cancel preserved data. Confirm deleted records in SQLite; list deletion removed its tasks and preserved unrelated lists. Joined lists have no owner-only deletion/email controls.
- Native Android Back closed a confirmation dialog. Upgrade from v0.6 retained records and email addresses. Deletion and remaining-record persistence were verified across restart.
- All native checks ran in the background Android API 36 emulator. Original test baseline was restored, removing generated fixtures.

Limits: emulator rather than physical device testing; actual sharing, invitation emails and membership require the future server API. Application remains WebView + native SQLite.
