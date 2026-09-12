#!/usr/bin/env python3
#
# SPDX-FileCopyrightText: 2025 microG Project Team
# SPDX-License-Identifier: Apache-2.0
#
"""
Remote DroidGuard Server for microG

This script runs on an Android device (via Termux) and serves DroidGuard
attestation results to remote microG clients. The device runs microG in
embedded mode and forwards requests to the local DroidGuard runtime.

Usage:
  1. Install Termux and Python on the server phone
  2. Install microG with DroidGuard enabled in embedded mode
  3. Run: python3 droidguard_server.py [--port 8080] [--host 0.0.0.0]

Protocol:
  The server handles three actions via HTTP POST:
  - begin:   Start a new DroidGuard session, returns a session ID
  - snapshot: Execute DroidGuard with provided data, returns base64 result
  - close:   End a session and clean up resources
"""

import argparse
import base64
import json
import logging
import os
import sys
import time
import uuid
from http.server import HTTPServer, BaseHTTPRequestHandler
from urllib.parse import parse_qs, urlparse

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    handlers=[logging.StreamHandler(sys.stdout)],
)
log = logging.getLogger("droidguard-server")

sessions = {}


class DroidGuardHandler(BaseHTTPRequestHandler):
    def do_POST(self):
        parsed = urlparse(self.path)
        params = parse_qs(parsed.query)

        action = params.get("action", [None])[0]
        flow = params.get("flow", [""])[0]
        source = params.get("source", ["unknown"])[0]
        session_id = params.get("sessionId", [None])[0]

        content_length = int(self.headers.get("Content-Length", 0))
        body = self.rfile.read(content_length).decode("utf-8") if content_length > 0 else ""
        post_data = parse_qs(body) if body else {}

        log.info("POST action=%s flow=%s source=%s session=%s", action, flow, source, session_id)

        if action == "begin":
            self.handle_begin(flow, source, params)
        elif action == "snapshot":
            self.handle_snapshot(flow, source, session_id, params, post_data)
        elif action == "close":
            self.handle_close(session_id)
        else:
            self.send_error(400, f"Unknown action: {action}")

    def handle_begin(self, flow, source, params):
        session_id = str(uuid.uuid4())
        x_request_params = {k: v[0] for k, v in params.items() if k.startswith("x-request-")}

        sessions[session_id] = {
            "flow": flow,
            "source": source,
            "created": time.time(),
            "request_params": x_request_params,
            "snapshot_count": 0,
        }

        log.info("Session started: %s (flow=%s, source=%s)", session_id, flow, source)
        response = f"sessionId={session_id}&status=ok"
        self.send_response(200)
        self.send_header("Content-Type", "text/plain; charset=UTF-8")
        self.end_headers()
        self.wfile.write(response.encode("utf-8"))

    def handle_snapshot(self, flow, source, session_id, params, post_data):
        if session_id and session_id not in sessions:
            self.send_error(404, f"Session not found: {session_id}")
            return

        if session_id:
            session = sessions[session_id]
            session["snapshot_count"] += 1
            log.info(
                "Snapshot #%d for session %s (flow=%s)",
                session["snapshot_count"],
                session_id,
                flow,
            )
        else:
            log.info("Single-step snapshot (flow=%s, source=%s)", flow, source)

        result = self.execute_droidguard(flow, post_data)
        self.send_response(200)
        self.send_header("Content-Type", "text/plain; charset=UTF-8")
        self.end_headers()
        self.wfile.write(result.encode("utf-8"))

    def handle_close(self, session_id):
        if session_id and session_id in sessions:
            session = sessions.pop(session_id)
            log.info(
                "Session closed: %s (snapshots=%d)",
                session_id,
                session["snapshot_count"],
            )
        self.send_response(200)
        self.send_header("Content-Type", "text/plain; charset=UTF-8")
        self.end_headers()
        self.wfile.write(b"status=ok")

    def execute_droidguard(self, flow, post_data):
        """
        Execute DroidGuard locally via the microG command line interface.

        This uses the `content` command to invoke the microG DroidGuard service.
        On a properly configured device, the DroidGuard embedded runtime handles
        the actual attestation work.
        """
        try:
            data_map = {}
            for key, values in post_data.items():
                data_map[key] = values[0] if isinstance(values, list) and len(values) == 1 else values

            request_json = json.dumps({"flow": flow, "data": data_map})

            cmd = [
                "content",
                "call",
                "--uri", "content://org.microg.gms.droidguard",
                "--method", "guard",
                "--extra", f"flow:s:{flow}",
                "--extra", f"data:s:{json.dumps(data_map)}",
            ]

            log.info("Executing DroidGuard for flow: %s", flow)

            import subprocess
            proc = subprocess.run(
                cmd,
                capture_output=True,
                text=True,
                timeout=30,
            )

            if proc.returncode == 0 and proc.stdout.strip():
                raw_result = proc.stdout.strip()
                try:
                    result_bytes = raw_result.encode("utf-8")
                    encoded = base64.urlsafe_b64encode(result_bytes).decode("utf-8").rstrip("=")
                    return encoded
                except Exception as e:
                    log.warning("Failed to encode result: %s", e)

            fallback = f"ERROR :DroidGuard execution failed for flow={flow}"
            return base64.urlsafe_b64encode(fallback.encode("utf-8")).decode("utf-8").rstrip("=")

        except subprocess.TimeoutExpired:
            log.error("DroidGuard execution timed out for flow: %s", flow)
            error = f"ERROR :DroidGuard timeout for flow={flow}"
            return base64.urlsafe_b64encode(error.encode("utf-8")).decode("utf-8").rstrip("=")
        except FileNotFoundError:
            log.error("content command not found. This server must run on an Android device with microG.")
            error = "ERROR :content command not available - server must run on Android device with microG"
            return base64.urlsafe_b64encode(error.encode("utf-8")).decode("utf-8").rstrip("=")
        except Exception as e:
            log.error("DroidGuard execution error: %s", e)
            error = f"ERROR :{e}"
            return base64.urlsafe_b64encode(error.encode("utf-8")).decode("utf-8").rstrip("=")

    def log_message(self, format, *args):
        log.info(format % args)


def cleanup_stale_sessions(timeout=3600):
    now = time.time()
    stale = [sid for sid, s in sessions.items() if now - s["created"] > timeout]
    for sid in stale:
        log.info("Cleaning up stale session: %s", sid)
        sessions.pop(sid, None)


def main():
    parser = argparse.ArgumentParser(description="Remote DroidGuard Server for microG")
    parser.add_argument("--host", default="0.0.0.0", help="Host to bind to (default: 0.0.0.0)")
    parser.add_argument("--port", type=int, default=8080, help="Port to listen on (default: 8080)")
    args = parser.parse_args()

    server = HTTPServer((args.host, args.port), DroidGuardHandler)
    log.info("DroidGuard server starting on %s:%d", args.host, args.port)
    log.info("Configure microG client to use: http://<device-ip>:%d/droidguard/", args.port)

    try:
        server.serve_forever()
    except KeyboardInterrupt:
        log.info("Server shutting down")
        server.shutdown()


if __name__ == "__main__":
    main()
