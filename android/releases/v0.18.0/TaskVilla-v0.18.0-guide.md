# TaskVilla — Android review app 0.18.0

Author: **Aniruddha DuttaChowdhury** · **ADChowdhury**
Application/package and Java namespace: **app.adc.taskvilla**

## Install and preview

Open taskvilla-v0.18.0.apk on Android 8+. It updates earlier TaskVilla review builds using the same package and signing key, retaining SQLite data. Fresh installs start empty; no sample lists, tasks, people or incoming shares ship with the APK.

Browser preview: C:\workspace\wunderlist-mobileapp\ui\index.html. The browser starts empty and keeps changes for the page session only. Android persists changes in native SQLite. No dependencies are needed for the preview.

## Lists and sharing

Tap Add list, choose a name, emoji and banner colour. Your own lists stay in My Lists, including lists you share with others. A standard group-of-people icon on the right indicates saved pending invitees. Tap the list row to open it. Its rightmost badge uses the list colour and displays the open-task count in inverted text (black/white fallback when needed for contrast). Shared with me contains only incoming/joined lists, and remains empty until server-backed invitations are available. There are no fake incoming records.

Open a list's vertical-dot menu for options. Its owner can add email addresses, save them, and remove people with confirmation. The separate Share this list checkbox is removed; sharing state follows the saved people list. Removing the last person clears the share marker after Save. Older manual sharing flags without invitees are normalised to this people-based model.

Email validation rejects invalid addresses and case-insensitive duplicates. Save includes an address still entered in the field. Addresses remain pending locally, are not actual members, and no emails are sent. API integration must implement invitation delivery, acceptance and server-side permissions later. Joined-list users have no owner email/delete controls.

## Tasks and deletion

Open an existing list and tap Add task; the task-name field receives focus. Tasks inherit their list. Stars are centred SVG icons, aligned in rows and task headers. Task details provide one delete icon in the top-right header; deletion asks for confirmation. Delete list is in the owner list three-dot menu, also with confirmation, and removes the list's tasks. Cancel leaves data untouched. Creation and edit forms use Save buttons with a floppy-disk icon; Add list/Add task start the forms. Successful deletions display bottom-centred Task deleted or List deleted toasts. Empty required names focus the field and show a red border; typing clears the error. Optional fields remain optional.

Drag the six-dot handle to reorder tasks within a list and the same completion group. The lifted task card follows the pointer/finger, with an empty placeholder and animated neighbouring rows. The lifted task card follows the pointer/finger, with an empty placeholder and animated neighbouring rows. Order persists in SQLite across refresh and restart. Keyboard review also supports Arrow Up/Down on a focused handle. Pull down at the top of the home/list/shared screen to reload local data. Pull/release indicators have a soft bounce and animated entry/exit; reduced-motion settings suppress animation. Pull/release indicators have a soft bounce and animated entry/exit; reduced-motion settings suppress animation. The refresh message explicitly says server sync is not connected; API synchronization comes later.

## Appearance

Wood is the default background; Settings also offers Stone wall and Brick wall, saved immediately in SQLite and retained across app restart. Wood is the original background, with a tiny tricolour brand accent. Only the banner uses the list colour and contrasting/inverted text. The banner includes the list icon. The private-list caption is removed. Boxes have 5-pixel corners. List items, task rows and appearance rows remain flat; action buttons have raised bevels. Rows have an additional one-pixel separation. Add list/Add task, Save and the people-add plus button use pale green bevels; delete actions use pale red. Back, the Settings gear and confirmation Cancel buttons use pale ash bevels. List colour applies only to the app header band; Android status/navigation bars stay black and independent of the app/list theme. Settings includes a reference-inspired developer icon and bottom Built by author credit linking ADChowdhury to https://www.linkedin.com/in/adchowdhury/. Android opens this link through ACTION_VIEW in the device browser. Standard empty states are retained.

Roboto Mono is bundled with its original SIL OFL/copyright notice at assets/ui/assets/fonts/RobotoMono-OFL.txt. Supplied app icon and reference designs are preserved.

## Future sharing requirements

See docs/taskvilla-sharing-flow.md. Sharing will require email OTP sign-in, with the same flow for login and registration. Invitations need accept/ignore, members can decline later, and owners can remove members. Pending invitations must not appear on the home screen. This build has no authentication or network sharing. End-to-end sharing tests are deferred until the user explicitly requests them at the end.

## Implementation and build

Android WebView UI with native SQLite, not React Native. Schema version 3 adds persisted task sort order, preserving existing records. The earlier schema 1 migration adds pending email addresses. Attachment filenames only are stored. Reminder preferences do not schedule notifications. There is no API, Firebase, authentication service or Internet permission.

