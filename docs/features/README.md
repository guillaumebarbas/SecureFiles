# Feature Delivery Order

This directory records the delivery history of SecureFiles. Every feature
summary uses the two-digit filename format `<NN>-<short-name>.md` and appears
once in this reading order. `README.md` is the only unnumbered file because it
is the index, not a feature summary.

Feature numbers are append-only: keep published numbers unchanged and assign
the next number after the current highest entry when documenting a new
feature. Do not reuse a number, create an unnumbered summary, or keep a second
summary for the same feature.

Merge the feature branches in this order:

0. `00-securefiles-console-foundation.md` establishes the initial backend bootstrap, frontend console shell, and shared UI primitives.
1. `01-backend-secure-file-flow.md` from `feat/backend-secure-file-flow` establishes the secure backend contract, persistence, storage, and antivirus workflow.
2. `02-frontend-dashboard-upload.md` from `feat/frontend-dashboard-upload` adds the dashboard that consumes the backend contract.
3. `03-securefiles-delivery-foundation.md` from `chore/securefiles-delivery-foundation` documents the final workflow, rules, and runtime guide.
4. `04-frontend-ux-ui-fixes.md` from `feat/frontend-ux-ui-fixes` records the console shell, backend status, upload feedback, and navigation refinements.
5. `05-large-file-antivirus-scan.md` from `fix/large-file-antivirus-scan` aligns the production size policy and adds the isolated ClamAV stream-limit integration scenario.
6. `06-frontend-profile-login-ux.md` records the initial Profile and Login console interactions.
7. `07-secure-file-access-authentication.md` from `feat/secure-file-access-authentication` adds persisted JWT sessions, authentication-aware uploads, and public recent-file metadata.
8. `08-file-list-server-pagination.md` from `feat/file-list-server-pagination` replaces frontend-only recent-file pagination with backend pagination and controlled server navigation.
9. `09-scan-retry-reliability.md` from `fix/scan-retry-reliability` hardens scan retries, lease recovery, broker confirmation, and terminal failure causes.
10. `10-frontend-developer-library-access.md` from `feat/frontend-developer-library-access` limits the Bibliothèque navigation item to profiles containing the `developpeur` role.
11. `11-native-diagram-skill-workflow.md` from `feature/skills-diagrams` adds the native draw.io skill workflow, simplified pairs, validation, and PNG previews.

The summaries remain with the branch that delivers the corresponding
implementation. After merging the branches in sequence, this directory gives
the complete reading order.