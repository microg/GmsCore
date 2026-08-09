# Remote DroidGuard for Play Integrity

This document describes how to use remote DroidGuard to obtain Play Integrity attestations from a separate "server" device (typically a stock or properly configured device that passes integrity checks). This allows client devices (e.g. custom ROMs without root or passing attestations) to obtain valid Play Integrity tokens without running DroidGuard locally on the client.

## Background and Motivation

Play Integrity API (and previously SafetyNet) uses DroidGuard for device attestation. microG supports two DroidGuard modes:

- **Embedded** (local): Runs DroidGuard VM locally (default). Requires device to pass attestation.
- **Network** (remote): Forwards DroidGuard requests over HTTP to a remote server that performs the actual attestation.

Remote mode was historically limited and did not fully support Play Integrity flows because Play Integrity uses a **multi-step** DroidGuard process (init + multiple snapshots with session state, different flows like `pia_attest_e1` and `pia_express`), whereas most other flows are single-step.

This implementation now supports multi-step remote DroidGuard sessions.

## Benefits

- No need to root the daily driver or run obfuscated Google code on it.
- Offload attestation to a dedicated device at home (or cloud service).
- Enables commercial "integrity as a service" offerings.
- Avoids constant maintenance of bypass modules on the client device.

## How it Works

1. On the **client** (your daily device):
   - Enable "DroidGuard" in microG settings → Mode = **Network**
   - Set **Network server URL** to the HTTP endpoint of your remote DroidGuard server.
2. The client uses `RemoteHandleImpl` which speaks a simple HTTP protocol to the server.
3. On the **server device** (stock phone or server that passes Play Integrity):
   - Run a remote DroidGuard server app/service.
   - It receives flows + data, calls local `DroidGuard.getClient(...).getResults(...)` (or handle APIs) and returns the result.
4. For multi-step flows (Play Integrity), the server/client maintain a `sid` (session id) across calls.

## Setting up the Client (microG device)

1. Install/update microG GmsCore that includes the remote DroidGuard fixes (this repo after merge).
2. Open **microG Settings** → **DroidGuard** (or Device Attestation / Advanced).
3. Set **Mode** to **Network** (or "Remote").
4. Enter the **Network server URL**, e.g.:
   - `http://192.168.1.42:8080/droidguard`
   - `https://my-integrity.example.com/dg`
5. Optionally enable "Force local disabled" if you want to ensure remote is always used.
6. Test using Play Integrity API apps (e.g. Play Integrity API Checker) or apps that use Play Integrity (banking, Dott, etc.).

Settings are stored under `SettingsContract.DroidGuard.*`.

## Writing / Running a Remote DroidGuard Server

The remote server must implement a simple HTTP protocol (GET/POST query params + form body).

### Protocol (current implementation)

**URL**: `http://server:port/?flow=XXX&source=com.example&sid=...&x-request-...=...`

- Query params:
  - `flow`: the DroidGuard flow name (e.g. `pia_attest_e1`, `pia_express`, `attest`, `checkin`, `devicekey`, ...)
  - `source`: calling package name
  - `sid`: optional session identifier (for multi-step)
  - `action`: `init` | `close` | omitted (for snapshot)
  - `x-request-*`: values from `DroidGuardResultsRequest.bundle` (may be base64 for bytes)

- Body (POST, urlencoded): key=value pairs from the `snapshot(map)` data.

**Response**:
- For single step or snapshot: base64 (URL-safe, no padding) of the DroidGuard result.
- For init that returns a new session: `SID|base64result` (pipe separated) or just the SID.
- Errors: start with `ERROR ` or HTTP error status.

The server should:

1. On `action=init` or first call: create a handle / call `DroidGuard.getClient(ctx).getResults(flow, data, request)` or use the full handle `initWithRequest` + `snapshot`.
2. For Play Integrity multi-step (pia_attest_e1 / pia_express), keep state keyed by `sid`.
3. Return the raw result bytes (as returned by DroidGuard) base64 encoded.

### Minimal Reference Server (Kotlin + Ktor or plain HttpServer)

A simple reference implementation can be written as a standalone JVM app or Android service.

Here is a minimal example using Java's built-in `com.sun.net.httpserver.HttpServer` (no external deps) that runs on any Android device with microG/GMS that has DroidGuard available locally:

