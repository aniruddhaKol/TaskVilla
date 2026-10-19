# TaskVilla 0.34.0 validation — 18 October 2026

PASS — Headless Edge: inline Period/dropdown centres align, label 14px; custom dropdown selection, selected tick, outside-click close, Escape and keyboard routing; filter updates chart/statistics correctly. Evaluated the exact Android Back JavaScript from MainActivity: open dropdown closes while staying on the page. This is browser execution of the handler, not a physical-device Back test.

PASS — Supplied 512px icon loads and appears in both history tabs, with white CSS tint across themes. Main History retains activity/default icons; detail history omits them. Both history card layouts are shorter than 0.33.0 with the same 44px action targets. Extra header edit removed; entry editing remains operational. Unchanged editor leaves directly. Saved activity records survive reload through a simulated native bridge.

PASS — Wood/Stone/Brick/BookShelves at 320/390/412px; no horizontal overflow; dropdown menus stay inside the viewport. Synthetic screenshots visually inspected. No JavaScript errors.

PASS — Final APK built and v2/v3 signatures verified with the original review certificate. Active UI assets and supplied history icon are byte-identical to APK contents. SQLite schema/storage are unchanged. No native migration, date-dialog or full IME regression was repeated for this UI change; prior coverage is retained in 0.32.0. No phone installation or Git publication.
