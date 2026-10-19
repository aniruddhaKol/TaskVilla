# TaskVilla 0.37.0 — Average and history order

Author: Aniruddha DuttaChowdhury · ADChowdhury

Average spending rounds upward to the next whole rupee, without decimal places. Lowest, Highest, Total and recorded amounts retain their existing precision.

All history views share a newest-first comparator: activity date descending, then latest creation time first for entries on the same date. Missing old creation timestamps are handled safely. Saving an entry clears stale history scroll positions so the updated history starts at the top.

Package app.adc.taskvilla; versionCode 37; original signing identity, SQLite schema 4 and backup format 2 unchanged. Local release only; no Git publication or phone installation.