Source: android-taskvilla/src/app/adc/taskvilla; packaged UI: android-taskvilla/assets/ui; preview: ui/. Build with powershell -ExecutionPolicy Bypass -File .\build.ps1 using SDK platform 36/build-tools 36.0.0 and C:\jdk-27. Release versionCode 18 is a debuggable review build. The existing review key remains in signing/taskvilla-review.keystore (alias taskvilla-review, review passwords android); keys are excluded from source ZIPs. Preserve the package and key for compatible updates.

Run open-emulator.ps1 only when the AVD is stopped to start Pixel_8_API_36 headlessly. The latest preference is background checking, without a visible emulator.

Earlier release folders and signing keys are preserved. Daylist's app.adc.wunderlistmobileapp remains a separate identity under android/; no automatic Daylist data import is performed.

## Version 0.11 update

Stone wall and Brick wall use the supplied photographic references, with their aspect ratios preserved. Today and Planned have no trailing arrows; tap the rows to open them. List appearance now says Choose theme; this controls the list banner/count colour. App wallpaper selection remains in Settings.

## Version 0.12 update

The launcher/in-app icon is a wooden house with two checklist rows, generated using the imagegen skill; original icon assets and older releases are preserved. List three-dot menu offers Copy, Details and Delete. Copy puts only incomplete task names on the clipboard, one per line, in the current saved order, with no heading, bullets or metadata. An empty list reports that there is nothing to copy. Delete is removed from list details, and task deletion remains only in the top-right icon. Deletion still asks for confirmation.

## Version 0.13 update

The wooden icon restores its original 1254×1254 canvas, rounded squircle with transparent corners, and a compact three-person group. Two checklist rows remain. Prior icon assets/releases are preserved. Header Back/Settings/More buttons have consistent 36-pixel boxes and 20-pixel symbols. Built by icon and label share one row. List deletion reports the list name and exact pending/completed task counts, remaining visible for six seconds.

## Version 0.14 update

Due dates show dd-MMM-yyyy (for example 05-Oct-2026) on task rows, task forms and planned headings; storage remains ISO yyyy-MM-dd. The transparent native date control still opens the standard date picker. Subtasks are hidden; saving existing tasks preserves their stored subtasks. Task top-right delete is a flat red icon on pale red, matching the list deletion icon colour. Back is a compact 30-pixel button with a fuller 26-pixel arrow. Screen navigation uses a subtle 220ms slide/fade and honours reduced-motion preference; same-screen rerenders stay still. The wooden icon uses more top/left space and a larger light group of three people. Previous art is preserved.

## Version 0.15 update

Settings follows the supplied reference with app identity, storage/reminder preferences, theme previews, Activity and an author/contact card. The supplied signature is retained unchanged. Email: a.d.chowdhury@gmail.com; phone: +919987484090. Connect opens LinkedIn; Email opens a composer; phone opens the dialler; WhatsApp opens the supplied number. No message or call is sent automatically. Back keeps the existing compact pale-ash styling. Custom colour sliders now have red, green and blue tracks and thumbs matching their channels.

## Version 0.16 update

Task importance controls are vertically aligned. The list More button matches the compact pale-ash Back button; task Delete uses the red bevel. Copy opens a popup for newline-separated incomplete task names or a one-column HTML table with plain-text fallback, in saved order. HTML escapes task names. Paste the table into an HTML-capable email editor or Excel; receiving clients may choose the plain-text fallback. The author contact line is hidden, signature/contact icons are 30% smaller, and Call opens the default dialler without placing a call. Email and LinkedIn actions retain their destinations.

## Version 0.17 update

Android status and navigation bars remain neutral black; app and list themes only affect app content. Extensive copy includes a list caption and column headers for Task, Status, Important, Due date, Reminder, Tags, Notes, Assigned to, saved Subtasks and non-image Files, for incomplete tasks in saved order. Images are excluded by filename extension; attachments in this review build are stored as filenames. HTML is escaped and accompanied by a tab-separated plain-text table fallback. Plain text list remains task names only. Raised buttons depress gently when tapped; list/task rows show a soft tap effect without delaying navigation or affecting drag gestures. Reduced-motion preference is honoured.

## Version 0.18 update

Home app icon is 40px. Screen navigation uses a stronger 380ms slide/fade with subtle scale; reduced-motion remains respected. Settings Storage row includes Export. JSON and XML format buttons share one row in its popup. Android opens the system create-document picker; browser preview starts a download. Backups contain all lists and tasks (including completed), IDs, list references, ordering, colours/icons, invitee records, hidden subtasks, filenames and theme/reminder preferences. Metadata includes TaskVilla, app version, package, author, UTC export timestamp, device timezone, format version, SQLite schema version and record counts. The typed XML preserves JSON object/array/number/boolean/null/string types. Restore UI is not implemented yet; backup restore compatibility was checked through SQLite. Actual attachment bytes are not stored by this review app and therefore are not in the backup. See docs/taskvilla-backup-format.md.
