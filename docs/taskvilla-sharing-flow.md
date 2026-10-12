# TaskVilla: confirmed future sharing flow

Author: Aniruddha DuttaChowdhury · ADChowdhury

These are requirements for the later server API phase. They are not implemented authentication or live sharing in the offline review APK.

## Identity and access

Use email OTP only. Login and registration are one flow; no passwords. Require authentication before showing shared data or initiating a share. Keep private local lists usable offline. API endpoints must enforce ownership and membership; local UI flags do not grant access.

## Harry shares with Tom

Harry owns a list and invites Tom by email. Tom sees the pending invitation under Shared with me after signing in. Offer Accept or Ignore. Until acceptance, the list must not appear among Tom's home lists and Tom has no list-content access. Acceptance grants membership and displays it as an accepted incoming list. Ignoring does not grant membership.

Tom may decline/leave an accepted share at any time. Harry may remove Tom at any time, with confirmation. Removing/declining revokes access and removes the list from Tom's accepted home lists after synchronization. Harry retains his own list. Owners' outgoing shares remain under My Lists and show a standard group-of-people marker. No separate Share this list checkbox is required.

## Refresh

Pull-to-refresh will synchronize authenticated invitations, acceptance/removal status, list content and task changes with the future API. The current review app only reloads SQLite and explicitly reports that server sync is not connected. Do not pretend local invitee addresses are authenticated members or delivered invitations.

## Deferred verification

Do not run end-to-end sharing tests until the user explicitly asks at the end. Then test OTP sign-in/registration, Harry's invitation, Tom's pending visibility, accept/ignore, no home entry before acceptance, Tom declining later, Harry removing Tom, authorization revocation and refresh across both accounts. These scenarios have been recorded, not executed in v0.8.
