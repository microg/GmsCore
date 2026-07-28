#!/usr/bin/env python3
"""
droidguard_server.py — Remote DroidGuard server for microg/GmsCore #2851.

Purpose
-------
Turns an old Android phone that passes Play Integrity into a small HTTP
DroidGuard server. Other phones running microG in **Remote** mode point to
this endpoint and forward Play Integrity / DroidGuard calls through it.

Protocol
--------
The server accepts both single-step and multi-step (Play Integrity) flows.

Single-step / snapshot
   POST ``/?flow=<flow>&source=<pkg>&x-request-...=...``
   body: url-encoded key/value payload
   -> raw token bytes (Base64 URL-safe, no padding)

Multi-step / Play Integrity
   * ``/session/begin``     - create a server-side session
     returns JSON ``{"sessionId": "<id>"}``
   * ``/session/next``      - append a step
     query: ``sessionId=<id>``, body: url-encoded payload
     returns ``204 No Content``
   * ``/session/snapshot``  - finalize and return raw token bytes
     query: ``sessionId=<id>``, body: url-encoded final payload
   * ``/session/close``     - discard the session
     query: ``sessionId=<id>``
   * ``/status``            - health / active-session count (GET)

Query parameters starting with ``x-request-`` are kept and mirrored back to
the local DroidGuard runtime so the Play Integrity metadata
(sessionId, isMultiStep, stepNumber) is preserved through the proxy.

Quick start (Termux)
--------------------
.. code-block:: bash

   pkg update && pkg upgrade
   pkg install python
   curl -LO https://raw.githubusercontent.com/microg/GmsCore/master/play-services-droidguard/server/droidguard_server.py
   python3 droidguard_server.py --port 8080

Configure the client phone:
``microG Settings → DroidGuard → Remote → Remote URL: http://<server-ip>:8080/``
"""

from __future__ import annotations

import argparse
import base64
import json
import logging
import sys
import urllib.parse
import uuid
from dataclasses import dataclass, field
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from typing import Any, Dict, Mapping, Optional

__all__ = ["main"]

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
log = logging.getLogger("droidguard_server")

TOKEN_PLACEHOLDER = (
    "dG9rZW46bG9jYWwtc2VydmVyLXBsYWNlaG9sZGVyLWFsbCBwYXJhbWV0ZXJzIGFycmVk"
)


# ---------------------------------------------------------------------------
# Utilities
# ---------------------------------------------------------------------------

def _decode_form(body: bytes) -> Dict[str, str]:
    """Parse an url-encoded POST body into a flat dict (first value wins)."""
    parsed = urllib.parse.parse_qs(body.decode("utf-8", errors="replace"),
                                  keep_blank_values=True)
    return {k: v[0] for k, v in parsed.items()}


def _query_dict(query: str) -> Dict[str, str]:
    parsed = urllib.parse.parse_qs(query, keep_blank_values=True)
    return {k: v[0] for k, v in parsed.items()}


def _pad(b64: str) -> str:
    return b64 + "=" * (-len(b64) % 4)


# ---------------------------------------------------------------------------
# Session storage
# ---------------------------------------------------------------------------

@dataclass
class Session:
    flow: str = ""
    source: str = ""
    meta: Dict[str, Any] = field(default_factory=dict)
    steps: Dict[int, Dict[str, Any]] = field(default_factory=dict)
    current_step: int = 0


class SessionStore:
    def __init__(self) -> None:
        self.sessions: Dict[str, Session] = {}

    def begin(self, params: Mapping[str, str], payload: Mapping[str, str]) -> Session:
        sid = uuid.uuid4().hex[:12]
        meta: Dict[str, Any] = {
            k: v for k, v in params.items() if k.startswith("x-request-")
        }
        meta["sessionId"] = sid
        meta["isMultiStep"] = "true"
        meta["stepNumber"] = "0"
        session = Session(
            flow=params.get("flow", ""),
            source=params.get("source", ""),
            meta=meta,
            steps={0: dict(payload)},
            current_step=0,
        )
        self.sessions[sid] = session
        return session

    def next_step(self, sid: str, payload: Mapping[str, str]) -> Optional[Session]:
        session = self.sessions.get(sid)
        if session is None:
            return None
        session.current_step += 1
        session.steps[session.current_step] = dict(payload)
        session.meta["stepNumber"] = str(session.current_step)
        return session

    def snapshot(self, sid: str, payload: Mapping[str, str]) -> Optional[Session]:
        session = self.sessions.pop(sid, None)
        if session is not None:
            session.steps[session.current_step] = dict(payload)
        return session

    def close(self, sid: str) -> None:
        self.sessions.pop(sid, None)


# ---------------------------------------------------------------------------
# Request handler
# ---------------------------------------------------------------------------

SERVER_HOST = "0.0.0.0"

# Runtime configuration is resolved once at startup from CLI args.
_cfg = argparse.Namespace(
    host=SERVER_HOST, port=8080, proxy=None, token=None
)


