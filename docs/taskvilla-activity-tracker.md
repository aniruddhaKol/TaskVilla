# TaskVilla 0.32.0 review guide

Install taskvilla-v0.32.0.apk on Android 8+ over your existing TaskVilla review app. The package and key are unchanged; do not uninstall if you want to keep local data. The database upgrades to schema 4. Older APKs do not implement a database downgrade.

List ordering: Add list or List options → Latest task on top → Save. The option controls where future tasks are inserted. Existing manual order is not reversed.

Comments: open a task, type under Notes and tap Add comment. The newest comment is first. Grey edit and red trash controls act on each comment. Save commits the task draft; Back offers Discard | Save. Typing a comment and tapping Save also posts that text. The limits are 100 characters for the task name and 280 for each new/edited comment.

Activity Tracker: use the new home button. Add activity, type a catalogue name or your own title, choose last done date, amount, repeat interval and optional notes, then Save. A new title becomes a personal suggestion. Set 0 days for no fixed schedule. Scheduled and overdue activities appear under Upcoming; All also includes unscheduled activities. Open an activity for details/history, use Add entry for the next occurrence, and use grey/red controls to edit or delete with confirmation. All history and Trends provide activity/period filters. History is ordered by date, newest first; the latest dated entry determines the summary and next due date.

Everything in Activity Tracker stays local and personal. No sharing, sign-in or backend is involved. The catalogue contains suggestions only, not prefilled records. Export includes tracker history and custom suggestions with other local data; attachment bytes remain excluded. Browser preview is ui/index.html and is session-only unless a native bridge is provided.

Author: Aniruddha DuttaChowdhury · ADChowdhury.
