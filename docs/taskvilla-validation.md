# TaskVilla v0.22.0 validation

Author: Aniruddha DuttaChowdhury · ADChowdhury

- app.adc.taskvilla versionCode 22 signed with the existing certificate; APK v2/v3 verification passed. SQLite schema 3 and earlier releases preserved.
- Restored renderer animation directly from the preserved v0.18 source: 380ms translateX 36px/8px/0 and scale .985/.997/1, intermediate keyframe at 70%, cubic-bezier(.16,.75,.25,1). Back reverses direction.
- Browser verified title-relative tricolour at widths 320/390/412, original forward/reverse animation, rapid navigation, no leftover shared/flip layers and reduced-motion bypass. Recent Storage/Theme/picker improvements retained; no resource or JavaScript errors or horizontal overflow.
- Removed v0.20/0.21 shared-element and popup-rise effects. Home screenshot visually inspected.
- Background Android API 36 verified title band, restored 380ms pre-flip animation and backup metadata version 0.22.0. No test records inserted.
- No new dependencies, backend changes or live sharing tests.
