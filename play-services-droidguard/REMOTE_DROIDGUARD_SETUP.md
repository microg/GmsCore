# Remote DroidGuard Setup for Play Integrity

This covers the minimal client-side configuration to use microG's remote
DroidGuard path (which this PR enables for request-backed flows).

## 1) When this matters

The `guardWithRequest` path is called when Play Integrity (or other
attestation services) sends a `DroidGuardResultsRequest`. Without this PR,
any app hitting this path gets `NotImplementedError`.

With this PR, the path is wired through the existing handle lifecycle:
`initWithRequest → snapshot → onResult → close`.

## 2) Client setup

1. Open microG Settings → DroidGuard → Remote
2. Set the Remote URL to your DroidGuard server endpoint
3. Save; microG will use remote mode for attestation

## 3) Required server endpoint

The remote endpoint receives:

- `GET`/`POST` at the configured URL
- Query params: `flow`, `source`, plus `x-request-*` from the request bundle
- POST body: URL-encoded key/value payload

It must return a Base64-encoded byte array (URL-safe, no padding).

## 4) Quick validation

1. Set a 30-second test timer
2. Trigger a Play Integrity check from a test app (e.g., Dott sign-in)
3. Confirm the remote server receives the request
4. Confirm the attestation flow completes without `NotImplementedError`

No AIDL changes. No new session APIs. Uses existing infrastructure.
