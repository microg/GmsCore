#!/usr/bin/env python3
"""Reference remote DroidGuard HTTP server with multi-step session support.

Subclass DroidGuardBackend and connect it to a real Play Services
DroidGuard implementation on a stock Android device.
"""

import base64
import json
import uuid
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

SESSIONS = {}


class DroidGuardBackend:
    def process(self, session_id, flow, request_b64, hashed_package_b64):
        """Return (response_b64, session_id) for the given session."""
        raise NotImplementedError


class Handler(BaseHTTPRequestHandler):
    backend = None

    def do_POST(self):
        if self.path != "/droidguard":
            self.send_error(404)
            return

        length = int(self.headers.get("Content-Length", 0))
        body = json.loads(self.rfile.read(length) or b"{}")

        session_id = body.get("sessionId") or uuid.uuid4().hex
        flow = body.get("flow")
        request_b64 = body.get("request")
        hashed_package_b64 = body.get("hashedClientPackage")

        if not request_b64:
            self._json({"error": "missing request"}, status=400)
            return

        response_b64, new_session_id = self.backend.process(
            session_id, flow, request_b64, hashed_package_b64
        )

        self._json({
            "response": response_b64,
            "sessionId": new_session_id or session_id,
        })

    def _json(self, obj, status=200):
        data = json.dumps(obj).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def log_message(self, fmt, *args):
        pass


def run(backend, host="0.0.0.0", port=8080):
    Handler.backend = backend
    server = ThreadingHTTPServer((host, port), Handler)
    print(f"DroidGuard server listening on {host}:{port}")
    server.serve_forever()


if __name__ == "__main__":
    run(DroidGuardBackend())