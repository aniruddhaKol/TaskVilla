# taskvilla 0.3.0 — icon and picker validation

Author: Aniruddha DuttaChowdhury · ADChowdhury
Package: app.adc.taskvilla
Validated 9 October 2026 on the Pixel 8 API 36 Android emulator.

- PASS: build and v2/v3 APK signature verification; versionCode 3 / versionName 0.3.0.
- PASS: same package and signing certificate as taskvilla 0.2.0; previous list/task records survive APK update.
- PASS: extracted app icon is displayed by the Android launcher and in the app header.
- PASS: searchable emoji categories, multi-person family emoji and custom keyboard emoji.
- PASS: invalid custom emoji is rejected without losing the list-name/sharing draft.
- PASS: preset colour, custom HEX and RGB sliders; invalid HEX feedback.
- PASS: chosen emoji and HEX colour saved to SQLite and rendered on the list row; persisted after force-stop and restart.
- PASS: legacy folder SVG icon and navy colour token continue to work for existing records.
- PASS: Android hardware Back closes the palette and retains the draft.
- PASS: 21 screen routes without horizontal overflow.
- PASS: first-run arrays remain empty; emulator test records removed by restoring the pretest emulator snapshot.

No database migration or server integration was introduced. Physical-device testing is still for the user. Emoji glyphs are rendered by the device's emoji font and may vary between Android versions. The app remains an Android WebView UI with native SQLite.
