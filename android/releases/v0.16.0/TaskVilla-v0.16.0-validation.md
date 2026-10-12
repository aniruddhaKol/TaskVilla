# TaskVilla v0.16.0 validation

Author: Aniruddha DuttaChowdhury · ADChowdhury

- Signed app.adc.taskvilla versionCode 16 with the existing review certificate; SQLite schema remains 3. No dependencies added; previous releases preserved.
- Browser verified checkbox/star/text centres on Create task and star/Delete centres on Edit task; screenshots inspected.
- More and Back match in size, colour, gradient, borders, radius and shadow. Task Delete has the pale-red bevel.
- Copy popup has two format buttons. Plain text contains only incomplete titles in saved order; HTML has one table cell per title, escapes special characters and includes equivalent plain-text fallback.
- Background Android API 36 passed actual native HTML and plain-text clipboard bridge calls through the popup. Clipboard destination paste in Excel/email was not exercised.
- Signature and contact icons reduced 30%; visible contact detail line removed. Exact mailto and dialler URLs verified; WhatsApp action replaced with Call. No call placed.
- No browser JavaScript/resource errors or horizontal overflow at 320/390/412 widths. Native test baseline restored; no seed data included.
- Live OTP/sharing end-to-end tests remain deferred until requested. No server API added.
