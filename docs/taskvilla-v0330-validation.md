# TaskVilla 0.33.0 validation — 18 October 2026

PASS — Headless Edge: visible search icon and live filtering; removed tracker caption; accessible tab selection, keyboard routing and SMS-style selected/unselected surfaces in Wood/Stone/Brick/BookShelves at 320/390/412px; no horizontal overflow. Activity edit Delete has the red gradient/text/shadow. All per-activity history remains accessible after footer removal.

PASS — Main and per-activity history: correct title/date/money structure; calendar and rupee-bag icons; every item has an activity/default icon; edit above delete on the right; alternate row colours. Confirmed action callbacks and underlying storage were not changed.

PASS — Per-entry graph retains two different dates in one month, uses proportional time spacing for uneven intervals, preserves same-day points and handles one/zero entries without invalid coordinates. X date labels and Y amount labels present. Summary values tested for ₹750/₹2,050: Lowest ₹750, Average ₹1,400, Highest ₹2,050, Total ₹2,800. Table stripes alternate. No old metric blocks or duplicated monthly totals. Saved tracker data survives reload through a simulated native bridge. No JavaScript errors. Synthetic screenshots visually inspected.

PASS — Final APK built; original certificate and v2/v3 signature verified; active UI assets byte-identical to the APK. SQLite schema 4 and native code unchanged from 0.32.0. No fresh native migration/picker/IME tests or physical-device tests were required/run for this UI-only change; prior native coverage is in the 0.32.0 report. No phone installation or Git operation performed.
