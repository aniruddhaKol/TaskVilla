# Daylist UI — Design Review 01

**Design intent:** A calm, reliable to-do app for families and friends, inspired by the usability of Wunderlist rather than its exact appearance. Working title: **Daylist** (renameable).

**Artboard:** 390 × 844 logical pixels, Android-first. Export screenshots: 780 × 1688 PNG, 2× scale.

**Tokens:** Primary #426BE8; success #39B88B; attention #FF846E; canvas #F7F8FC; ink #202C43.

## Screen inventory

| # | Screen | Intent |
|---|---|---|
| 01 | Welcome | Friendly value proposition and first-run onboarding |
| 02 | Sign in | Email / Google affordances; mock only |
| 03 | Today overview | Today's focus, progress and shared spaces |
| 04 | My lists | Personal and shared list browsing |
| 05 | Shared list | Progress, filters, completion, member access |
| 06 | Task details | Due date, assignee, priority, subtasks, notes, comments |
| 07 | Create task | Minimal capture + list, due date, assignee, priority |
| 08 | Share a list | Invitation code plus explicit owner approval |
| 09 | Members & permissions | Owner, editor, viewer, pending requests |
| 10 | Activity feed | Collaboration activity and updates |
| 11 | Upcoming | Calendar strip and due-date agenda |
| 12 | Settings | Preferences, privacy and export |
| 13 | Search | Cross-list search patterns |
| 14 | Empty state | Friendly first-task onboarding |

## Decision points for review

1. Brand name and palette: keep Daylist / choose another name?
2. Top-level tabs: Today, Lists, Upcoming, Activity — keep these four?
3. Task density: full-width comfortable rows or a more compact list?
4. Sharing policy: approval-required versus instant-invite links?
5. Reminder screens: do we need a separate reminder configuration screen?
6. Desktop/web equivalent: needed now or after Android screens are approved?

## Known prototype gaps

- No working backend, security enforcement, or live sharing.
- The date and accounts are static illustrative data.
- Buttons for permissions/configuration that would require a server remain explanatory prototypes.
- Native mobile navigation, accessibility auditing, and dark theme are implementation-phase work.

The `ui/assets` vector files are original placeholders. No remote fonts or external image URLs are needed.
