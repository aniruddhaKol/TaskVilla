# TaskVilla 0.31.0

Author: Aniruddha DuttaChowdhury · ADChowdhury

- Author Email now opens a draft to a.d.chowdhury@gmail.com with subject “TaskVilla Feedback / Support Request” and the approved greeting, feedback placeholder and closing. Android mail routing accepts the subject/body parameters. The user reviews and sends the email.
- Includes 0.30.0: new task and new list now show the same Discard | Save prompt as task/list editing when leaving changed forms. Cross closes only the prompt; Save validates and persists before leaving; failed saves retain drafts.
- Preserves BookShelves, existing button styles, app.adc.taskvilla, the original signing certificate, SQLite schema 3 and backup format 2. No backend/authentication or live list sync.

Verified exact email recipient/subject/body in headless browser; create/edit guard tests include validation and simulated save failures. APK signatures verified. Installed on the review phone with all database/private-file hashes unchanged during upgrade. Email-client-specific rendering and full native IME coverage were not end-to-end tested.
