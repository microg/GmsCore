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

### How Play Integrity uses this

The Play Integrity / PoToken flow (`PoTokenHelper` in `play-services-core`) requests DroidGuard
results via `DroidGuardClient.getResultsMultiStep(...)`, which drives a full session
(`begin` → `nextStep` → `snapshotWithSession` → `closeSession`) instead of a single
`snapshot` call. This means the PoToken / Play Integrity flow now also works when
DroidGuard is executed against a remote DroidGuard server, and it transparently falls
back to a plain `snapshot` if the session API is not available (e.g. older server or
local legacy handler), so existing setups keep working unchanged.

## 2) Client setup in microG

Remote (network) mode is controlled by the DroidGuard settings exposed through
`SettingsContract.DroidGuard`:

- `MODE = Network` — use the remote DroidGuard server instead of the embedded local VM
- `NETWORK_SERVER_URL` — base URL of your reachable DroidGuard-compatible endpoint
- `ENABLED = true` — DroidGuard overall must be enabled

In the microG Settings app these are the "DroidGuard" / "Device certification"
preferences (select *Network* mode and set the server URL), so no code changes or
rebuilds are needed on the client side.

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

### Setting up a phone as the DroidGuard server

A commonly used setup is [kaduvert/PlayIntegrityServer](https://github.com/kaduvert/PlayIntegrityServer)
(DroidGuard Relay): a Zygisk module installed on a *server* device (a phone with working
Google Play / SafetyNet-attested environment via microG or real Gms) that executes the
DroidGuard VM work on behalf of remote clients and serves the resulting tokens over HTTP(S).

1. Install the relay module on the server phone, keep it reachable via Wi-Fi/VPN
2. Note the endpoint URL it exposes (e.g. `https://<server>:<port>/droidguard/`)
3. On the client device set DroidGuard mode to *Network* and paste that URL
   (see section 2)
4. The PoToken / Play Integrity flow on the client now transparently runs its multi-step
   session against the server phone - no root or TrickyStore-style bypass on the client

## 4) Suggested minimum validation flow

1. Start a fresh Play Integrity flow from a test app
2. Confirm the first request includes `x-request-is-multi-step=true`
3. Confirm subsequent `nextStep` calls arrive without `sessionId` mismatch
4. Confirm the final `snapshotWithSession` returns non-empty bytes

Keep payload logs scrubbed of PII before sharing them in bug reports.
