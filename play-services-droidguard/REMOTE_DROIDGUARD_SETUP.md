# Experimental remote DroidGuard sessions (v1)

This local prototype supports a versioned HTTP session transport and a reference
server on CPython 3.8 or later (standard library only). It does **not** include an
Android DroidGuard backend, produce Play Integrity tokens, or establish a passing
integrity verdict. The Python tests exercise the supplied server with a stateful
test backend; that backend is not an attestation implementation.

## Protocol and client configuration

In microG's existing DroidGuard **Network** mode, set the server URL to
`https://your-server.example/v1#droidguard-session-v1`. The exact fragment opts
into this experimental protocol. URLs without that fragment retain the existing
single-request protocol. There is no negotiation or automatic fallback. The
session endpoint must have no query string. The fragment is removed before HTTP
requests. The endpoint is captured at initialization for the handle's lifetime.

All operations use POST with
`Content-Type: application/x-www-form-urlencoded; charset=UTF-8` and require HTTP
200. Redirects are rejected. Responses are limited to 1 MiB. Connection timeout
is 15 seconds; read timeout uses the request's timeout (default 60 seconds,
clamped to 1–120 seconds).

| Endpoint | Form input | Response |
| --- | --- | --- |
| `/v1/init` | `source` package, optional `flow`, `request-<key>` metadata | Opaque session ID, 1–128 ASCII letters/digits/underscore/hyphen |
| `/v1/snapshot` | `session`, `data-<key>` snapshot strings | URL-safe Base64 snapshot bytes, no padding |
| `/v1/close` | `session` | Empty body |

String, integer, long and boolean request bundle values are encoded as text.
A backend must restore the types of known fields (e.g. `clientVersion`,
`timeoutMs`, `openHandles`). Binder objects, file descriptors and network objects
are rejected in session mode rather than silently dropped. Init returns no
Parcelable/file descriptor; flows requiring such an init reply are unsupported.
Snapshot keys and values must be strings. Data keys are prefixed so a caller's
`session` key cannot overwrite the transport's session identifier.

Repeated snapshots use one backend handle. Reinitialization closes the previous
session first; close is locally idempotent and discards local state even if HTTP
cleanup fails. Init, snapshot and close on a client handle are serialized. A
snapshot error never creates a replacement session or switches to legacy mode.
The reference server returns 410 for expired/unknown sessions, 503 at capacity,
400 for malformed forms, and 500 for backend failures. A failed backend snapshot
invalidates and closes its handle. Idle sessions expire after 120 seconds when
the next valid request is processed; server shutdown closes remaining handles.
The server processes operations serially and holds at most 32 sessions.

## Verify the reference implementation

From the repository root:

```sh
python3 -m unittest discover -s play-services-droidguard/server -v
python3 play-services-droidguard/server/integration_session_server.py -v
./gradlew :play-services-droidguard-core:testDebugUnitTest
```

The first command exercises the real HTTP handler with in-memory socket streams
and verifies multi-step state, session isolation, form encoding, binary results,
cleanup, expiration, capacity, and failure behavior. The second runs the same
cases against a real loopback HTTP server and requires TCP socket permissions.
The Gradle command exercises the client's session lifecycle without Android
dependencies in the session state class. None checks Play Integrity verdicts or
real Android Binder execution.

## Backend integration and runtime

Supply a Python module exporting `create_handle(flow, source, request)`. It must
initialize exactly one real DroidGuard handle and return an object with
`snapshot(data) -> bytes` and `close()`. It owns cleanup if creation fails.
Snapshots must act on that same live handle, never create a new VM per call.
The supplied server can then be run with:

```sh
python3 play-services-droidguard/server/session_server.py --backend your_android_backend --port 8080
```

This command requires a separately implemented and verified Android backend;
`your_android_backend` is a placeholder module name, not a supplied package.
The server binds only to loopback. For a deployment, expose it through an HTTPS
reverse proxy with access control and request limits. The client has no new
credential configuration; deployment authentication must work with the existing
URL configuration. Do not expose the unauthenticated reference port publicly.
The single-threaded reference runtime is intended for local integration, not a
commercial service; backend timeouts and Android Binder permissions are the
backend's responsibility.

There is currently no verified stock-phone setup in this prototype. Installing
Python in Termux does not grant access to another application's Binder handles
or spoof the original caller's package. Replacing GMS with microG is not proof
that a stock phone remains compliant. Before documenting a phone as an
attestation server, a real backend must demonstrate authorized Binder access,
source-package attribution, repeated snapshots on one VM, supported init reply
semantics, and a complete Play Integrity request on two devices. Root/bypass
management and automatic updates are outside this transport prototype.

The review discussion in [PR #3575](https://github.com/microg/GmsCore/pull/3575)
is context for these limitations, not verification of its proposed server.
