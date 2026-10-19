# TaskVilla 0.33.0 — Activity Tracker review

Author: Aniruddha DuttaChowdhury · ADChowdhury

- Search activities has a visible search icon. Removed the Personal / On this device caption from the tracker screen.
- Upcoming, All and History use SMSAnalyzer-style segmented tabs with themed container, selected bevel and pressed feedback. Activity detail uses History and singular Trend tabs. Tabs support arrow keys, Home/End and accessible selected/panel state.
- Activity edit-screen Delete now uses the same pale-red bevel as the other delete controls. Edit remains grey. Existing confirmation and draft behavior are preserved.
- Removed the All history and Trends footer buttons. The per-activity History tab now shows every entry instead of only the first five. Main History still covers all activities.
- History cards have alternating neutral backgrounds. Every item has its activity icon or a default pin. Per-activity cards show calendar/date at upper left and a rupee money bag/amount at lower left. Main history adds the activity title above the date. Edit is at upper right and Delete at lower right. Calendar icons identify the displayed date; use Edit to change it.
- Trend is a line graph of individual entries: actual date on X and amount on Y. Entries within the same month remain separate; X spacing uses elapsed days. Same-day entries retain individual points at the same date. One entry shows a point and a short message; empty periods show an empty state. Period filtering is available inside the Trend tab.
- Replaced metric boxes and the repeated monthly-total list with one striped table ordered Lowest, Average, Highest, Total. Statistics use entries in the selected period. Date/amount tooltips remain on chart points.

Package app.adc.taskvilla; versionCode 33; original signing identity, SQLite schema 4 and backup format 2 preserved. No data migration or backend changes. This release is local only: no Git push/publication or phone installation.
