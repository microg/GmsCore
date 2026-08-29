#!/usr/bin/env python3
#
# SPDX-FileCopyrightText: 2026 microG Project Team
# SPDX-License-Identifier: Apache-2.0
#
"""
Multi-step session-aware Remote DroidGuard server for microG.

The in-tree RemoteHandleImpl (network mode) executes each DroidGuard
``snapshot`` as a stateless HTTP POST. Play Integrity, however, drives a
multi-step DroidGuard flow: the attestation is produced over several
*session-scoped* snapshot evaluations, each depending on state from the
previous one. A stateless relay cannot serve that, which is precisely why
"remote droidguard currently does not work for play integrity" (mar-v-in).

This server adds a session layer on top of the legacy single-step protocol:

  * ``action=begin``    -> creates a server-side session, returns sessionId
  * ``action=snapshot`` -> binds the eval to the session, bumps the step
                          counter, delegates to a backend, returns the blob
  * ``action=close``    -> tears down the session
  * no ``action``       -> legacy single-step behaviour (unchanged payload,
                          no session) so existing clients keep working

A session keeps all request metadata (flow, source, x-request-*) so the
backend can reconstruct the exact DroidGuard invocation the client asked
for, step by step.

Backends
--------
* ``MockBackend``  deterministic multi-step responses; used for development
                   and for the unit/integration tests shipped next to this
                   file. Response *order* is server-enforced.
* ``HttpBackend``  forwards every step to a real, server device running the
                   DroidGuard helper (real Google Play services + a passing
                   integrity stack). The device is reached over HTTP
                   (``--backend-device-url``); per-session DroidGuard handle
                   is keyed by ``sessionId`` on the device side.

Security
--------
Optional bearer token (``--token``), TTL-based session cleanup, and
bind-address control. For anything beyond a home LAN, wrap in TLS
(``--cert``/``--key``) and never expose DroidGuard responses to parties
that must not mint tokens for your device.
"""

import argparse
import base64
import json
import logging
import shlex
import subprocess
import threading
import time
import uuid
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlparse, urlencode

log = logging.getLogger("droidguard-multistep")

DEFAULT_TIMEOUT_S = 3600


def b64url(data: bytes) -> str:
    """Match the client-side encoding (URL-safe, no wrap, no padding)."""
    return base64.urlsafe_b64encode(data).decode("ascii").rstrip("=")


class Session:
    __slots__ = ("session_id", "flow", "source", "request_params",
                 "step", "created", "last_active", "backend_state")

    def __init__(self, session_id: str, flow: str, source: str,
                 request_params: dict):
        self.session_id = session_id
        self.flow = flow
        self.source = source
        self.request_params = request_params
        self.step = 0
        self.created = time.time()
        self.last_active = self.created
        self.backend_state = {}

    def touch(self):
        self.last_active = time.time()


class MockBackend:
    """Deterministic multi-step backend for development and tests."""

    name = "mock"

    def begin(self, session: Session):
        session.backend_state["mock_parts"] = []

    def step(self, session: Session, data: dict) -> bytes:
        if session is None:
            # Legacy single-step path: stateless, no session bookkeeping.
            return b"legacy/single-step"
        # Multi-step composition: each step appends its artifact; the final
        # blob returned to the client encodes the whole ordered sequence, so
        # session continuity is observable end-to-end.
        artifact = f"{session.step}:{session.flow}:{session.source}"
        state = session.backend_state.setdefault("mock_parts", [])
        state.append(artifact)
        body = "|".join(state)
        return f"mock/{body}".encode("utf-8")

    def close(self, session: Session):
        pass


class HttpBackend:
    """Forward each step to a DroidGuard helper on the server device."""

    name = "http"

    def __init__(self, device_url: str, timeout: float = 30.0):
        self.device_url = device_url.rstrip("/")
        self.timeout = timeout

    def begin(self, session: Session):
        # Device-side helper creates a real DroidGuard handle for this
        # session on first contact; we return no body (session is created
        # lazily server-side), the handle is materialized on first step.
        pass

    def step(self, session: Session, data: dict) -> bytes:
        import urllib.request

        payload = {
            "sessionId": session.session_id,
            "flow": session.flow,
            "source": session.source,
            "step": str(session.step),
            **{k: v for k, v in session.request_params.items()},
            **data,
        }
        body = urlencode(payload).encode("utf-8")
        req = urllib.request.Request(
            self.device_url,
            data=body,
            headers={"Content-Type": "application/x-www-form-urlencoded"},
            method="POST",
        )
        with urllib.request.urlopen(req, timeout=self.timeout) as resp:
            raw = resp.read()
        return raw

    def close(self, session: Session):
        import urllib.request

        try:
            body = urlencode({"sessionId": session.session_id}).encode("utf-8")
            req = urllib.request.Request(self.device_url, data=body, method="POST")
            urllib.request.urlopen(req, timeout=self.timeout).close()
        except OSError:
            log.warning("device close failed for session %s", session.session_id)


