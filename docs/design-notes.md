# Mobile UI design — Round 02

Author: Aniruddha DuttaChowdhury · ADChowdhury  
Application identifier for native conversion: `app.adc.wunderlistmobileapp`

## Reference interpretation

The supplied mobile reference informs My Lists navigation, compact list rows, starred tasks, full-page task details, bundled shared lists and new-list creation. The desktop reference informs task fields only. Reference text is treated as design material; it does not authorize services, uploads or external integrations.

## Flow

My Lists → existing list → Add a task → Add task → originating list. Task creation has no Add to List section. Inbox is an existing built-in list. Planned and search navigate into a task in its actual list context. The review dropdown can inspect creation screens with a representative existing list selected.

Private lists and shared lists are distinct. New-list creation lets the user choose a name, icon, tricolour accent and sharing. Tasks added to a newly created list stay there. The prototype preserves the reference's private-list convention; it does not add a convert-to-shared action.

## Theme and density

Saffron #FF9933, white #FFFFFF, green #138808 and navy #000080. Every page shares a saffron top, white central surface and pale green lower surface. Dark saffron #A94B00 gives readable button labels. Green identifies completion and shared-list accents. Navy is reserved for small brand/colour details. List rows are compact; fields are separated by thin rules. Save/Add stays at the bottom, with scrolling content above it.

## Screen inventory

01 Welcome; 02 Sign in; 03 My lists; 04 Personal list; 05 Shared list; 06 Task details; 07 Create task; 08 Invite people; 09 Members; 10 Activity; 11 Planned; 12 Settings; 13 Search; 14 Inbox; 15 Shared lists; 16 Create new list.

## Limits

All dates, members and tasks are samples. Data resets on reload. Attachments are filename-only local previews. Reminder selections do not schedule device notifications. Authentication and sharing are illustrative. No backend, network services or React Native implementation was added.

Original source and the previous tricolour revision are preserved in docs/original-round01 and docs/tricolour-round01 respectively.

## Recipient sharing flow

Shared lists provides both code entry and a received-invitation row. Invitation review shows only the list name, owner, membership count and invited role. Accepting sends a local demo request; tasks stay hidden until the approval transition. Pending requests are visible in My Lists and Shared Lists and can be cancelled. Approved membership adds the list once. Existing invitations reopen the joined list without duplication. Invalid codes use inline feedback; expired invitations offer another-code entry.

Screens 17–21: Join a shared list, Review invitation, Request pending, Access approved, Invitation expired. This extends the existing owner-approval convention. In review mode only, a Simulate approval button outside the mobile screen advances to approval; it is not a mobile product control. The owner-side Members screen still uses illustrative sample requests. No real identities, services or sharing are connected.

## Typography

Roboto Mono variable font (weights 100–700), bundled locally in ui/assets/fonts for offline preview. Font source: https://github.com/google/fonts/tree/main/ofl/robotomono. The original author attribution and SIL Open Font License are preserved in RobotoMono-OFL.txt.
