# TaskVilla — Android review app 0.7.0

Author: **Aniruddha DuttaChowdhury** · **ADChowdhury**
Application/package and Java namespace: **app.adc.taskvilla**

## Install and preview

Open taskvilla-v0.7.0.apk on Android 8+. It updates earlier TaskVilla review builds using the same package and signing key, retaining SQLite data. Only the display name changes to TaskVilla. Fresh installs start empty; no sample lists, tasks, people or incoming shares ship with the APK.

Browser preview: C:\workspace\wunderlist-mobileapp\ui\index.html. The browser starts empty and keeps changes for the page session only. Android persists changes in native SQLite. No dependencies are needed for the preview.

## Lists and sharing

Tap Add list, choose a name, emoji and banner colour. Your own lists stay in My Lists, including lists you share with others. A share icon on the right indicates people added to your list. Shared with me contains only incoming/joined lists, and remains empty until server-backed invitations are available. There are no fake incoming records.

Open a list's vertical-dot menu for options. Its owner can add email addresses, save them, and remove people with confirmation. The separate Share this list checkbox is removed; sharing state follows the saved people list. Removing the last person clears the share marker after Save. Older manual sharing flags without invitees are normalised to this people-based model.

Email validation rejects invalid addresses and case-insensitive duplicates. Save includes an address still entered in the field. Addresses remain pending locally, are not actual members, and no emails are sent. API integration must implement invitation delivery, acceptance and server-side permissions later. Joined-list users have no owner email/delete controls.

## Tasks and deletion

Open an existing list and tap Add task; the task-name field receives focus. Tasks inherit their list. Stars are centred SVG icons, aligned in rows and task headers. Task details provide delete controls in the header and bottom of the form; deletion asks for confirmation. Delete list is in owner List options, also with confirmation, and removes the list's tasks. Cancel leaves data untouched. Edit forms use consistent Save buttons.

## Appearance

Wood is the main background, with a tiny tricolour brand accent. Only the banner uses the list colour and contrasting/inverted text. The banner includes the list icon. The private-list caption is removed. Boxes have 5-pixel corners. List items, task rows and appearance rows remain flat; action buttons have raised bevels. Floating Add list/Add task controls share their appearance. Standard empty states are retained.

Roboto Mono is bundled with its original SIL OFL/copyright notice at assets/ui/assets/fonts/RobotoMono-OFL.txt. Supplied app icon and reference designs are preserved.

## Implementation and build

Android WebView UI with native SQLite, not React Native. Schema version 2 is unchanged from v0.5/0.6; the earlier migration from schema 1 adds pending email addresses without losing records. Attachment filenames only are stored. Reminder preferences do not schedule notifications. There is no API, Firebase, authentication service or Internet permission.

Source: android-taskvilla/src/app/adc/taskvilla; packaged UI: android-taskvilla/assets/ui; preview: ui/. Build with powershell -ExecutionPolicy Bypass -File .\build.ps1 using SDK platform 36/build-tools 36.0.0 and C:\jdk-27. Release versionCode 7 is a debuggable review build. The existing review key remains in signing/taskvilla-review.keystore (alias taskvilla-review, review passwords android); keys are excluded from source ZIPs. Preserve the package and key for compatible updates.

Run open-emulator.ps1 only when the AVD is stopped to start Pixel_8_API_36 headlessly. The latest preference is background checking, without a visible emulator.

Earlier release folders and signing keys are preserved. Daylist's app.adc.wunderlistmobileapp remains a separate identity under android/; no automatic Daylist data import is performed.
