# Shared-list recipient flow validation

Validated in Edge, 9 October 2026.

- PASS: blank and invalid codes show inline errors.
- PASS: valid code opens invitation metadata before access.
- PASS: requesting access shows pending state and a pending row, without exposing list tasks.
- PASS: cancel request and decline invitation do not add a list.
- PASS: simulated owner approval opens confirmation and adds the list exactly once.
- PASS: recipient can open the approved list and add a task within it.
- PASS: already-joined code opens its existing list; repeated acceptance does not duplicate it.
- PASS: expired invitation and retry state.
- PASS: 5 new 780 × 1688 screenshots and 390-pixel responsive layout.
- PASS: no JavaScript exceptions or failed resources.

Approval is triggered by a review-only control outside the phone. Codes, invitations and requests are local sample states; no service, real invitation delivery or membership enforcement is implemented.
