# Reentrant session completion

`SessionImpl.notifySessionEnded` announces `onSessionEnding` before completing the session. A listener can synchronously call `notifySessionEnded` again. The inner call then completes the session, but the outer call previously announced a second completion and queued cleanup again.

The two-line guard checks whether that callback already completed the session. Ordinary termination and the existing proxy-error fallback are unchanged.

## Focused JVM reproduction

Executed October 4, 2026 on OpenJDK 17.0.20, compiling the complete production `SessionImpl` with Java 8 language/API compatibility. Minimal Android/Binder types and recording session-manager/router/proxy collaborators supplied the boundaries; the test did not copy the state machine or change private state.

The connected-session case set an ending listener to `session.notifySessionEnded(71)` and then invoked `session.notifySessionEnded(72)`:

| Observation | Before | After |
| --- | --- | --- |
| Completion error codes | 71, 72 | 71 |
| Cleanup dispatches | 2 | 1 |
| Final session state | Disconnected | Disconnected |

The same five cases produced **4 passing / 1 failing before**, **5 passing / 0 failing after**. The four controls covered ordinary completion, repeated terminal calls, `end(true)` followed by completion, and a proxy `RemoteException` during `end(false)` (original exception rethrown, `INTERNAL_ERROR` delivered).

Original source blob: `5de4cf7828b53593b8a98b937309d95acbec5a82` at PR head `b282f0d516b6fd093bd754d0dedac3de2bad2723`.
Candidate source blob: `1463df7a706b8b734006f39f0c6741b91fd91f42`.
Only the two added Java lines change production behavior; CRLF is preserved.

This is synchronous class-level lifecycle evidence. Android/Binder integration, a physical receiver, cross-thread safety, and same-object lifecycle reuse were not exercised.

## Preparation callbacks cannot announce an already failed session

The client proxy's `onStarting` and `onResuming` callbacks can synchronously
report that the session failed. Previously, `SessionImpl` still sent the matching
starting/resuming notification to the session manager after that failure. The
later state check prevented the proxy operation, but listeners had already
received an impossible failure-then-start sequence.

Two immediate state checks now stop that outer notification when preparation
has already moved the session out of its expected starting/resuming state.
Existing checks after the manager callbacks, proxy exception handling and the
earlier terminal-notification repair remain intact.

### Focused execution — October 4, 2026

Both complete production classes, `SessionImpl` and `SessionManagerImpl`, were
compiled with OpenJDK 17.0.20 / Java 8 compatibility and executed using retained
Robolectric 4.14.1, API 28 and JUnit 4.13.2 dependencies. Android `Bundle` and the
actual manager's callback paths were exercised. Client proxy, router and listener
interfaces used recording Java proxies; the compiled context and other unchanged
dependencies were reused.

| Scenario | Before | After |
| --- | --- | --- |
| Failure inside `onStarting` | StartFailed, Starting | StartFailed only |
| Failure inside `onResuming` | ResumeFailed, Resuming | ResumeFailed only |
| Ordinary start | Starting; proxy start once | Same |
| Ordinary resume | Resuming; proxy resume once | Same |

The same four local cases produced 2 passing / 2 failing before and 4 passing /
0 failing after. Candidate checks also confirmed no proxy start/resume after
the synchronous failure and the expected terminal/active session states.

Source parent: `0b54bf4a4b04f24cac9262f6a875e91545155b59`.
Previous SessionImpl blob: `1463df7a706b8b734006f39f0c6741b91fd91f42`.
Corrected SessionImpl blob: `9e16ba1c388ef6d23295ed84d661b4829bca16a5`.
Unchanged SessionManagerImpl blob: `023f33578868c0e91ef92552baceb7950a5a2705`.

This run did not exercise Binder transport, a physical receiver, APK assembly,
the full Gradle suite, cross-thread behavior or same-object lifecycle reuse.
No framework test suite or dependency was introduced. The existing physical
receiver proof-video acceptance remains separate.
