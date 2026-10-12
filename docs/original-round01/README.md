# Daylist — Wunderlist-style mobile UI concept

This is **Round 01: UI screens only**. There is no cloud database, server, actual sign-in, or real collaboration. The working app name "Daylist" is a placeholder.

## Open the preview

Open `ui/index.html` in Chrome, Edge, or Firefox. Select any screen from the left sidebar or click inside the phone preview. Tasks can be checked, a demo task can be added, and screens can be navigated. All changes are local to this temporary browser session.

## Contents

- `ui/index.html` — review gallery and clickable mobile preview
- `ui/styles.css` — design system and device layouts
- `ui/app.js` — screen mockups and demo-only interactions
- `ui/screens/` — 14 rendered PNG screenshots at 780 × 1688 (2× 390 × 844)
- `ui/assets/` — original SVG logo, illustrations, and illustrated avatars
- `ui/design-notes.md` — screen inventory, decisions, and review points

## Suggested installation folder (Windows)

Extract this archive so the root becomes:

`C:\workspace\wunderlist-mobileapp\ui\index.html`

Do **not** start implementing a backend based on this preview yet. Review navigation, task/list density, sharing permissions, and visual style first. Once screens are approved, the structure can be translated into React Native components.

## Important limits

The sharing code, accounts, members, dates, and notifications are fictional mock data. Navigation and form input are prototype-only; there is no network connection. Offline persistence is a planned backend/product feature, not implemented by this prototype.
