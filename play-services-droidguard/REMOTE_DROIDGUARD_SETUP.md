# Remote DroidGuard Setup for Play Integrity Multi-Step Flows

This document covers only the client-side configuration that microG already provides and a minimal
self-hosted server pattern for testing remote Play Integrity in issue [#2851](https://github.com/microg/GmsCore/issues/2851).

## 1) What changed for Play Integrity compatibility

Play Integrity can use a multi-step DroidGuard flow. Before this fix, microG accepted only single
snapshot calls over remote DroidGuard. The implementation now supports:

- `begin(...)` to start a multi-step session
- `nextStep(...)` to submit one intermediate step
- `snapshotWithSession(...)` to submit the final payload
- `closeSession(...)` to clean up server-side state if needed

The remote request packet now carries the multi-step metadata in `DroidGuardResultsRequest`:

- `sessionId`
- `stepNumber` (0-based)
- `totalSteps` (if known by caller)
- `isMultiStep = true`

## 2) Client setup in microG

1. Open microG Settings → DroidGuard → Remote
2. Set the Remote URL to your reachable endpoint
3. Save and keep microG in Remote mode for the target app profile

The remote client sends:

- query params:
  - `flow`
  - `source` (package name)
  - `x-request-*` values from `DroidGuardResultsRequest`
- `POST` body with the step payload as URL-encoded key/value pairs

## 3) Minimal server endpoint expectation

MicroG remote mode expects a DroidGuard-compatible endpoint that receives:

- `POST` at the configured URL
- query parameters above
- `application/x-www-form-urlencoded` body
- returns raw DroidGuard token bytes (`byte[]`)

The request sequence is:

1. `begin(...)` initializes a server-side session context
2. each `nextStep(...)` appends state
3. `snapshotWithSession(...)` finalizes and returns response bytes

If you are hosting your own server, return a non-empty byte array and ensure CORS/network
and TLS are configured for your environment.

## 4) Suggested minimum validation flow

1. Start a fresh Play Integrity flow from a test app
2. Confirm the first request includes `x-request-is-multi-step=true`
3. Confirm subsequent `nextStep` calls arrive without `sessionId` mismatch
4. Confirm the final `snapshotWithSession` returns non-empty bytes

Keep payload logs scrubbed of PII before sharing them in bug reports.
