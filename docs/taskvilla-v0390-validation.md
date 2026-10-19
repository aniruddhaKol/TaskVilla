# TaskVilla 0.39.0 validation

- Browser: Settings card gaps measured 2px; selected background visible; Offline SQLite removed; help artwork loads; cream fieldsets, themed heading, scrolling and close/Escape/focus return passed for Wood, Stone, Brick and BookShelves at 320/390/412px. No JavaScript errors. Synthetic screenshots reviewed.
- Build: original package app.adc.taskvilla, versionCode 39, versionName 0.39.0; APK signature verified with original certificate fa5cf9a1d95a29fc2241b9fb090f3248cc38555cec60e992a0ba38ecc4ddfc79. Active packaged UI assets match source.
- Phone: adb install -r succeeded. SHA-256 inventory of existing private database/files unchanged immediately after installation. Installed package reports 0.39.0; activity launch requested.
- Limitation: phone was locked (lockscreen/NotificationShade). WebView CDP connection timed out, so live help rendering and native Android Back behavior were not verified. Android Back help dismissal is implemented; browser close and Escape were verified.
- No Git push, GitHub release, schema change, device-data wipe or backend integration.
