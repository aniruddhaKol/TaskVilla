# TaskVilla — next-release attachment requirements

Requested by the owner on 15 October 2026. Planning record only; do not implement until the next release is authorized.

- Attach file currently does not work as expected; repair the complete attachment flow.
- Support selecting and attaching multiple files to a task.
- Maximum file size: 10 MB per file.
- Proposed maximum: 10 files per task; owner described this count as tentative.
- Show a concise attachment-limit disclaimer near the control, including actual storage/sharing limitations where relevant.
- Provide a way to share the attached files using installed applications.
- Preserve existing task data and handle cancelled selections, unreadable files and over-limit selections clearly.

v0.24.0 is published unchanged with respect to attachments. Current storage is filenames only; these requirements do not imply that binary file persistence or sharing already works.

## Implemented in v0.25.0

The owner authorized this work: multi-file local storage, 10 MB decimal per file, maximum 10 per task, neutral disclaimer/yellow icon and Android file sharing. Legacy filename-only records remain preserved and labelled unavailable. See current validation notes for tested behavior and limits.

## Pending — approved toast wording for the next build (30 October 2026)

Use `docs/taskvilla-revised-toast-messages.tsv` as the exact approved message-to-scenario mapping. Apply these revisions in the next authorized build, covering JavaScript and native Android toast/error paths. Preserve placeholders and scenario distinctions, including separate sync-success messages with/without conflicts and the 6-second list-deletion toast. Do not change behavior, limits, or server error handling just to revise wording. This supersedes earlier message lists for the listed scenarios.

Stored only at this request: no application code changes, build, deployment, Git push or release.

## Applied in v0.58.15 (31 October 2026)

The approved revised toast messages were applied to JavaScript and native Android message paths. Returned service errors retain their existing handling. No behavior or limits were changed.

## Pending — preserve scroll when changing themes (31 October 2026)

When selecting or applying a theme, preserve the current page and scroll position instead of jumping to the top. Restore the same position after rendering the selected theme so the user retains context. Record only; do not implement until a future change/build is authorized.
