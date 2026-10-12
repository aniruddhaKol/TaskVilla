# taskvilla v0.4.0 validation

Author: Aniruddha DuttaChowdhury · ADChowdhury

- Reviewed all 11 files in sample/, including original wood mobile screens, closely stacked task rows, and desktop fields used only as field references.
- Installed signed v0.4.0 over v0.3.0 in Android API 36 emulator; pre-update list and task retained.
- Created a private list with custom emoji and HEX colour. Creation has no share checkbox.
- Created and edited a task inside its list. Share/unshare moved list grouping and retained its tasks. Edited colour and sharing state persisted in SQLite across force-stop/restart.
- Checked nine colours including black, white, middle grey, saffron, green, navy and custom blue. All computed text ratios at least 4.5:1 (lowest 4.61:1). Task rows/forms use the exact list surface colour. Text-bearing captions have a solid colour surface.
- Verified list, new task, task details, options, members and invitation routes retain contextual colour. Android status/navigation bars follow list colour with readable icon brightness.
- No horizontal overflow across 22 routes on the emulator. Android Back closes a palette before leaving the options screen.
- Inspected screenshots of home, list, task, creation and options layouts; compact 44 CSS pixel row minimums and floating create-list button retained.
- Emulator test records were removed by restoring its original baseline; APK contains no seeded user data.
- JavaScript syntax, signed APK v2/v3 verification passed. Same package app.adc.taskvilla and same certificate SHA-256 fa5cf9a1d95a29fc2241b9fb090f3248cc38555cec60e992a0ba38ecc4ddfc79.

Limits: emulator validation rather than physical device testing. Actual collaboration awaits server integration. Current application is WebView + SQLite; attachment content and scheduled notifications are not implemented.

Browser preview validation passed in installed Microsoft Edge: Roboto Mono and image references loaded, list/task navigation worked, no JavaScript or failed-resource errors, no horizontal overflow at 320, 390 and 412 pixel widths. Final APK reinstall smoke checks passed with empty baseline and native colour bridge available.
