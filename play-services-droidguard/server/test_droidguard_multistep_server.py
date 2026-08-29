#!/usr/bin/env python3
#
# SPDX-FileCopyrightText: 2026 microG Project Team
# SPDX-License-Identifier: Apache-2.0
#
"""Unit + integration tests for the multi-step Remote DroidGuard server."""

import base64
import json
import threading
import time
import urllib.parse
import urllib.request

import pytest

from droidguard_multistep_server import (
    HttpBackend,
    MockBackend,
    RemoteDroidGuardServer,
    b64url,
    make_server,
)


@pytest.fixture
def dg():
    return RemoteDroidGuardServer(MockBackend())


def test_begin_returns_session_id(dg):
    sid = dg.begin("attest", "com.example.app", {"nonce": "abc"})
    assert len(sid) == 32
    assert sid in dg.sessions
    assert dg.sessions[sid].flow == "attest"
    assert dg.sessions[sid].source == "com.example.app"


def test_multi_step_session_continuity(dg):
    """Steps must be bound to the session and ordered 1..N."""
    sid = dg.begin("attest", "com.dott.rider", {"nonce": "cafebabe"})
    blob1 = dg.snapshot(sid, "attest", "com.dott.rider", {}, {"k": "v1"})
    blob2 = dg.snapshot(sid, "attest", "com.dott.rider", {}, {"k": "v2"})
    assert blob1 == b"mock/1:attest:com.dott.rider"
    # step 2 must carry both artifacts, proving server-side continuity
    assert blob2 == b"mock/1:attest:com.dott.rider|2:attest:com.dott.rider"
    assert dg.sessions[sid].step == 2


def test_unknown_session_raises(dg):
    with pytest.raises(KeyError):
        dg.snapshot("does-not-exist", "attest", "x", {}, {})


def test_close_removes_session(dg):
    sid = dg.begin("attest", "com.example", {})
    assert dg.close(sid) is True
    assert sid not in dg.sessions
    assert dg.close(sid) is False


def test_legacy_single_step_compat(dg):
    """A stateless (no session) request must still return a blob."""
    blob = dg.legacy_snapshot("ads", "com.example", {}, {"a": "b"})
    assert blob == b"legacy/single-step"


def test_blob_uses_client_side_encoding():
    """Encode matches RemoteHandleImpl (URL_SAFE | NO_WRAP | NO_PADDING)."""
    raw = b"\xfb\xff\x00\x01\xfe"
    enc = b64url(raw)
    assert enc == base64.urlsafe_b64encode(raw).decode("ascii").rstrip("=")
    assert "=" not in enc
    assert base64.urlsafe_b64decode(enc + "=" * (-len(enc) % 4)) == raw


def test_auth_required():
    dg = RemoteDroidGuardServer(MockBackend(), token="sekret")
    assert dg.check_auth("sekret") is True
    assert dg.check_auth("nope") is False
    assert dg.check_auth(None) is False


def test_ttl_cleanup():
    dg = RemoteDroidGuardServer(MockBackend(), timeout_s=0.001)
    sid = dg.begin("attest", "com.example", {})
    time.sleep(0.05)
    assert dg.cleanup() == 1
    assert sid not in dg.sessions


class _DeviceHelper:
    """Fake server-device DroidGuard helper that records what it received."""

    def __init__(self):
        from http.server import BaseHTTPRequestHandler, HTTPServer

        self.requests = []

        class H(BaseHTTPRequestHandler):
            def do_POST(self_):
                length = int(self_.headers.get("Content-Length", 0))
                body = self_.rfile.read(length).decode("utf-8")
                outer.requests.append(dict(urllib.parse.parse_qsl(body)))
                data = b"device-blob"
                self_.send_response(200)
                self_.send_header("Content-Type", "application/octet-stream")
                self_.send_header("Content-Length", str(len(data)))
                self_.end_headers()
                self_.wfile.write(data)

            def log_message(self_, *a):
                pass

        outer = self
        self.httpd = HTTPServer(("127.0.0.1", 0), H)
        self.port = self.httpd.server_address[1]
        self.thread = threading.Thread(target=self.httpd.serve_forever, daemon=True)
        self.thread.start()

    def close(self):
        self.httpd.shutdown()


def test_http_backend_forwarding():
    helper = _DeviceHelper()
    try:
        dg = RemoteDroidGuardServer(
            HttpBackend(f"http://127.0.0.1:{helper.port}", timeout=5)
        )
        sid = dg.begin("attest", "com.dott.rider", {"nonce": "n1"})
        dg.snapshot(sid, "attest", "com.dott.rider", {}, {"payload": "p1"})
        dg.snapshot(sid, "attest", "com.dott.rider", {}, {"payload": "p2"})
        assert len(helper.requests) == 2
        assert helper.requests[0]["sessionId"] == sid
        assert helper.requests[0]["step"] == "1"
        assert helper.requests[0]["flow"] == "attest"
        assert helper.requests[1]["sessionId"] == sid
        assert helper.requests[1]["step"] == "2"
        assert helper.requests[1]["payload"] == "p2"
    finally:
        helper.close()


def _http_post(url, query="", body=None, token=None):
    headers = {"Content-Type": "application/x-www-form-urlencoded; charset=UTF-8"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    req = urllib.request.Request(f"{url}?{query}", data=body, headers=headers, method="POST")
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            return resp.status, resp.read().decode("utf-8")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8")


def test_http_protocol_end_to_end():
    dg = RemoteDroidGuardServer(MockBackend())
    server = make_server(dg, "127.0.0.1", 0)
    port = server.server_address[1]
    thread = threading.Thread(target=server.serve_forever, daemon=True)
    thread.start()
    try:
        url = f"http://127.0.0.1:{port}"
        code, body = _http_post(url, "action=begin&flow=attest&source=com.dott.rider")
        assert code == 200
        sid = urllib.parse.parse_qs(body)["sessionId"][0]

        code, blob1 = _http_post(url, f"action=snapshot&sessionId={sid}", body=b"k=v1")
        assert code == 200
        code, blob2 = _http_post(url, f"action=snapshot&sessionId={sid}", body=b"k=v2")
        assert code == 200
        assert blob1 == b64url(b"mock/1:attest:com.dott.rider")
        assert blob2 == b64url(b"mock/1:attest:com.dott.rider|2:attest:com.dott.rider")

        code, body = _http_post(url, f"action=close&sessionId={sid}")
        assert code == 200

        code, _ = _http_post(url, f"action=snapshot&sessionId={sid}", body=b"k=v3")
        assert code == 404
    finally:
        server.shutdown()


def test_http_legacy_path_still_works():
    dg = RemoteDroidGuardServer(MockBackend())
    server = make_server(dg, "127.0.0.1", 0)
    port = server.server_address[1]
    thread = threading.Thread(target=server.serve_forever, daemon=True)
    thread.start()
    try:
        code, blob = _http_post(f"http://127.0.0.1:{port}",
                                "flow=ads&source=com.example", body=b"a=b")
        assert code == 200
        assert blob == b64url(b"legacy/single-step")
    finally:
        server.shutdown()


def test_http_auth_enforced():
    dg = RemoteDroidGuardServer(MockBackend(), token="sekret")
    server = make_server(dg, "127.0.0.1", 0)
    port = server.server_address[1]
    thread = threading.Thread(target=server.serve_forever, daemon=True)
    thread.start()
    try:
        code, _ = _http_post(f"http://127.0.0.1:{port}", "action=begin")
        assert code == 403
        code, body = _http_post(f"http://127.0.0.1:{port}", "action=begin", token="sekret")
        assert code == 200
    finally:
        server.shutdown()