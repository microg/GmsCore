#!/usr/bin/env python3
"""
DroidGuard Plugin — reference implementation.

This plugin receives JSON on stdin: {"action": "...", "params": {...}, "payload": "..."}
and must return the appropriate response on stdout.

In production, this process would forward requests to an Android device running
DroidGuard bytecode (either via ADB, a companion APK, or an HTTP bridge).

For testing / development, this stub returns dummy data that exercises the protocol.
"""

import json
import sys
import base64
import hashlib
import uuid


def main():
    raw = sys.stdin.read()
    try:
        msg = json.loads(raw)
    except json.JSONDecodeError as e:
        print(f"error=invalid_json&message={e}", file=sys.stderr)
        sys.exit(1)

    action = msg.get("action", "")
    params = msg.get("params", {})
    payload = msg.get("payload", "") or ""
    session_id = params.get("sessionId", "")

    if action == "begin":
        # In production: forward to server device which starts a DroidGuard session.
        # The server device needs:
        #   - microG installed with remote DroidGuard enabled
        #   - PlayIntegrityFix / TrickyStore to spoof device state
        #   - DroidGuard VM to execute the bytecode
        # Return sessionId and any session metadata.
        result_id = session_id or uuid.uuid4().hex[:12]
        print(f"sessionId={result_id}")

    elif action == "snapshot":
        # In production: forward payload to DroidGuard VM on the server device,
        # capture the raw response bytes, Base64-encode them, and return.
        #
        # Dummy response below exercises the protocol — replace with real DroidGuard
        # invocation once a server device is configured.
        payload_bytes = payload.encode("utf-8")
        dummy_hash = hashlib.sha256(payload_bytes).digest()
        result_b64 = base64.b64encode(dummy_hash).decode("ascii")
        print(result_b64)

    elif action == "close":
        # In production: tear down the DroidGuard session on the server device.
        # The server device should clean up any temporary state.
        print("status=ok")

    else:
        print(f"error=unknown_action&action={action}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
