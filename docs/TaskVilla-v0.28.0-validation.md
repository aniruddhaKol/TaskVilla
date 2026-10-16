# TaskVilla 0.28.0 validation

PASS — Headless Edge: empty-home counts and Favourites empty state; populated Today/Planned/Favourites counts and navigation; undated favourites included, completed tasks excluded; unstar and return refreshes counts. All three boxes remain in one row without overflow, with 2px gaps, in wood/stone/brick at widths 320/390/412px. Plain duplicate notice shown and duplicate rejected; no JavaScript errors. Screenshot visually inspected with synthetic data.

PASS — Android build; APK v2/v3 signatures verified with the original review certificate. Packaged active UI files are byte-identical to the browser preview. This release did not repeat native IME/picker/storage tests or end-to-end list sharing. No visible emulator was opened.
