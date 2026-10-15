# TaskVilla v0.24.0 validation

Author: Aniruddha DuttaChowdhury · ADChowdhury

Changes:
- Complete inset focus rings for editable fields and date wrappers; task title flex sizing/padding prevents its focus rectangle overflowing the row.
- Shared keyboard.js handles visual-viewport/window resizing, reveals the focused field above the keyboard and fixed Save bar, and restores prior scroll positions after dismissal. Handles nested picker/dialog scrolling and preserves typed values.
- Android keyboard bridge now supports all editable text/number/email/search fields, rather than only the new-task title. System adjustResize remains enabled; no double subtraction of IME height.

Passed:
- Headless browser checks at 320/390/412px: title outline contained; simulated keyboard viewport keeps task notes, list name and emoji popup field visible; switching fields, create-task autofocus, retained edits and scroll restoration pass without JavaScript errors.
- Browser keyboard/picker screenshots visually inspected. These images simulate reduced viewport height; they do not show an Android keyboard.
- Android build and existing signing certificate verified with APK signature schemes v2/v3. Package app.adc.taskvilla; versionCode 24. Preview/packaged assets match.

Native validation limitation:
- APK installed successfully in the headless Android API 36 emulator. Native field outline computed style confirmed as an inset -2px ring.
- Real IME coverage/restoration test did NOT pass: the emulator's network stack/system process crashed during startup, and later System UI reported not responding. mInputShown remained false; no real keyboard viewport was available to validate. No claim of successful native keyboard testing is made.
- Test records existed only in memory with persistence disabled and were removed afterward. No live sharing tests, dependencies, SQLite schema or backend changes.
