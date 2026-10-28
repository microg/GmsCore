#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 microG Project Team
# SPDX-License-Identifier: Apache-2.0

"""Focused HTTP/content-command acceptance; no Android device or native VM.

From play-services-droidguard with Python 3.8+:
python -m unittest discover -s server/tests -v
The maintained adapter and ContentBridge execute unchanged. Only the Android
content-command boundary is a disposable Python subprocess.
"""

import base64
import concurrent.futures
import http.client
import importlib.util
import json
import sys
import tempfile
import threading
import time
import unittest
from pathlib import Path
from urllib.parse import parse_qs, urlencode


_SPEC = importlib.util.spec_from_file_location(
    "droidguard_server_protocol_source", Path(__file__).resolve().parents[1] / "droidguard_server.py"
)
ADAPTER = importlib.util.module_from_spec(_SPEC)
_SPEC.loader.exec_module(ADAPTER)
TOKEN = "disposable-protocol-test-token-2026"
SERIAL = "disposable-content-fixture"

_CONTENT_FIXTURE = r'''
import base64
import json
import os
import sys
import time
from pathlib import Path

work = Path(sys.argv[1])
argv = sys.argv[2:]
method = argv[argv.index("--method") + 1]
encoded = argv[argv.index("--arg") + 1]
payload = json.loads(base64.urlsafe_b64decode(encoded + "=" * (-len(encoded) % 4)))
record = {"method": method, "payload": payload, "argv": argv}
fd = os.open(str(work / "calls.jsonl"), os.O_WRONLY | os.O_CREAT | os.O_APPEND, 0o600)
try:
    os.write(fd, (json.dumps(record, ensure_ascii=False) + "\n").encode("utf-8"))
finally:
    os.close(fd)

if method == "begin":
    session = {"com.example.A": "session-A", "com.example.B": "session-B"}[payload["source"]]
    response = {"status": "ok", "sessionId": session}
elif method == "snapshot":
    if payload["sessionId"] == "session-A":
        (work / "a-entered").touch()
        deadline = time.monotonic() + 15
        while not (work / "release-a").exists():
            if time.monotonic() >= deadline:
                raise RuntimeError("Controlled A snapshot was not released")
            time.sleep(0.01)
    result = json.dumps(payload, ensure_ascii=False, sort_keys=True).encode("utf-8")
    response = {"status": "ok", "result": base64.urlsafe_b64encode(result).decode("ascii")}
elif method == "close":
    response = {"status": "ok"}
else:
    raise RuntimeError("Unexpected content operation")

outer = base64.urlsafe_b64encode(json.dumps(response).encode("utf-8")).decode("ascii").rstrip("=")
print("Result: Bundle[{response=" + outer + "}]")
'''


