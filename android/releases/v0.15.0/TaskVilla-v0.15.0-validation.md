# TaskVilla v0.15.0 validation

Author: Aniruddha DuttaChowdhury · ADChowdhury

- Signed app.adc.taskvilla versionCode 15 using the existing review certificate; SQLite schema remains 3. No dependencies added.
- Browser checks passed for four Settings cards, the unchanged signature image, exact LinkedIn/email/phone/WhatsApp links, reminder preference, theme selection and Activity return to Settings.
- Back remains 30px with its existing arrow styling. No horizontal overflow or JavaScript/resource failures at 320, 390 and 412px widths.
- RGB tracks and thumbs use their channel colours. Changing channels to 200/80/100 produced #C85064. Settings and picker screenshots inspected.
- Background Android API 36 check passed: signature loaded, compact Back retained and theme selection persisted through the SQLite bridge. Reminder control was exercised. Original local baseline restored after testing; no seed data added.
- External contact routing compiled successfully; destination URLs were verified without placing calls or sending messages. Actual external app handoff was not exercised.
- Physical device and live sharing tests remain pending. OTP/sharing end-to-end tests remain deferred until requested. Server API is not connected.
