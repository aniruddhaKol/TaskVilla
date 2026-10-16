# TaskVilla backup format 1

Author: Aniruddha DuttaChowdhury · ADChowdhury

Export captures all local lists and tasks, including completed tasks, in current saved order. Format 1 contains metadata and data. Metadata: format=TaskVillaBackup, formatVersion=1, dataSchemaVersion=3, appName, appVersion, applicationId, author, exportedAt (UTC ISO 8601), device timeZone, attachmentStorage=filenames-only, listCount and taskCount. Data: lists, tasks (each sortOrder set to its saved array position), state with persisted theme and notify preferences. IDs and task.list references remain intact.

JSON uses ordinary typed objects and arrays. XML has taskvillaBackup as root; type attributes describe object, array, number, boolean, null or string. Object children are field elements with a name attribute; array children are item elements. Text is XML escaped. Empty string, empty array and null remain distinct. The format intentionally preserves hidden subtasks and all stored task/list properties. File/photo fields contain filenames, not image bytes.

A future restore implementation must validate format/package/version, support or migrate the data schema, decode XML without external entities, validate unique IDs/list references, handle conflicts explicitly, and write lists/tasks/preferences atomically. Current UI provides export only. Restore compatibility was tested by decoding both exports and writing their data through the existing SQLite transaction.

Android uses ACTION_CREATE_DOCUMENT without broad storage permissions. The user selects the destination; success/cancel/error is reported afterwards. Browser review downloads a UTF-8 Blob. Filename is TaskVilla-backup-{UTC timestamp with safe separators}.json or .xml.

## Format 2 — TaskVilla 0.25.0

New exports use formatVersion 2 and attachmentStorage `metadata-only; file bytes excluded`. SQLite schema remains 3. Each task.files entry can be a preserved legacy filename string or a metadata object with id (UUID), name, size (bytes), and mime. The actual file bytes live in private Android storage and are not included in JSON/XML exports. Restore implementations must not claim to restore attachment bytes from metadata alone; missing attachments must be labelled unavailable or reattached. Format 1 remains the historical filename-only format.

TaskVilla 0.27 adds optional `crc32` to attachment metadata: eight lowercase hexadecimal digits representing CRC32 of the file bytes. Matching size and CRC32 prevent duplicate attachments within a task only. This optional metadata does not change formatVersion 2 and does not include file bytes. Older native attachment sidecars are checksummed on demand.
