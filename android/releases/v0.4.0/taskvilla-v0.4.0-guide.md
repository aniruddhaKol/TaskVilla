# taskvilla — Android local review build 0.4.0

Author: **Aniruddha DuttaChowdhury** · **ADChowdhury**
Application identifier and Java namespace: **app.adc.taskvilla**

## Install and preview

Open taskvilla-v0.4.0.apk on Android 8 or newer. Installing over taskvilla 0.2/0.3 with this same signing key retains local data. Fresh installs start with zero lists and tasks. Earlier releases and signing keys are preserved.

Browser preview: open C:\workspace\wunderlist-mobileapp\ui\index.html. It starts empty; browser-only data lasts for the current page session. The Android app persists data in native SQLite. No dependencies are needed for the browser preview.

## Use

Tap the floating + to create a list, choose an emoji and colour, then open the list and Add a task. Task creation inherits its list. Open the list's (…) options to change its name, emoji, colour, or switch sharing on/off, then Save list. There is no sharing choice during creation. Shared lists move into the Shared lists group; switching sharing off keeps their tasks and moves them back to My lists.

The wooden background and compact paper rows follow the supplied Wunderlist samples. Tricolour appears only as a tiny mark under the app name. Inside a list, its colour continues through the task, options, invitation and member screens, including Android system bars. Text uses the inverse colour when contrast is sufficient, otherwise black/white. The settings gear has eight identical symmetric teeth. Roboto Mono and the supplied launcher icon are retained.

Lists, tasks, appearance, sharing state and reminder preferences survive restarting. Dates, tags, notes, subtasks, completion and stars work locally. Attachments save filenames only; reminder preferences do not schedule notifications. Shared records remain on this device: invitations and other members need the future server API. No API, Firebase, authentication or Internet permission is added.

## Source and build

This is an Android WebView UI with native SQLite, not React Native. Database schema version 1 remains compatible. Native source: android-taskvilla/src/app/adc/taskvilla. Packaged UI: android-taskvilla/assets/ui. Browser UI: ui/. Reference samples: sample/. Original designs and third-party attribution are preserved.

Run powershell -ExecutionPolicy Bypass -File .\build.ps1 from android-taskvilla with SDK platform 36, build-tools 36.0.0 and C:\jdk-27. The build is a debuggable review APK, versionCode 4. Preserve signing/taskvilla-review.keystore (alias taskvilla-review, review passwords android) for compatible updates; signing keys are excluded from source ZIPs.

Daylist's earlier app.adc.wunderlistmobileapp identity remains separate under android/. Renaming it to app.adc.taskvilla was a separate installation, with no automatic data import. Existing packages, releases and keys are preserved.

Roboto Mono's SIL OFL and copyright notices are retained in assets/ui/assets/fonts/RobotoMono-OFL.txt.
