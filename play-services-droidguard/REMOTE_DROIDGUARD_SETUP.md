# Operator-controlled remote DroidGuard

This bridge lets an explicitly authorized ADB operator run the existing remote
DroidGuard protocol against microG's native Embedded runtime on a second Android
device. The Android provider keeps one real native handle per session; the Python
adapter forwards each `begin`, `snapshot`, and `close` to that provider.

This requires a build containing `DroidGuardSessionProvider`. The provider is
**disabled by default**. The older Python-only setup cannot supply the missing
native provider, and ordinary Termux execution does not have the Android
permissions needed by the `content` command.

## Start the bridge

You need two devices: the server uses Embedded DroidGuard and the client uses
Network DroidGuard. On the operator's computer, install Python 3.8 or newer and
Android SDK Platform Tools. Authorize the intended devices for ADB and obtain
their serials with `adb devices`.

On the server device, enable DroidGuard in **Embedded** mode. A packaged setting
that disables local DroidGuard must remain respected. Then explicitly enable the
bridge for the intended Android user; these examples use user 0:

```sh
adb -s SERVER_SERIAL shell pm enable --user 0 \
  com.google.android.gms/org.microg.gms.droidguard.core.DroidGuardSessionProvider
```

Create a private authentication-token file on the operator's computer:

```sh
umask 077
python3 -c 'import secrets; print(secrets.token_urlsafe(32))' > droidguard-token.txt
python3 play-services-droidguard/server/droidguard_server.py \
  --serial SERVER_SERIAL --user 0 --token-file droidguard-token.txt
```

The adapter requires an explicit device serial and a random token of at least 24
characters. Instead of `--token-file`, it can read the `DROIDGUARD_TOKEN`
environment variable, or another variable selected by `--token-env NAME`.
It checks that the selected ADB device is connected before listening. It neither
enables the provider nor changes DroidGuard settings automatically.

The listener defaults to `127.0.0.1:8080`. Connect the client device through ADB
reverse forwarding:

```sh
adb -s CLIENT_SERIAL reverse tcp:8080 tcp:8080
```

On that client device, select **Network** DroidGuard mode and configure its remote
server URL as:

```text
http://127.0.0.1:8080/droidguard/?token=TOKEN_FROM_THE_PRIVATE_FILE
```

The generated token uses URL-safe characters. For a differently formatted token,
percent-encode its query value. The current remote client preserves the base
URL's query on every lifecycle request. Keep the URL private; it carries the
credential. The adapter does not log URLs, request maps, or native results.

`--host` and `--port` can change the listener. HTTP supplies no transport
encryption, so use an authenticated private tunnel when connecting across a
network. The loopback-plus-ADB setup above does not require a LAN listener.

## Stop and disable

Stop the Python process with Ctrl-C, remove the client forwarding, and disable the
provider when finished:

```sh
adb -s CLIENT_SERIAL reverse --remove tcp:8080
adb -s SERVER_SERIAL shell pm disable --user 0 \
  com.google.android.gms/org.microg.gms.droidguard.core.DroidGuardSessionProvider
```

Explicit close makes a session ID unusable immediately and requests native
cleanup. Cleanup can finish after the response. Sessions abandoned by a client or
adapter become unusable after 120 seconds of inactivity; a periodic cleanup
pass runs at most 30 seconds later. The provider retains capacity until native
cleanup finishes.

## Protocol and limits

The HTTP endpoint is `/droidguard/`, using POST requests:

| Action | Query and body | Successful response |
| --- | --- | --- |
| `begin` | Query: `token`, `action`, `flow`, `source`, optional `x-request-*`; no body | `sessionId=...&status=ok` |
| `snapshot` | Query: `token`, `action`, `sessionId`; form body is this step's string map | Native DroidGuard Base64, unchanged |
| `close` | Query: `token`, `action`, `sessionId`; no body | `status=ok` |

Initialization request metadata and snapshot data remain separate. The provider
reconstructs the supported scalar initialization fields with their expected
types; Android parcelables such as `fd` and `networkToUse` cannot be transported.
No package certificate override is introduced.

The adapter invokes `adb -s SERIAL shell content call` against
`content://org.microg.gms.droidguard`. Its method matches the HTTP action and
`--arg` contains URL-safe Base64 of UTF-8 JSON:

```json
{"flow":"FLOW","source":"PACKAGE","request":{"clientVersion":"VERSION"}}
{"sessionId":"SESSION_ID","data":{"STEP_KEY":"STEP_VALUE"}}
{"sessionId":"SESSION_ID"}
```

The provider returns a Bundle with one `response` field containing URL-safe
Base64 JSON. Successful responses have `status: "ok"` and either `sessionId`,
`result`, or neither for close. Errors have `status: "error"`, an HTTP-compatible
`code`, and an error message. This outer envelope is decoded once; the native
DroidGuard Base64 inside `result` is returned verbatim. Content-command stdout
and diagnostics are never treated as attestation bytes.

