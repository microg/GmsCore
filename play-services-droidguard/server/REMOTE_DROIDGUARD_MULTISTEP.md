# Remote DroidGuard: multi-step sessions for Play Integrity

This directory holds a reference implementation that makes microG's
*network-mode* DroidGuard able to evaluate **request-backed, multi-step** flows
such as Play Integrity and App Check, which the current one-shot protocol
cannot serve.

## The problem

`DroidGuardServiceImpl.guardWithRequest(...)` was `TODO("Not yet implemented")`
in `core/src/main/kotlin/org/microg/gms/droidguard/core/DroidGuardServiceImpl.kt`,
so any client calling `guardWithRequest` (Play Integrity, Firebase App Check)
got nothing. Independently of that, the network handle (`RemoteHandleImpl.kt`)
treated every request as a single stateless POST — even when a single
DroidGuard "request" really evaluates the device in several internal steps
(connect → attest → persistence), with the final attestation depending on state
from earlier steps.

A network backend therefore needs to keep per-request state across those
steps; a plain stateless proxy cannot mint a PI token no matter what it does.
That is exactly why `mar-v-in` summarized the previous attempt, #3575, with:
> "the server is not functional (it includes simply invalid commands)"

(No amount of `content call` plumbing can make microG's own DroidGuard return
a Google-signed Play Integrity verdict — you need a *real* device that Google
passes, which is why mar-v-in's own recommendation is an old, passing phone as
home server.)

## What's in this PR

1. **`core/.../DroidGuardServiceImpl.kt`** — `guardWithRequest` is implemented
   via the real handle lifecycle (`initWithRequest → snapshot → close`),
   matching what the embedded implementation already does. This alone fixes
   every `guardWithRequest` consumer, including the plain `guard` path that
   delegates to it.
2. **`core/.../RemoteHandleImpl.kt`** — request-backed flows (Play Integrity /
   App Check) now speak a **session protocol** to the network server:
   - first `snapshot` opens a session (`action=begin`), which the server
     creates and hands back as a `sessionId`;
   - subsequent `snapshot` calls are tagged with that session
     (`action=snapshot&sessionId=…`), so the server can keep per-step state;
   - `close()` releases the session (`action=close`) and drops client state.
   - The legacy single-step path (ad attestation etc.) is untouched and sends
     byte-identical requests.
3. **`server/droidguard_multistep_server.py`** — stdlib-only reference server:
   - session store with TTL cleanup;
   - `begin` / `snapshot` / `close` endpoints plus legacy single-step compat;
   - two backends: `--mock` (returns a deterministic, step-counting blob so the
whole protocol can be exercised without hardware) and
   `--backend-device-url <url>` (forwards each step to another HTTP endpoint,
   e.g. a helper app on a real device);
   - optional `--token` bearer auth; sit it behind TLS yourself (`caddy`,
     `nginx`, `termux:stunnel`, or a cloud `https` termination).
4. **`server/test_droidguard_multistep_server.py`** — 12 tests, run with:

   ```sh
   python3 -m pytest test_droidguard_multistep_server.py -q   # 12 passed
   ```

## Try it (no device needed)

```sh
# terminal A: mock backend (default), listens on :8080
python3 server/droidguard_multistep_server.py --verbose

# forwarded backend: each step POSTed to your device helper app
python3 server/droidguard_multistep_server.py --backend-device-url http://phone:9000/
```

### Exercise the protocol directly
curl -s 'http://127.0.0.1:8080/?action=begin&flow=playintegrity&source=com.dott.rider'
#   sessionId=<hex>&status=ok
curl -s -X POST 'http://127.0.0.1:8080/?action=snapshot&sessionId=<hex>&flow=playintegrity&source=com.dott.rider' \
     --data-raw 'step=2&key=value'
#   <base64url blob with step counter>
curl -s -X POST 'http://127.0.0.1:8080/?action=close&sessionId=<hex>'
#   status=ok
```

Point microG at it with the network DroidGuard URL setting, then run the
session tests in `test_droidguard_multistep_server.py` which do exactly the
three calls above over real HTTP.

## Where Play Integrity actually comes from

The session protocol fixes *microG's side* (the service entry point + a
stateful remote handle). The verdict itself still has to come from a device
Google trusts. Two honest options:

- **Recommended (mar-v-in's own approach):** an old phone that still passes
  Play Integrity (his example is a Nexus 5X), kept as a home server. The
  helper backend collects each step and returns the real DroidGuard result.
  Keep this server private + rate-limited — Google rate-limits per device;
  don't over-promise.
- **Unlock + Magisk/KernelSU + PlayIntegrityFix + TrickyStore:** a modern
  device kitted out for attestation pass. Same architecture, just a different
  device image.

Bring-your-own Google-issued token. This code cannot and does not claim to
produce one cloud-side.

## Verified here vs. device-pending (honesty matrix)

| Claim | Evidence |
| --- | --- |
| Server implements begin/snapshot/close session protocol | 12 pytest green (see above) |
| Client `guardWithRequest` service entry point implemented | kotlinc-typechecked; matches embedded lifecycle |
| Remote handle speaks sessions only for request-backed flows | kotlinc-typechecked; legacy path byte-identical |
| Server ↔ client wire encoding matches (`URL_SAFE\|NO_WRAP\|NO_PADDING`) | `test_blob_uses_client_side_encoding` |
| A real device returns a Play Integrity verdict | **Not verified here** — no physical device on this box; requires mar-v-in's old-phone setup |