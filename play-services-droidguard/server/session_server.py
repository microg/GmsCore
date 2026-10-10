#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 microG Project Team
# SPDX-License-Identifier: Apache-2.0
"""Reference v1 transport. A backend must supply persistent DroidGuard handles."""
import argparse
import base64
import importlib
import secrets
import time
from http.server import BaseHTTPRequestHandler, HTTPServer
from urllib.parse import parse_qs


class SessionServer(HTTPServer):
    def __init__(self, address, factory, ttl=120, capacity=32):
        super().__init__(address, Handler)
        self.factory = factory
        self.ttl = ttl
        self.capacity = capacity
        self.sessions = {}

    def expire(self):
        now = time.monotonic()
        for key, (handle, accessed) in list(self.sessions.items()):
            if now - accessed >= self.ttl:
                del self.sessions[key]
                handle.close()

    def close_sessions(self):
        for handle, _ in list(self.sessions.values()):
            try:
                handle.close()
            except Exception:
                pass
        self.sessions.clear()

    def server_close(self):
        try:
            self.close_sessions()
        finally:
            super().server_close()


class Handler(BaseHTTPRequestHandler):
    def log_message(self, *_):
        pass  # Do not log session identifiers or attestation inputs.

    def reply(self, status, body=b''):
        self.send_response(status)
        self.send_header('Content-Type', 'text/plain; charset=UTF-8')
        self.send_header('Content-Length', str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_POST(self):
        self.connection.settimeout(15)
        try:
            if self.path not in ('/v1/init', '/v1/snapshot', '/v1/close'):
                self.reply(404)
                return
            if self.headers.get('Content-Type') != 'application/x-www-form-urlencoded; charset=UTF-8':
                self.reply(415)
                return
            length = int(self.headers.get('Content-Length', '-1'))
            if not 0 <= length <= 1048576:
                self.reply(413)
                return
            body = self.rfile.read(length)
            if len(body) != length:
                self.reply(400)
                return
            parsed = parse_qs(body.decode('utf-8'), keep_blank_values=True, strict_parsing=True)
            if any(len(values) != 1 for values in parsed.values()):
                self.reply(400)
                return
            parameters = {key: values[0] for key, values in parsed.items()}
            self.server.expire()
            if self.path == '/v1/init':
                if 'source' not in parameters or not parameters['source']:
                    self.reply(400)
                    return
                if len(self.server.sessions) >= self.server.capacity:
                    self.reply(503)
                    return
                handle = self.server.factory(parameters.get('flow'), parameters['source'],
                                             {k[8:]: v for k, v in parameters.items() if k.startswith('request-')})
                session = secrets.token_urlsafe(32)
                self.server.sessions[session] = (handle, time.monotonic())
                self.reply(200, session.encode('ascii'))
                return
            session = parameters.get('session')
            entry = self.server.sessions.get(session)
            if entry is None:
                self.reply(410)
                return
            handle, _ = entry
            if self.path == '/v1/close':
                del self.server.sessions[session]
                handle.close()
                self.reply(200)
            else:
                data = {k[5:]: v for k, v in parameters.items() if k.startswith('data-')}
                try:
                    result = handle.snapshot(data)
                    encoded = base64.urlsafe_b64encode(result).rstrip(b'=')
                    if len(encoded) > 1048576:
                        raise ValueError('Result too large')
                except Exception:
                    del self.server.sessions[session]
                    handle.close()
                    raise
                self.server.sessions[session] = (handle, time.monotonic())
                self.reply(200, encoded)
        except (ValueError, UnicodeError):
            self.reply(400)
        except Exception:
            self.reply(500)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--backend', required=True, help='Python module exporting create_handle(flow, source, request)')
    parser.add_argument('--port', type=int, default=8080)
    args = parser.parse_args()
    backend = importlib.import_module(args.backend)
    with SessionServer(('127.0.0.1', args.port), backend.create_handle) as server:
        server.serve_forever()
