# TaskVilla — Android review app 0.9.0

Author: **Aniruddha DuttaChowdhury** · **ADChowdhury**
Application/package and Java namespace: **app.adc.taskvilla**

## Install and preview

Open taskvilla-v0.9.0.apk on Android 8+. It updates earlier TaskVilla review builds using the same package and signing key, retaining SQLite data. Fresh installs start empty; no sample lists, tasks, people or incoming shares ship with the APK.

Browser preview: C:\workspace\wunderlist-mobileapp\ui\index.html. The browser starts empty and keeps changes for the page session only. Android persists changes in native SQLite. No dependencies are needed for the preview.

## Lists and sharing

Tap Add list, choose a name, emoji and banner colour. Your own lists stay in My Lists, including lists you share with others. A standard group-of-people icon on the right indicates saved pending invitees. Tap the list row to open it. Its rightmost badge uses the list colour and displays the open-task count in inverted text (black/white fallback when needed for contrast). Shared with me contains only incoming/joined lists, and remains empty until server-backed invitations are available. There are no fake incoming records.

Open a list's vertical-dot menu for options. Its owner can add email addresses, save them, and remove people with confirmation. The separate Share this list checkbox is removed; sharing state follows the saved people list. Removing the last person clears the share marker after Save. Older manual sharing flags without invitees are normalised to this people-based model.

Email validation rejects invalid addresses and case-insensitive duplicates. Save includes an address still entered in the field. Addresses remain pending locally, are not actual members, and no emails are sent. API integration must implement invitation delivery, acceptance and server-side permissions later. Joined-list users have no owner email/delete controls.

## Tasks and deletion

Open an existing list and tap Add task; the task-name field receives focus. Tasks inherit their list. Stars are centred SVG icons, aligned in rows and task headers. Task details provide delete controls in the header and bottom of the form; deletion asks for confirmation. Delete list is in owner List options, also with confirmation, and removes the list's tasks. Cancel leaves data untouched. Creation and edit forms use Save buttons with a floppy-disk icon; Add list/Add task start the forms. Successful deletions display centred Task deleted or List deleted toasts. Empty required names focus the field and show a red border; typing clears the error. Optional fields remain optional.

Drag the six-dot handle to reorder tasks within a list and the same completion group. The lifted task card follows the pointer/finger, with an empty placeholder and animated neighbouring rows. Order persists in SQLite across refresh and restart. Keyboard review also supports Arrow Up/Down on a focused handle. Pull down at the top of the home/list/shared screen to reload local data. Pull/release indicators have a soft bounce and animated entry/exit; reduced-motion settings suppress animation. The refresh message explicitly says server sync is not connected; API synchronization comes later.

## Appearance

Wood is the main background, with a tiny tricolour brand accent. Only the banner uses the list colour and contrasting/inverted text. The banner includes the list icon. The private-list caption is removed. Boxes have 5-pixel corners. List items, task rows and appearance rows remain flat; action buttons have raised bevels. Rows have an additional one-pixel separation. Add list/Add task, Save and the people-add plus button use pale green bevels; delete actions use pale red. The Settings gear uses a contrasting cream button with a clear lower edge. Settings includes a bottom author credit linking ADChowdhury to https://www.linkedin.com/in/adchowdhury/. Android opens this link through ACTION_VIEW in the device browser. Standard empty states are retained.

Roboto Mono is bundled with its original SIL OFL/copyright notice at assets/ui/assets/fonts/RobotoMono-OFL.txt. Supplied app icon and reference designs are preserved.

## Future sharing requirements

See docs/taskvilla-sharing-flow.md. Sharing will require email OTP sign-in, with the same flow for login and registration. Invitations need accept/ignore, members can decline later, and owners can remove members. Pending invitations must not appear on the home screen. This build has no authentication or network sharing. End-to-end sharing tests are deferred until the user explicitly requests them at the end.

## Implementation and build

Android WebView UI with native SQLite, not React Native. Schema version 3 adds persisted task sort order, preserving existing records. The earlier schema 1 migration adds pending email addresses. Attachment filenames only are stored. Reminder preferences do not schedule notifications. There is no API, Firebase, authentication service or Internet permission.

Source: android-taskvilla/src/app/adc/taskvilla; packaged UI: android-taskvilla/assets/ui; preview: ui/. Build with powershell -ExecutionPolicy Bypass -File .\build.ps1 using SDK platform 36/build-tools 36.0.0 and C:\jdk-27. Release versionCode 9 is a debuggable review build. The existing review key remains in signing/taskvilla-review.keystore (alias taskvilla-review, review passwords android); keys are excluded from source ZIPs. Preserve the package and key for compatible updates.

Run open-emulator.ps1 only when the AVD is stopped to start Pixel_8_API_36 headlessly. The latest preference is background checking, without a visible emulator.

Earlier release folders and signing keys are preserved. Daylist's app.adc.wunderlistmobileapp remains a separate identity under android/; no automatic Daylist data import is performed.
