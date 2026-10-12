# taskvilla — Android local review build 0.6.0

Author: **Aniruddha DuttaChowdhury** · **ADChowdhury**
Application identifier and Java namespace: **app.adc.taskvilla**

## Install and preview

Open taskvilla-v0.6.0.apk on Android 8 or newer. Installing over taskvilla 0.2/0.3 with this same signing key retains local data. Fresh installs start with zero lists and tasks. Earlier releases and signing keys are preserved.

Browser preview: open C:\workspace\wunderlist-mobileapp\ui\index.html. It starts empty; browser-only data lasts for the current page session. The Android app persists data in native SQLite. No dependencies are needed for the browser preview.

## Use

Tap the floating + to create a list, choose an emoji and colour, then open the list and Add a task. Task creation inherits its list. Open the list's (…) options to change its name, emoji, colour, or switch sharing on/off, then Save list. There is no sharing choice during creation. Shared lists move into the Shared lists group; switching sharing off keeps their tasks and moves them back to My lists.

The wooden background and compact paper rows follow the supplied Wunderlist samples. Tricolour appears only as a tiny mark under the app name. List colours apply to header banners with their list icon. Only header text and controls use an inverse colour (black/white fallback for contrast); the body stays wood with neutral paper panels. Android status bars follow banners, and navigation bars stay wood. The settings gear has eight identical symmetric teeth. Roboto Mono and the supplied launcher icon are retained.

Lists, tasks, appearance, sharing state and reminder preferences survive restarting. Dates, tags, notes, subtasks, completion and stars work locally. Attachments save filenames only; reminder preferences do not schedule notifications. Shared records remain on this device: invitations and other members need the future server API. No API, Firebase, authentication or Internet permission is added.

## Source and build

This is an Android WebView UI with native SQLite, not React Native. Database schema version 2 adds pending invitee emails through a preserving ALTER TABLE migration from version 1. Native source: android-taskvilla/src/app/adc/taskvilla. Packaged UI: android-taskvilla/assets/ui. Browser UI: ui/. Reference samples: sample/. Original designs and third-party attribution are preserved.

Run powershell -ExecutionPolicy Bypass -File .\build.ps1 from android-taskvilla with SDK platform 36, build-tools 36.0.0 and C:\jdk-27. The build is a debuggable review APK, versionCode 6. Preserve signing/taskvilla-review.keystore (alias taskvilla-review, review passwords android) for compatible updates; signing keys are excluded from source ZIPs.

Daylist's earlier app.adc.wunderlistmobileapp identity remains separate under android/. Renaming it to app.adc.taskvilla was a separate installation, with no automatic data import. Existing packages, releases and keys are preserved.

Roboto Mono's SIL OFL and copyright notices are retained in assets/ui/assets/fonts/RobotoMono-OFL.txt.

## v0.6.0 controls and email invitations

Activity returns to Settings through both its Back button and Android Back. The list menu uses vertical dots. Floating Add list and Add task use the same prominent rounded shape, plus sign and readable label. Edit forms use the same Save label, dimensions and brown/cream style. Standard empty states cover empty lists, all-completed lists and activity.

Only an owner (a locally created list rather than a joined list) can add people under List options. Enter one email address at a time, tap + or Enter, and Save. Invalid addresses and case-insensitive duplicates are rejected. Save also handles a valid address still in the input. Pending emails are stored in SQLite and may be removed before saving. They are not members and are not emailed in this offline build. Real invitation delivery, acceptance and server-side permissions await API integration. Sharing can still be switched off without losing tasks.

Run open-emulator.ps1 to start Pixel_8_API_36 headlessly in the background. The latest user preference replaces the previous visible emulator requirement. Do not start a second copy while that AVD is running.

## v0.6.0 finish

Paper boxes, rows, inputs and palettes have 3-pixel corners. Buttons use raised bevels with a light upper edge, shaded lower edge and inset highlights; the pressed state appears depressed. Floating Add list/Add task retain matching rounded shapes. All v0.5 navigation, banner, owner-email and consistent Save changes are included. Updates keep app.adc.taskvilla and the same signing key. No sample records ship with the APK.
