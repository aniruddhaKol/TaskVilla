# TaskVilla 0.42.0 — Reverse task order and paired selectors

Author: Aniruddha DuttaChowdhury · ADChowdhury

Changing Latest task on top now reverses that list's existing task order when saved. Switching it off reverses the current order back. Unchanged saves do not reverse again; newly created tasks still go first while enabled. Other lists retain their positions. Discard or persistence failure preserves the original task order and saved preference.

List options displays icon and theme selectors in the same row, each with a down arrow on the right. Existing theme and picker behavior retained.

Package app.adc.taskvilla; versionCode 42; original signing identity and SQLite schema 4 retained. Build/signature verified. Focused browser checks passed for reversal, repeated save, new-task insertion, switching off, save-failure rollback, discard, other-list positions and paired selectors at 320/390/412px. No phone installation, Git push or GitHub release.
