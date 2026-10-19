# TaskVilla 0.38.0 — Trend display and neutral feedback

Author: Aniruddha DuttaChowdhury · ADChowdhury

All Trend amounts now round upward to whole rupees: Lowest, Average, Highest, Total, graph point labels/tooltips and axis values. Graph points use the displayed whole-rupee amounts. Calculations continue to use exact recorded paise before display rounding; stored records, history and edit values retain their precision. Axis scale keeps whole-rupee tick labels distinct for small values.

Create list and List options now have an equal-width action row: grey Cancel on the left, green Save on the right. Cancel follows the existing Back/unsaved-change guard. Clean forms leave directly; changed forms offer Discard | Save.

Pull-to-refresh messages are white with dark text, and bottom toasts are black with white text. Neither uses theme textures or colours; animation and placement remain unchanged. Other app surfaces keep the selected theme.

Package app.adc.taskvilla; versionCode 38; original signing identity, SQLite schema 4 and backup format 2 unchanged. Local release only; no Git publication or phone installation.
