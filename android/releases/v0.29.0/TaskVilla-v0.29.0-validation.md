# TaskVilla 0.29.0 validation

PASS — Headless browser: 2×2 theme choices at 320/390/412px; one selected tick; BookShelves background, header, checked toggle, action-bar backing and toast; all four theme headers; original button surfaces and list-colour banner; saved selection survives reload through a simulated persistence bridge; no JavaScript errors. Settings/home screenshots visually inspected. Browser persistence test does not alone establish native SQLite behavior.

PASS — APK build and v2/v3 signature verification with the original review certificate. Active UI and supplied BookShelves image match the APK assets byte for byte. No database/schema/signing changes. Installation status is recorded separately in installation-result.md. No live list-sharing or IME tests were repeated.
