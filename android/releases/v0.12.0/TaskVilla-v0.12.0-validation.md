# TaskVilla v0.12.0 validation

Author: Aniruddha DuttaChowdhury · ADChowdhury

- Built/signed app.adc.taskvilla versionCode 12, existing review certificate; background Android update passed. SQLite schema unchanged at 3.
- Wooden house icon generated using imagegen; exactly two checklist rows. Inspected full-resolution art and small in-app/launcher assets. Original colourful assets are preserved.
- Headless browser passed Copy/Details/Delete menu, exact newline-separated incomplete-name clipboard payload (excluding completed and unrelated tasks), empty-copy status, dismissal, no delete in list details, and one top-right task deletion control.
- Confirmation Cancel preserved data; confirmation Delete removed only the selected list/tasks, preserving unrelated data. Responsive checks passed at 320/390/412 pixels without resource/JavaScript failures.
- Background Android actual clipboard paste verified the exact two incomplete task names, separated by a newline. Native menu/details checks passed. Original empty baseline restored; no sample data shipped.
- Physical-device testing is pending. OTP/live-sharing tests remain deferred until requested. Server APIs are not connected.