class DroidGuardHandler(BaseHTTPRequestHandler):
    session_store = SessionStore()

    def do_GET(self) -> None:
        if self.path in ("/", "/status"):
            count = len(self.session_store.sessions)
            self._json(200, {"status": "ok", "active_sessions": count})
            return
        self._json(404, {"error": "unknown path"})

    def do_POST(self) -> None:
        length = int(self.headers.get("Content-Length", 0))
        body = self.rfile.read(length) if length else b""
        path, _, query = self.path.partition("?")
        params = _query_dict(query)
        payload = _decode_form(body) if body else {}

        if path in ("/", "/droidguard/"):
            self._handle_snapshot(params, payload)
            return
        if path == "/session/begin":
            session = self.session_store.begin(params, payload)
            self._json(201, {"sessionId": session.meta["sessionId"]})
            return
        if path == "/session/next":
            sid = params.get("sessionId", "")
            if self.session_store.next_step(sid, payload):
                self._no_content()
            else:
                self._json(404, {"error": "session not found"})
            return
        if path == "/session/snapshot":
            sid = params.get("sessionId", "")
            session = self.session_store.snapshot(sid, payload)
            if session is None:
                self._json(404, {"error": "session not found"})
            else:
                combined = {**session.meta}
                for step_data in session.steps.values():
                    combined.update(step_data)
                self._handle_snapshot(
                    {"flow": session.flow, "source": session.source, **combined},
                    combined,
                )
            return
        if path == "/session/close":
            self.session_store.close(params.get("sessionId", ""))
            self._no_content()
            return
        self._json(404, {"error": "unknown path"})

    def _handle_snapshot(self, params: Mapping[str, str], payload: Mapping[str, str]) -> None:
        try:
            token_bytes = _compute_token(params, payload)
            self._raw(200, token_bytes, "application/octet-stream")
            log.info(
                "served token for flow=%s source=%s step=%s",
                params.get("flow"),
                params.get("source"),
                params.get("x-request-stepNumber", params.get("stepNumber", "")),
            )
        except Exception as exc:
            log.exception("token generation failed")
            self._json(500, {"error": str(exc)})

    def _raw(self, code: int, data: bytes, ctype: str) -> None:
        self.send_response(code)
        self.send_header("Content-Type", ctype)
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def _json(self, code: int, data: Any) -> None:
        self._raw(code, json.dumps(data).encode("utf-8"), "application/json")

    def _no_content(self) -> None:
        self.send_response(204)
        self.send_header("Content-Length", "0")
        self.end_headers()

    def log_message(self, format: str, *args: Any) -> None:
        log.info("%s - %s", self.address_string(), format % args)


# ---------------------------------------------------------------------------
# Token computation (local DroidGuard bridge)
# ---------------------------------------------------------------------------

def _compute_token(params: Mapping[str, str], payload: Mapping[str, str]) -> bytes:
    """Generate the raw token bytes.

    Production path
    ---------------
    On a passing self-hosted server phone this handler is wrapped by a small
    GmsCore IntentService that binds the embedded DroidGuard runtime and
    forwards the merged request through ``IDroidGuardHandle`` (the same path
    used by local apps).  This pure-Python script exercises the REST surface
    and can proxy to that wrapper when ``--proxy`` is given.

    Development path
    ----------------
    * ``--proxy <host:port>`` : forward to an internal DroidGuard wrapper
      service on this phone.
    * ``--token <base64>``    : emit a fixed sample token (test mode).
    * (default)               : emit a documented placeholder token.
    """
    if _cfg.proxy:
        return _proxy_request(params, payload)
    raw = _cfg.token if _cfg.token else TOKEN_PLACEHOLDER
    try:
        return base64.urlsafe_b64decode(_pad(raw))
    except Exception as exc:
        raise ValueError(f"invalid token payload: {exc}") from exc


def _proxy_request(params: Mapping[str, str], payload: Mapping[str, str]) -> bytes:
    """Forward to an internal DroidGuard wrapper running on the server phone."""
    import urllib.request as _urllib_request

    proxy_url = f"http://{_cfg.proxy}/droidguard/"
    query = urllib.parse.urlencode({k: v for k, v in params.items()})
    body = urllib.parse.urlencode({k: v for k, v in payload.items()}).encode()
    req = _urllib_request.Request(
        f"{proxy_url}?{query}",
        data=body,
        headers={"Content-Type": "application/x-www-form-urlencoded; charset=UTF-8"},
        method="POST",
    )
    with _urllib_request.urlopen(req, timeout=30) as resp:
        return resp.read()


# ---------------------------------------------------------------------------
# Entrypoint
# ---------------------------------------------------------------------------

def main() -> None:
    global _cfg
    parser = argparse.ArgumentParser(description="Remote DroidGuard server")
    parser.add_argument("--host", default=SERVER_HOST)
    parser.add_argument("--port", type=int, default=8080)
    parser.add_argument(
        "--proxy",
        default=None,
        help="host:port of an internal DroidGuard wrapper service on this phone",
    )
    parser.add_argument(
        "--token",
        default=None,
        help="fixed URL-safe Base64 token to return (test mode)",
    )
    _cfg = parser.parse_args()  # type: ignore[assignment]

    server = ThreadingHTTPServer((_cfg.host, _cfg.port), DroidGuardHandler)
    log.info("DroidGuard server starting on %s:%d", _cfg.host, _cfg.port)
    if _cfg.proxy:
        log.info("proxying to local wrapper at http://%s", _cfg.proxy)
    elif _cfg.token:
        log.info("using fixed test token")
    else:
        log.info("using placeholder token (use --proxy or --token for a real flow)")
    log.info("configure client with: http://<device-ip>:%d/", _cfg.port)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        log.info("shutting down")
        server.shutdown()


if __name__ == "__main__":
    main()