```kotlin
// Example: RemoteDroidGuardServer.kt
// Compile/run as part of a minimal Android app or use on desktop with a fake context if possible.
// For phone, package as an app with a foreground service exposing the port.

import android.content.Context
import com.google.android.gms.droidguard.DroidGuard
import com.google.android.gms.droidguard.internal.DroidGuardResultsRequest
import com.google.android.gms.tasks.Tasks
import fi.iki.elonen.NanoHTTPD // or use built-in server; example simplified
import java.net.InetSocketAddress
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import android.util.Base64
import java.net.URLDecoder
import java.net.URLEncoder

class RemoteDroidGuardServer(private val context: Context, port: Int = 8080) : NanoHTTPD(port) {
    private val sessions = ConcurrentHashMap<String, Any>() // placeholder for state

    override fun serve(session: IHTTPSession): Response {
        val params = session.parms
        val flow = params["flow"] ?: return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "ERROR no flow")
        val source = params["source"] ?: "unknown"
        val sid = params["sid"]
        val action = params["action"]

        val request = DroidGuardResultsRequest()
        params.filterKeys { it.startsWith("x-request-") }.forEach { (k, v) ->
            val realKey = k.removePrefix("x-request-")
            // handle base64 etc.
            try {
                request.bundle.putString(realKey, URLDecoder.decode(v, "UTF-8"))
            } catch (_: Exception) {}
        }

        val data = mutableMapOf<String, String>()
        if (session.method == Method.POST) {
            val body = session.inputStream.bufferedReader().readText()
            body.split("&").forEach {
                val (k,v) = it.split("=", limit=2).let { p -> p[0] to URLDecoder.decode(p.getOrNull(1)?:"", "UTF-8") }
                data[k] = v
            }
        }

        return try {
            val result = if (action == "init") {
                // For multi-step, we could create handle and store
                val token = DroidGuard.getClient(context).getResults(flow, data, request).get() // or await
                // Generate sid if needed for pia flows
                val newSid = UUID.randomUUID().toString()
                sessions[newSid] = Unit // store handle if advanced
                "$newSid|${Base64.encodeToString(token.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)}"
            } else if (sid != null && sessions.containsKey(sid) && action == "close") {
                sessions.remove(sid)
                "CLOSED"
            } else {
                // snapshot or simple
                val token = DroidGuard.getClient(context).getResults(flow, data, request).get()
                Base64.encodeToString(token.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            }
            newFixedLengthResponse(Response.Status.OK, "text/plain", result)
        } catch (e: Exception) {
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "ERROR ${e.message}")
        }
    }
}
```

**Notes**:
- For full multi-step support (init/snapshot/close on handle), the server should use `DroidGuardHandle` API instead of the convenience `getResults`.
- In the client `RemoteHandleImpl`, we added `sid` and `action` support.
- To run on phone: create a small APK with this server (use NanoHTTPD or OkHttp server or Ktor). Expose via a foreground service + notification. Use `adb forward` or open port on WiFi.
- For production: add auth (API key), TLS (self-signed or Let's Encrypt), rate limiting.

### Alternative: Use the archived microg/RemoteDroidGuard as starting point

The original remote implementation lived at https://github.com/microg/RemoteDroidGuard (archived). You can adapt its service to expose the HTTP endpoint described above.

## Running the Server on a Stock / Passing Device

### Recommended Setup for Server Device

- Use a stock ROM or a device that can pass **DEVICE** (or **STRONG**) integrity with real Google Play Services.
- Or use LineageOS + microG + PlayIntegrityFix + TrickyStore + valid keybox (as described in community guides).
- Keep the server device powered on and connected to the same network (or exposed via reverse proxy / Tailscale / ngrok for remote access).
- Run the Remote DroidGuard server app persistently (foreground service).
- Optionally run it headless on a Raspberry Pi with Android or use an old phone.

### Exposing Securely

- Local WiFi only (recommended for privacy).
- Use Tailscale / ZeroTier / WireGuard.
- Cloud: run a small VPS with Android-x86 or use a commercial service (future).

## Testing

1. On client set remote URL.
2. Use an app like:
   - [Play Integrity API Checker](https://play.google.com/store/apps/details?id=com.google.android.play.core.integrity.verifier) (or forks)
   - Banking / ride apps that use Play Integrity.
3. Check logs in microG / logcat for "RemoteGuardImpl".
4. On server side: watch for incoming requests.

## Limitations & Future Work

- Currently the remote implementation returns `null` for `initWithReply` (PFD objects). Full low-latency reply support may require more work.
- Session state on server is currently in-memory; restart loses sessions (fine for most flows).
- Hardware-backed strong integrity may require the server device to have a valid keybox / TEE state.
- Authentication / multi-tenancy for public servers not implemented.

## Related Code

- `play-services-droidguard/core/src/main/kotlin/org/microg/gms/droidguard/core/RemoteHandleImpl.kt`
- `DroidGuardServiceImpl.kt`
- `DroidGuardClientImpl.java`
- `IntegrityService.kt` and `IntegrityExtensions.kt` (for pia_attest_e1 / pia_express flows)
- `PoTokenHelper.kt`

## Contributing

Improvements to multi-step handling, a reference server app, and docs are welcome.

## References

- Original issue: https://github.com/microg/GmsCore/issues/2851
- RemoteDroidGuard (historical): https://github.com/microg/RemoteDroidGuard
- DroidGuard deep dive papers and community guides for passing integrity on custom devices.

---

*This feature enables privacy-friendly and maintainable Play Integrity attestation.*
