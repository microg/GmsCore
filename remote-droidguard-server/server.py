#!/usr/bin/env python3
"""
Remote DroidGuard Server — reference implementation for microG GmsCore.

This server handles the begin/snapshot/close session lifecycle for multi-step
DroidGuard attestation (Play Integrity).  It receives requests from microG's
RemoteHandleImpl and proxies them to a *server-device* plugin that runs the
actual DroidGuard bytecode on a stock (or suitably patched) Android phone.

Endpoints (all POST, x-www-form-urlencoded or query-string params):
  /begin   — start a session; returns sessionId
  /snapshot — process one DroidGuard step; returns Base64-encoded result
  /close   — tear down a session

Environment variables:
  DG_PLUGIN     — path to plugin executable (default: ./plugin.py)
  DG_LISTEN_HOST — bind address      (default: 0.0.0.0)
  DG_LISTEN_PORT — TCP port          (default: 8080)

Quick start:
  pip install flask
  python server.py
"""

import os
import json
import uuid
import subprocess
import base64
import sys
import logging
from pathlib import Path
from flask import Flask, request, Response

# ---------------------------------------------------------------------------
# Config
# ---------------------------------------------------------------------------
LISTEN_HOST = os.environ.get("DG_LISTEN_HOST", "0.0.0.0")
LISTEN_PORT = int(os.environ.get("DG_LISTEN_PORT", "8080"))
PLUGIN_PATH = os.environ.get("DG_PLUGIN", str(Path(__file__).with_name("plugin.py")))

logging.basicConfig(level=logging.INFO, stream=sys.stderr,
                    format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger("droidguard-server")

app = Flask(__name__)

# In-memory session store (replace with Redis / SQLite for production)
sessions: dict[str, dict] = {}


def call_plugin(action: str, params: dict, payload: str | None = None) -> str:
    """Invoke the plugin executable with JSON on stdin; return stdout."""
    input_obj = {
        "action": action,
        "params": params,
        "payload": payload,
    }
    input_json = json.dumps(input_obj)

    logger.debug("Calling plugin %s with action=%s sessionId=%s",
                 PLUGIN_PATH, action, params.get("sessionId", "<none>"))

    result = subprocess.run(
        [sys.executable, PLUGIN_PATH],
        input=input_json,
        capture_output=True,
        text=True,
        timeout=30,
    )
    if result.returncode != 0:
        logger.error("Plugin stderr: %s", result.stderr.strip() or "(empty)")
        raise RuntimeError(f"Plugin exited {result.returncode}: {result.stderr.strip()}"
                           or f"Plugin exited {result.returncode}")

    return result.stdout.strip()


def build_params() -> dict:
    """Collect relevant request parameters into a flat dict."""
    params = {}
    # Query-string parameters
    for key in request.args:
        params[key] = request.args[key]
    # Form-encoded body parameters
    if request.form:
        for key in request.form:
            params[key] = request.form[key]
    # Also accept raw body as a single payload if it's not form-encoded
    if not request.form and request.data:
        params["__raw_body"] = request.data.decode("utf-8", errors="replace")
    return params


# ---------------------------------------------------------------------------
# Endpoints
# ---------------------------------------------------------------------------

@app.route("/begin", methods=["POST"])
def begin():
    params = build_params()
    flow = params.get("flow", "")
    source = params.get("source", "unknown")

    logger.info("Begin session — flow=%s source=%s", flow, source)

    session_id = uuid.uuid4().hex[:12]
    sessions[session_id] = {
        "flow": flow,
        "source": source,
        "created_at": params.get("created_at") or "",
    }

    try:
        result = call_plugin("begin", params | {"sessionId": session_id})
    except RuntimeError as exc:
        logger.error("Plugin begin failed: %s", exc)
        return Response(f"error=plugin_failed&message={exc}", status=502, mimetype="text/plain")

    # Plugin returns key=value lines; forward its response
    return Response(result or "sessionId=" + session_id, mimetype="text/plain")


@app.route("/snapshot", methods=["POST"])
def snapshot():
    params = build_params()
    session_id = params.get("sessionId", "")

    if not session_id:
        # Single-step mode: no session, just process in one shot
        logger.info("Snapshot — single-step mode (no sessionId)")
    else:
        if session_id not in sessions:
            logger.warning("Snapshot for unknown sessionId=%s", session_id)
            # Don't fail hard — single-step fallback may still work
        logger.info("Snapshot — sessionId=%s", session_id)

    payload = params.get("__raw_body", "")
    if not payload:
        # Try to reconstruct payload from other params
        pl_parts = [
            f"{k}={v}" for k, v in params.items()
            if not k.startswith("x-request-")
            and k not in ("action", "flow", "source", "sessionId", "__raw_body")
        ]
        if pl_parts:
            payload = "&".join(pl_parts)

    logger.debug("Snapshot payload length=%d", len(payload))

    try:
        result = call_plugin("snapshot", params | {"sessionId": session_id}, payload)
    except RuntimeError as exc:
        logger.error("Plugin snapshot failed: %s", exc)
        return Response(f"error=plugin_failed&message={exc}", status=502, mimetype="text/plain")

    # The response should be Base64-encoded bytes (the DroidGuard result).
    # Return it as-is; microG's RemoteHandleImpl will Base64-decode it.
    return Response(result or base64.b64encode(b"").decode(), mimetype="text/plain")


@app.route("/close", methods=["POST"])
def close():
    params = build_params()
    session_id = params.get("sessionId", "")

    logger.info("Close session — sessionId=%s", session_id)

    if session_id and session_id in sessions:
        try:
            call_plugin("close", {"sessionId": session_id})
        except RuntimeError as exc:
            logger.warning("Plugin close failed (non-fatal): %s", exc)
        del sessions[session_id]

    return Response("status=ok", mimetype="text/plain")


@app.route("/health", methods=["GET"])
def health():
    return {"status": "ok", "sessions": len(sessions)}


if __name__ == "__main__":
    logger.info("Starting DroidGuard server on %s:%s", LISTEN_HOST, LISTEN_PORT)
    logger.info("Plugin: %s", PLUGIN_PATH)
    if not Path(PLUGIN_PATH).exists():
        logger.warning("Plugin not found at %s — start with DG_PLUGIN=/path/to/plugin.py",
                       PLUGIN_PATH)
    app.run(host=LISTEN_HOST, port=LISTEN_PORT, debug=False)
