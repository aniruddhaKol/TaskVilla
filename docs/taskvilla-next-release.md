# TaskVilla — next-release attachment requirements

Historical planning record from 15 October 2026. Implemented in local builds 0.25–0.28; see docs/releases/v0.28.0.md and the current README for present behavior. The requirements below describe the original request, not outstanding work.

- Attach file currently does not work as expected; repair the complete attachment flow.
- Support selecting and attaching multiple files to a task.
- Maximum file size: 10 MB per file.
- Proposed maximum: 10 files per task; owner described this count as tentative.
- Show a concise attachment-limit disclaimer near the control, including actual storage/sharing limitations where relevant.
- Provide a way to share the attached files using installed applications.
- Preserve existing task data and handle cancelled selections, unreadable files and over-limit selections clearly.

v0.24.0 is published unchanged with respect to attachments. Current storage is filenames only; these requirements do not imply that binary file persistence or sharing already works.
