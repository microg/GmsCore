#!/usr/bin/env python3
#
# SPDX-FileCopyrightText: 2025 microG Project Team
# SPDX-License-Identifier: Apache-2.0
#
"""Authenticated HTTP adapter for the operator-enabled DroidGuard provider.

The Android provider owns real native handles. This process only translates the
existing begin/snapshot/close HTTP protocol to explicit adb shell content calls.
See ../REMOTE_DROIDGUARD_SETUP.md for deployment and upstream protocol credit.
"""

import argparse
import base64
import hmac
import json
import logging
import os
import re
import subprocess
import threading
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import parse_qs, urlencode, urlsplit

LOG = logging.getLogger("droidguard-server")
AUTHORITY = "content://org.microg.gms.droidguard"
MAX_BODY = 32 * 1024
MAX_QUERY = 8 * 1024
MAX_ARGUMENT = 65536
MAX_RESULT = 256 * 1024
RESPONSE = re.compile(r"Result: Bundle\[\{response=([A-Za-z0-9_-]+={0,2})\}\]")


class BridgeError(Exception):
    def __init__(self, status, message):
        super().__init__(message)
        self.status = status


class ContentBridge:
    def __init__(self, adb, serial, user):
        self.command = [adb, "-s", serial]
        self.user = user

    def check_device(self):
        try:
            result = subprocess.run(
                self.command + ["get-state"], capture_output=True, text=True,
                encoding="utf-8", timeout=10,
            )
        except (OSError, subprocess.TimeoutExpired) as exc:
            raise BridgeError(503, "Selected ADB device is unavailable") from exc
        if result.returncode or result.stdout.strip() != "device":
            raise BridgeError(503, "Selected ADB device is not connected and authorized")

    def call(self, method, payload):
        raw = json.dumps(payload, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
        argument = base64.urlsafe_b64encode(raw).decode("ascii").rstrip("=")
        if len(raw) > 48 * 1024 or len(argument) > MAX_ARGUMENT:
            raise BridgeError(413, "Request exceeds the provider payload limit")
        # Every argument sent through the device shell is fixed, numeric, or
        # URL-safe Base64. In particular, untrusted JSON is never shell text.
        command = self.command + [
            "shell", "content", "call", "--uri", AUTHORITY,
            "--user", str(self.user), "--method", method, "--arg", argument,
        ]
        try:
            result = subprocess.run(
                command, capture_output=True, text=True, encoding="utf-8",
                timeout=55,
            )
        except subprocess.TimeoutExpired as exc:
            raise BridgeError(504, "Provider command timed out; operation was not retried") from exc
        except (OSError, UnicodeError) as exc:
            raise BridgeError(503, "Unable to communicate with the selected ADB device") from exc
        # Android's content CLI can print an exception and still exit zero.
        # Never reinterpret diagnostic stdout as a DroidGuard result.
        if result.returncode or result.stderr.strip() or len(result.stdout) > 512 * 1024:
            raise BridgeError(502, "Provider command failed; check the selected device and provider")
        match = RESPONSE.fullmatch(result.stdout.strip())
        if match is None:
            raise BridgeError(502, "Provider returned no valid response envelope")
        try:
            encoded = match.group(1)
            decoded = base64.b64decode(encoded + "=" * (-len(encoded) % 4), altchars=b"-_", validate=True)
            response = json.loads(decoded.decode("utf-8"))
        except (ValueError, UnicodeError) as exc:
            raise BridgeError(502, "Provider returned an invalid response envelope") from exc
        if not isinstance(response, dict):
            raise BridgeError(502, "Provider returned an invalid response object")
        if response.get("status") == "error":
            code = response.get("code")
            if type(code) is not int or code not in (400, 404, 502, 503, 504):
                code = 502
            message = response.get("error")
            if not isinstance(message, str) or len(message) > 2048:
                message = "DroidGuard provider failed"
            raise BridgeError(code, message)
        if response.get("status") != "ok":
            raise BridgeError(502, "Provider returned an unknown status")
        return response


def one_value(fields, name, required=False):
    values = fields.get(name, [])
    if len(values) > 1 or (required and (not values or not values[0])):
        raise BridgeError(400, "Missing or repeated parameter: " + name)
    return values[0] if values else ""


class DroidGuardHandler(BaseHTTPRequestHandler):
    def setup(self):
        self.request.settimeout(10)
        super().setup()

    def respond(self, status, body, content_type="text/plain; charset=UTF-8"):
        data = body.encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(data)))
        self.send_header("Cache-Control", "no-store")
        self.send_header("Connection", "close")
        self.end_headers()
        self.wfile.write(data)
        self.close_connection = True

    def do_POST(self):
        try:
            if len(self.path.encode("utf-8")) > MAX_QUERY:
                raise BridgeError(414, "Request URL exceeds the adapter limit")
            target = urlsplit(self.path)
            if target.path not in ("/droidguard", "/droidguard/"):
                raise BridgeError(404, "Unknown endpoint")
            fields = parse_qs(target.query, keep_blank_values=True, errors="strict", max_num_fields=128)
            token = one_value(fields, "token")
            if not token or not hmac.compare_digest(token.encode("utf-8"), self.server.token):
                raise BridgeError(401, "Authentication required")
            action = one_value(fields, "action", required=True)
            if action not in ("begin", "snapshot", "close"):
                raise BridgeError(400, "Unknown DroidGuard action")
            if self.headers.get("Transfer-Encoding") is not None:
                raise BridgeError(400, "Transfer-Encoding is not supported")
            lengths = self.headers.get_all("Content-Length", [])
            if len(lengths) > 1:
                raise BridgeError(400, "Repeated Content-Length")
            length = int(lengths[0]) if lengths else 0
            if length < 0 or length > MAX_BODY:
                raise BridgeError(413, "Request body exceeds the adapter limit")
            if length and self.headers.get_content_type() != "application/x-www-form-urlencoded":
                raise BridgeError(400, "Expected a form-encoded snapshot body")
            body = self.rfile.read(length)
            if len(body) != length:
                raise BridgeError(400, "Incomplete request body")
            data = parse_qs(body.decode("utf-8"), keep_blank_values=True, errors="strict", max_num_fields=256)
            if action == "begin":
                if length:
                    raise BridgeError(400, "Begin does not accept snapshot data")
                request = {key[len("x-request-"):]: one_value(fields, key)
                           for key in fields if key.startswith("x-request-")}
                response = self.server.bridge.call("begin", {
                    "flow": one_value(fields, "flow", required=True),
                    "source": one_value(fields, "source", required=True),
                    "request": request,
                })
                session_id = response.get("sessionId")
                if not isinstance(session_id, str) or not session_id or len(session_id) > 64:
                    raise BridgeError(502, "Provider did not return a session ID")
                self.respond(200, urlencode({"sessionId": session_id, "status": "ok"}))
            elif action == "snapshot":
                response = self.server.bridge.call("snapshot", {
                    "sessionId": one_value(fields, "sessionId", required=True),
                    "data": {key: one_value(data, key) for key in data},
                })
                result = response.get("result")
                if not isinstance(result, str) or len(result) > MAX_RESULT:
                    raise BridgeError(502, "Provider did not return a DroidGuard result")
                try:
                    base64.b64decode(result + "=" * (-len(result) % 4), altchars=b"-_", validate=True)
                except ValueError as exc:
                    raise BridgeError(502, "Provider returned invalid DroidGuard Base64") from exc
                self.respond(200, result)
            else:
                if length:
                    raise BridgeError(400, "Close does not accept snapshot data")
                self.server.bridge.call("close", {"sessionId": one_value(fields, "sessionId", required=True)})
                self.respond(200, "status=ok")
        except BridgeError as exc:
            self.respond(exc.status, json.dumps({"error": str(exc)}), "application/json")
        except (ValueError, UnicodeError):
            self.respond(400, '{"error":"Invalid request encoding or length"}', "application/json")
        except (TimeoutError, ConnectionError):
            self.close_connection = True

    def log_request(self, code="-", size="-"):
        # The base URL contains a credential; never log the URL or snapshot map.
        LOG.info("HTTP %s", code)

    def log_message(self, format, *args):
        pass