class DroidGuardServerProtocolTest(unittest.TestCase):
    def test_independent_session_progresses_while_snapshot_subprocess_is_held(self):
        with tempfile.TemporaryDirectory(prefix="droidguard-protocol-") as directory:
            work = Path(directory)
            fixture = work / "content_fixture.py"
            fixture.write_text(_CONTENT_FIXTURE, encoding="utf-8")
            bridge = ADAPTER.ContentBridge("unused-adb", SERIAL, 7)
            # Retain production JSON/Base64 encoding, argv construction, subprocess
            # execution, and provider-envelope decoding. This prefix selects only
            # the disposable content-command executable, never a connected device.
            bridge.command = [sys.executable, str(fixture), str(work), "-s", SERIAL]
            server = ADAPTER.DroidGuardServer(("127.0.0.1", 0), bridge, TOKEN)
            serving = threading.Thread(target=server.serve_forever, kwargs={"poll_interval": 0.02}, daemon=True)
            serving.start()
            try:
                self._exercise_retained_sessions(server.server_address[1], work)
            finally:
                (work / "release-a").touch()
                server.shutdown()
                server.server_close()
                serving.join(2)
                self.assertFalse(serving.is_alive(), "HTTP adapter did not shut down")

    def _exercise_retained_sessions(self, port, work):
        flow_a = "pia_attest_e1 +&=% / 流"
        metadata_a = {"clientVersion": "252432000", "appArchitecture": "arm64\n'\";$(literal)|&%"}
        flow_b = "second-flow"
        metadata_b = {"clientVersion": "17", "appArchitecture": "separate"}
        data_a = {"rpc": "challenge; $(literal) &%+", "键+&=%": "值 🧪\n'\"", "empty": ""}
        data_b = {"rpc": "sign", "literal": "+ & = % /"}

        status, begin_a = self._post(port, "begin", self._begin_query(flow_a, "com.example.A", metadata_a))
        self.assertEqual(200, status)
        session_a = parse_qs(begin_a)["sessionId"][0]
        self.assertEqual("session-A", session_a)

        with concurrent.futures.ThreadPoolExecutor(max_workers=2) as requests:
            held_a = requests.submit(self._post, port, "snapshot", {"sessionId": session_a}, data_a)
            try:
                self._wait_for_file(work / "a-entered")
                self.assertFalse(held_a.done(), "A snapshot escaped its content-command latch")
                independent_b = requests.submit(self._complete_b, port, flow_b, metadata_b, data_b)
                try:
                    result_b = independent_b.result(timeout=6)
                except concurrent.futures.TimeoutError:
                    self.fail("Independent session B blocked behind the held A snapshot")
                self.assertEqual({"sessionId": "session-B", "data": data_b}, result_b)
                self.assertFalse(held_a.done(), "A finished before its explicit release")
            finally:
                (work / "release-a").touch()
            status, result_a = held_a.result(timeout=5)

        self.assertEqual(200, status)
        self.assertEqual({"sessionId": session_a, "data": data_a}, self._decode_native_fixture(result_a))
        self.assertEqual((200, "status=ok"), self._post(port, "close", {"sessionId": session_a}))

        calls = [json.loads(line) for line in (work / "calls.jsonl").read_text(encoding="utf-8").splitlines()]
        self.assertEqual(["begin", "snapshot", "begin", "snapshot", "close", "close"], [call["method"] for call in calls])
        self.assertEqual({"flow": flow_a, "source": "com.example.A", "request": metadata_a}, calls[0]["payload"])
        self.assertEqual({"flow": flow_b, "source": "com.example.B", "request": metadata_b}, calls[2]["payload"])
        self.assertEqual({"sessionId": "session-A", "data": data_a}, calls[1]["payload"])
        self.assertEqual({"sessionId": "session-B", "data": data_b}, calls[3]["payload"])
        self.assertEqual([{"sessionId": "session-B"}, {"sessionId": "session-A"}], [call["payload"] for call in calls[4:]])

        for call in calls:
            expected = ["-s", SERIAL, "shell", "content", "call", "--uri", ADAPTER.AUTHORITY,
                        "--user", "7", "--method", call["method"], "--arg"]
            self.assertEqual(expected, call["argv"][:-1])
            self.assertRegex(call["argv"][-1], r"^[A-Za-z0-9_-]+$")
            self.assertNotIn(TOKEN, json.dumps(call))
        # An absent token on any lifecycle request would prevent its content call.
        # Six distinct calls also reject silent command replay during contention.
        self.assertEqual(6, len(calls))

    def _complete_b(self, port, flow, metadata, data):
        status, begin = self._post(port, "begin", self._begin_query(flow, "com.example.B", metadata))
        self.assertEqual(200, status)
        session = parse_qs(begin)["sessionId"][0]
        self.assertEqual("session-B", session)
        status, result = self._post(port, "snapshot", {"sessionId": session}, data)
        self.assertEqual(200, status)
        self.assertEqual((200, "status=ok"), self._post(port, "close", {"sessionId": session}))
        return self._decode_native_fixture(result)

    @staticmethod
    def _begin_query(flow, source, metadata):
        return dict({"flow": flow, "source": source}, **{"x-request-" + key: value for key, value in metadata.items()})

    @staticmethod
    def _post(port, action, query, data=None):
        fields = dict({"token": TOKEN, "action": action}, **query)
        target = "/droidguard/?" + urlencode(fields, encoding="utf-8")
        body = None if data is None else urlencode(data, encoding="utf-8").encode("ascii")
        headers = {} if body is None else {"Content-Type": "application/x-www-form-urlencoded; charset=UTF-8"}
        connection = http.client.HTTPConnection("127.0.0.1", port, timeout=15)
        try:
            connection.request("POST", target, body=body, headers=headers)
            response = connection.getresponse()
            return response.status, response.read().decode("utf-8")
        finally:
            connection.close()

    @staticmethod
    def _decode_native_fixture(value):
        return json.loads(base64.urlsafe_b64decode(value + "=" * (-len(value) % 4)).decode("utf-8"))

    def _wait_for_file(self, path):
        deadline = time.monotonic() + 5
        while not path.exists():
            if time.monotonic() >= deadline:
                self.fail("A snapshot did not enter the content-command subprocess")
            time.sleep(0.01)


if __name__ == "__main__":
    unittest.main()
