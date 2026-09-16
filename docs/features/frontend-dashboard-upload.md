Feature name: frontend-dashboard-upload

## Summary

Adds the SecureFiles console dashboard for uploading files, following antivirus status changes, and reviewing recent files. The browser communicates exclusively through a typed Axios client and makes scan states and blocking failures visible.

## Status

Stable

## Public API / Contracts

- [filesApi](../../frontend/src/api/filesApi.ts) centralizes multipart upload, upload-size configuration, metadata polling, file listing, progress reporting, request cancellation, and normalized API errors.
- [FileUpload](../../frontend/src/shared/forms/FileUpload/FileUpload.tsx) loads the server upload limit before enabling submission, blocks oversized files locally, reports transfer progress, and polls accepted files until a terminal scan state.
- [DashboardPage](../../frontend/src/pages/Dashboard/DashboardPage.tsx) displays the file register, supports retry after a listing error, and shows stable failure-code descriptions through accessible status tags.
- [App](../../frontend/src/App.tsx) makes the dashboard the default route and exposes the components and file-register routes.

## Quick usage

Run the backend and then `npm run dev` from `frontend/`. Select a file on the dashboard after the maximum permitted size has loaded; the console shows upload progress and continues polling the accepted file until its scan reaches a terminal status.

## Design decisions

- Upload-size validation is a usability guard based on the backend configuration; the server remains the enforcement point.
- The console offers no download action for a scan status, so it cannot create a UI path around the backend `CLEAN` requirement.
- Scan failure codes are mapped to short, safe descriptions rather than exposing raw server errors or object-storage details.
- Polling retries only transient metadata failures and stops for terminal statuses, preserving blocked outcomes as visible states.
- Shared status tags provide tooltip support for icon-only failure context, while text-labelled actions remain free of redundant tooltips.

## Tests & validation

- [API tests](../../frontend/src/tests/api/filesApi.test.ts) cover multipart content, progress, configuration, metadata, listing, cancellation, and normalized errors.
- [FileUpload tests](../../frontend/src/tests/shared/forms/FileUpload/FileUpload.test.tsx) cover server limits, local rejection, retry, progress, status updates, and upload errors.
- [Dashboard tests](../../frontend/src/tests/pages/Dashboard/DashboardPage.test.tsx) cover listing, scan polling, failure detail, retry, and error feedback.
- `npm test` completed successfully on 2026-09-16: 17 test files and 49 tests passed.
- `npm run build` completed successfully on 2026-09-16.

## Related files

- [fileFailureCodes](../../frontend/src/shared/constants/fileFailureCodes.ts)
- [Tag](../../frontend/src/shared/feedback/Tag.tsx)
- [GenericTable](../../frontend/src/shared/data/GenericTable.tsx)
- [styles.css](../../frontend/src/styles.css)

## Changelog

- 2026-09-16 - Added dashboard upload, scan tracking, server size preflight, and file register UI.