The adapter caps URL length at 8 KiB, request bodies at 32 KiB, snapshot maps at
256 entries, and concurrent HTTP handlers at 10. Session IDs are limited to
64 characters. Provider arguments are capped at 65,536 encoded characters
and 48 KiB decoded JSON; native Base64 results are capped at 256 KiB. The provider
allows at most eight sessions, including pending initialization and cleanup, and
uses bounded execution queues. A second concurrent operation on the same
session receives HTTP 503 before occupying a worker; the existing session stays
open, and unrelated sessions can use the other worker. Native begin/snapshot
waits are capped at 45
seconds; each ADB command has a 55-second deadline. Native work cannot always be
preempted: a hung operation keeps its capacity occupied until it exits, rather
than permitting an unbounded number of replacement handles.

A nonzero ADB exit, stderr, malformed response, unavailable provider, or explicit
provider error status produces an HTTP error. Operations are not retried automatically because
a lost response does not prove that initialization or a snapshot was not run.
Valid Base64 alone does not establish successful attestation; the native runtime
can also return error bytes.

## Authorization and acceptance

The provider checks `android.permission.DUMP` and allows only the same application
UID, Android shell, or root before handling requests. Its native execution is an
explicit operator capability. The ordinary DroidGuard service and its calling-
package checks remain in place. Enabling the provider does not make it available
to ordinary Termux or arbitrary Android application callers.

This integration does not establish Play Integrity verdicts, Dott login, scooter
unlock, or a completed ride. Those require the real server/client devices and
app flow; the Dott bounty's requested unlock/ride evidence remains separate.

## Executed source validation (2026-10-04)

The complete provider, session store, and core `DroidGuardHandleImpl` were compiled
with Kotlin 2.2.21 for JVM 1.8 and run with retained OpenJDK 17, JUnit 4.13.2,
Robolectric 4.12.2, and Android API 29 dependencies. The six focused scenarios
passed:

| Scenario | Observed behavior |
| --- | --- |
| Failed native setup | Native `init()` returning false and `rb()` throwing each returned 502 without an ID and closed the failed proxy exactly once. |
| Owner and idle lifetime | Foreign owner received 404; the owner could snapshot; advancing the injected idle clock expired the ID and closed it once. |
| Caller and input admission | Missing DUMP permission and an ordinary UID were denied. Disabled local mode was checked before decoding malformed input. A nonobject request was rejected before factory work. |
| Independent sessions | With A held inside a snapshot, another A operation received 503; B initialized and snapshotted before A was released. A remained usable, and both handles closed once. |
| Retained native lifecycle | One core handle and actual `HandleProxy` performed two snapshots before one close. Source package, callback package, request metadata, and integer Bundle fields were preserved. |
| Initialization deadline | An injected 30 ms deadline returned 504 without an ID. Capacity stayed occupied until the late factory result was closed, preventing replacement-handle growth. |

These executions used the actual provider, store, core handle, `HandleProxy`,
`GuardCallback`, and Android Binder, permission, Bundle, Base64, and JSON behavior.
The network factory returned a recording VM and local preferences were controlled.
This exercises the native-handle integration boundary; it does not execute Google's
VM or JNI, contact its service, or establish an attestation verdict.

The provider was manually attached in Robolectric using a retained resource APK.
The manifest below was pinned as source; installed component enablement, export,
and manifest permission resolution were not tested. This was not a Gradle build
or a new APK, and it does not supersede earlier PR-specific APK source pins.

The production Python HTTP handler also ran with real subprocess execution and
an explicit disposable content-command fixture. Ten HTTP requests caused seven
content-command invocations: begin, two different snapshots, and close succeeded;
an invalid token, duplicate field, and oversized body were rejected before any
command; a provider 404 propagated; malformed stdout and zero-exit stderr became
502 errors. Initialization fields and result bytes were preserved, credentials
were absent from adapter logs, and no command was retried. The subprocess fixture
is a transport boundary, not evidence of a connected Android device.

Executed production source:

| Path | Git blob |
| --- | --- |
| `core/src/main/kotlin/org/microg/gms/droidguard/core/DroidGuardHandleImpl.kt` | `8809a8344c24769006065207647453a03e8710da` |
| `core/src/main/kotlin/org/microg/gms/droidguard/core/DroidGuardSessionProvider.kt` | `bbf96e0edc57a92d00345abf1fdd3b916c37d86a` |
| `core/src/main/kotlin/org/microg/gms/droidguard/core/DroidGuardSessionStore.kt` | `79439b79c05d7978c48ec81ca6e77183f4fbe344` |
| `core/src/main/AndroidManifest.xml` (source only) | `54e5626f46c25fd1c6cf2063be80b108959a673c` |
| `server/droidguard_server.py` | `962a63b960a804b913c6f820d5eae312d0c0324f` |

## Source credit

The HTTP action/form protocol and original Python server structure were adapted
from GautamKumarOffical's [microg/GmsCore PR #3575](https://github.com/microg/GmsCore/pull/3575),
source commit `9d9a8d37e1f4b70997ba1868d2c196261dfd7d0f`. The Python file retains
its Apache-2.0 SPDX notice. This version replaces that proposal's metadata-only
sessions and nonexistent one-shot provider invocation with the native retained-
handle provider implemented in this contribution.