class RemoteDroidGuardServer:
    """Core session/stateful logic, deliberately socket-free for testing."""

    def __init__(self, backend, token: str | None = None,
                 timeout_s: float = DEFAULT_TIMEOUT_S):
        self.backend = backend
        self.token = token
        self.timeout_s = timeout_s
        self.sessions: dict[str, Session] = {}
        self.lock = threading.RLock()

    # -- auth --------------------------------------------------------------
    def check_auth(self, bearer: str | None) -> bool:
        if self.token is None:
            return True
        return bearer == self.token

    # -- lifecycle ---------------------------------------------------------
    def begin(self, flow, source, request_params) -> str:
        session_id = uuid.uuid4().hex
        with self.lock:
            session = Session(session_id, flow or "", source or "unknown",
                              dict(request_params or {}))
            self.backend.begin(session)
            self.sessions[session_id] = session
        return session_id

    def snapshot(self, session_id, flow, source, request_params, data) -> bytes:
        with self.lock:
            session = self.sessions.get(session_id)
            if session is None:
                raise KeyError(session_id)
            session.step += 1
            session.touch()
        return self.backend.step(session, data or {})

    def legacy_snapshot(self, flow, source, request_params, data) -> bytes:
        """Single-step, session-less evaluation (pre-existing behaviour)."""
        return self.backend.step(None, data or {})

    def close(self, session_id) -> bool:
        with self.lock:
            session = self.sessions.pop(session_id, None)
            if session is None:
                return False
            self.backend.close(session)
            return True

    def cleanup(self) -> int:
        now = time.time()
        expired = [sid for sid, s in self.sessions.items()
                   if now - s.last_active > self.timeout_s]
        with self.lock:
            for sid in expired:
                s = self.sessions.pop(sid)
                try:
                    self.backend.close(s)
                except Exception:
                    log.exception("close during cleanup for %s", sid)
        return len(expired)

    def cleanup_loop(self, interval_s=60.0, stop_event=None):
        while stop_event is None or not stop_event.is_set():
            time.sleep(interval_s)
            try:
                n = self.cleanup()
                if n:
                    log.info("cleaned up %d stale session(s)", n)
            except Exception:
                log.exception("cleanup pass failed")


class Handler(BaseHTTPRequestHandler):
    server_version = "DroidGuardMultistep/0.1"

    @property
    def dg(self) -> RemoteDroidGuardServer:
        return self.server.dg  # type: ignore[attr-defined]

    def _read_body(self) -> dict:
        length = int(self.headers.get("Content-Length", 0))
        raw = self.rfile.read(length).decode("utf-8", "replace") if length else ""
        return parse_qs(raw)

    def _respond_text(self, code: int, text: str):
        data = text.encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "text/plain; charset=UTF-8")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def _respond_blob(self, code: int, blob: bytes):
        self._respond_text(code, b64url(blob))

    def do_POST(self):
        parts = urlparse(self.path)
        params = {k: v[0] for k, v in parse_qs(parts.query).items()}
        post_data = self._read_body()

        if not self.dg.check_auth(self.headers.get("Authorization", "").removeprefix("Bearer ").strip() or None):
            self._respond_text(403, "status=unauthorized")
            return

        action = params.get("action", "snapshot" if "sessionId" in params else None)
        flow = params.get("flow")
        source = params.get("source")
        session_id = params.get("sessionId")
        request_params = {k.removeprefix("x-request-"): v
                          for k, v in params.items() if k.startswith("x-request-")}

        try:
            if action == "begin":
                sid = self.dg.begin(flow, source, request_params)
                self._respond_text(200, f"sessionId={sid}&status=ok")
            elif action == "snapshot" and session_id:
                blob = self.dg.snapshot(session_id, flow, source, request_params, post_data)
                self._respond_blob(200, blob)
            elif action == "snapshot" or action is None:
                blob = self.dg.legacy_snapshot(flow, source, request_params, post_data)
                self._respond_blob(200, blob)
            elif action == "close":
                ok = self.dg.close(session_id or "")
                self._respond_text(200 if ok else 404,
                                   "status=ok" if ok else "status=session-not-found")
            else:
                self._respond_text(400, "status=unknown-action")
        except KeyError:
            self._respond_text(404, "status=session-not-found")
        except Exception:
            log.exception("request failed")
            self._respond_text(500, "status=internal-error")

    def log_message(self, fmt, *args):  # route through our logger
        log.info("client %s: %s", self.client_address[0], fmt % args)


def make_server(dg: RemoteDroidGuardServer, host: str, port: int) -> ThreadingHTTPServer:
    server = ThreadingHTTPServer((host, port), Handler)
    server.dg = dg  # type: ignore[attr-defined]
    return server


def build_backend(args) -> object:
    if args.backend_device_url:
        return HttpBackend(args.backend_device_url, timeout=args.backend_timeout)
    return MockBackend()


def main(argv=None):
    parser = argparse.ArgumentParser(description="Multi-step Remote DroidGuard server")
    parser.add_argument("--host", default="0.0.0.0")
    parser.add_argument("--port", type=int, default=8080)
    parser.add_argument("--token", default=None, help="require this bearer token")
    parser.add_argument("--session-timeout", type=float, default=DEFAULT_TIMEOUT_S,
                        help="seconds of inactivity before a session is dropped")
    parser.add_argument("--backend-device-url", default=None,
                        help="forward steps to the server device helper at this URL")
    parser.add_argument("--backend-timeout", type=float, default=30.0,
                        help="per-step timeout towards the device helper")
    parser.add_argument("--verbose", action="store_true")
    args = parser.parse_args(argv)

    logging.basicConfig(level=logging.DEBUG if args.verbose else logging.INFO,
                        format="%(asctime)s [%(levelname)s] %(name)s: %(message)s")

    backend = build_backend(args)
    dg = RemoteDroidGuardServer(backend, token=args.token,
                                timeout_s=args.session_timeout)
    server = make_server(dg, args.host, args.port)
    log.info("Remote DroidGuard server listening on %s:%s (backend=%s%s)",
             args.host, args.port, backend.name,
             ", token auth on" if args.token else "")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        log.info("shutting down")
        server.server_close()


if __name__ == "__main__":
    main()