# TaskVilla 0.34.0 — Compact activity history

Author: Aniruddha DuttaChowdhury · ADChowdhury

- Period is now a 14px label on the same row as its dropdown.
- Replaced tracker period/activity selects with the simple SMSAnalyzer-style custom dropdown: neutral summary, selected tick, outside-tap dismissal, Escape/arrow-key selection and Android Back dismissal before navigation.
- Removed activity icons from activity-specific history cards; retained them (including fallback icons) in main History.
- Used the supplied transparent history icon in both History tabs. Original image bytes are preserved; CSS renders the icon white to match themed tab text.
- Removed the redundant top-right Edit activity action from activity details. Delete remains; each history entry still has its own Edit/Delete controls, including access to the activity title when editing an entry.
- Reduced padding, row height and internal gaps in both history layouts. Edit/Delete retain 44px targets, grey/red bevels, confirmation and pressed feedback. Alternating backgrounds, calendar/date and currency-bag/amount remain.

Package app.adc.taskvilla; versionCode 34; original signing identity, SQLite schema 4 and backup format 2 preserved. No data migration or backend changes. Local release only: nothing pushed to Git or installed on a phone.
