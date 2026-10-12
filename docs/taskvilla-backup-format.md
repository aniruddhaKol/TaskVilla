# TaskVilla backup format 1

Author: Aniruddha DuttaChowdhury · ADChowdhury

Export captures all local lists and tasks, including completed tasks, in current saved order. Format 1 contains metadata and data. Metadata: format=TaskVillaBackup, formatVersion=1, dataSchemaVersion=3, appName, appVersion, applicationId, author, exportedAt (UTC ISO 8601), device timeZone, attachmentStorage=filenames-only, listCount and taskCount. Data: lists, tasks (each sortOrder set to its saved array position), state with persisted theme and notify preferences. IDs and task.list references remain intact.

JSON uses ordinary typed objects and arrays. XML has taskvillaBackup as root; type attributes describe object, array, number, boolean, null or string. Object children are field elements with a name attribute; array children are item elements. Text is XML escaped. Empty string, empty array and null remain distinct. The format intentionally preserves hidden subtasks and all stored task/list properties. File/photo fields contain filenames, not image bytes.

A future restore implementation must validate format/package/version, support or migrate the data schema, decode XML without external entities, validate unique IDs/list references, handle conflicts explicitly, and write lists/tasks/preferences atomically. Current UI provides export only. Restore compatibility was tested by decoding both exports and writing their data through the existing SQLite transaction.

Android uses ACTION_CREATE_DOCUMENT without broad storage permissions. The user selects the destination; success/cancel/error is reported afterwards. Browser review downloads a UTF-8 Blob. Filename is TaskVilla-backup-{UTC timestamp with safe separators}.json or .xml.