class DroidGuardServer(ThreadingHTTPServer):
    daemon_threads = True

    def __init__(self, address, bridge, token):
        self.bridge = bridge
        self.token = token.encode("utf-8")
        self.slots = threading.BoundedSemaphore(10)
        super().__init__(address, DroidGuardHandler)

    def process_request(self, request, client_address):
        if not self.slots.acquire(blocking=False):
            try:
                request.settimeout(1)
                request.sendall(b"HTTP/1.1 503 Service Unavailable\r\nContent-Length: 0\r\nConnection: close\r\n\r\n")
            finally:
                self.shutdown_request(request)
            return
        try:
            super().process_request(request, client_address)
        except BaseException:
            self.slots.release()
            raise

    def process_request_thread(self, request, client_address):
        try:
            super().process_request_thread(request, client_address)
        finally:
            self.slots.release()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--serial", required=True, help="Explicit ADB serial of the Embedded-mode server device")
    parser.add_argument("--user", type=int, default=0, help="Android user ID on that device (default: 0)")
    parser.add_argument("--adb", default="adb", help="ADB executable (default: adb)")
    parser.add_argument("--host", default="127.0.0.1", help="Listener address (default: 127.0.0.1)")
    parser.add_argument("--port", type=int, default=8080)
    credentials = parser.add_mutually_exclusive_group()
    credentials.add_argument("--token-file", type=Path, help="File containing the HTTP authentication token")
    credentials.add_argument("--token-env", default="DROIDGUARD_TOKEN", help="Token environment variable (default: DROIDGUARD_TOKEN)")
    args = parser.parse_args()
    if args.user < 0 or not 1 <= args.port <= 65535:
        parser.error("--user must be nonnegative and --port must be 1 through 65535")
    try:
        token = args.token_file.read_text(encoding="utf-8").strip() if args.token_file else os.environ.get(args.token_env, "")
    except (OSError, UnicodeError):
        parser.error("Unable to read the token file")
    if len(token) < 24 or any(char.isspace() for char in token):
        parser.error("Configure a random token of at least 24 characters without whitespace")
    logging.basicConfig(level=logging.INFO, format="%(levelname)s %(message)s")
    bridge = ContentBridge(args.adb, args.serial, args.user)
    try:
        bridge.check_device()
        with DroidGuardServer((args.host, args.port), bridge, token) as server:
            LOG.info("DroidGuard adapter listening on %s:%s", args.host, args.port)
            try:
                server.serve_forever()
            except KeyboardInterrupt:
                pass
    except (BridgeError, OSError) as exc:
        parser.error(str(exc))


if __name__ == "__main__":
    main()
