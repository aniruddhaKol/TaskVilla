# TaskVilla v0.21.0 validation

Author: Aniruddha DuttaChowdhury · ADChowdhury

- app.adc.taskvilla versionCode 21 signed with the preserved certificate; APK v2/v3 verification passed. SQLite schema 3 retained and earlier releases preserved.
- Browser verified Storage: On this device, Export spacing at 320/390/412px, Theme title case, picker search/close clearance of at least 12px, pale-green Use and working emoji selection, Show more icon and 120-to-240 pagination.
- Verified shared list animation duration 650ms and task/popup durations 600ms, Back direction, task autofocus, rapid navigation cleanup and reduced-motion bypass. Responsive checks passed without JavaScript/resource errors or overflow. Settings and picker screenshots inspected.
- Background Android API 36 verified settings labels, picker gap/green Use/more icon, 650ms shared-element motion and cleanup, task slide, form rise with autofocus and popup rise. Original SQLite data restored after checking.
- Backup metadata reports 0.21.0. No seeded data, new dependencies, backend changes or live sharing tests.
