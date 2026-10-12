# taskvilla v0.6.0 validation

Author: Aniruddha DuttaChowdhury · ADChowdhury

- Package app.adc.taskvilla, versionCode 6, same review signing key; APK v2/v3 signatures verified.
- Browser preview loaded bundled fonts and images; navigation/email flow passed without JavaScript errors. Checked 320/390/412-pixel widths.
- Verified 3-pixel empty-state/task-row/input-panel corners and raised/pressed button bevel styles; inspected screenshots.
- Activity toolbar and native Android Back return to Settings.
- Banner-only list colour and icon, inverse header text with contrast fallback, neutral paper body, matching Add/Save controls, vertical menu dots, standard empty states validated.
- SQLite migration from schema 1 to 2 preserved records. Owner-only email form rejects invalid addresses and case-insensitive duplicates. Pending email records persisted across restart/update; they are not actual members or sent messages.
- Upgrade/restart checked in a headless Android API 36 emulator; test data removed by restoring its pre-test baseline.

Current app is Android WebView UI + native SQLite. Collaboration/email delivery needs the future server API. Filenames only are saved for attachments; reminder preferences do not schedule notifications. Physical-device testing is still pending.
