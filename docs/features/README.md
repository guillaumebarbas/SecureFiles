# Feature Delivery Order

This directory records the delivery history of SecureFiles. The existing
`securefiles-console-foundation.md` document is already part of `main` and
precedes the numbered feature deliveries below.

Merge the numbered feature branches in this order:

1. `01-backend-secure-file-flow.md` from `feat/backend-secure-file-flow` establishes the secure backend contract, persistence, storage, and antivirus workflow.
2. `02-frontend-dashboard-upload.md` from `feat/frontend-dashboard-upload` adds the dashboard that consumes the backend contract.
3. `03-securefiles-delivery-foundation.md` from `chore/securefiles-delivery-foundation` documents the final workflow, rules, and runtime guide.
4. `04-frontend-ux-ui-fixes.md` from `feat/frontend-ux-ui-fixes` records the console shell, backend status, upload feedback, and navigation refinements.
5. `05-large-file-antivirus-scan.md` from `fix/large-file-antivirus-scan` aligns the production size policy and adds the isolated ClamAV stream-limit integration scenario.
6. `06-frontend-profile-login-ux.md` records the initial Profile and Login console interactions.
7. `07-secure-file-access-authentication.md` from `feat/secure-file-access-authentication` adds persisted JWT sessions, authentication-aware uploads, and public recent-file metadata.

The numbered summaries intentionally remain with the branch that delivers the
corresponding implementation. After merging the branches in sequence, this
directory gives the complete reading order.