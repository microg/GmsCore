# SPDX-FileCopyrightText: 2026 microG Project Team
# SPDX-License-Identifier: Apache-2.0
import base64
import http.client
import threading
import unittest
from urllib.parse import urlencode
from session_server import SessionServer


class Handle:
    def __init__(self):
        self.steps = []
        self.closed = False

    def snapshot(self, data):
        self.steps.append(data)
        if data.get('fail') == 'yes':
            raise RuntimeError('backend failed')
        return bytes([len(self.steps), 0, 255])

    def close(self):
        self.closed = True


class SessionProtocolTest(unittest.TestCase):
    def setUp(self):
        self.created = []

        def factory(flow, source, request):
            handle = Handle()
            self.created.append((flow, source, request, handle))
            return handle

        self.server = SessionServer(('127.0.0.1', 0), factory)
        self.thread = threading.Thread(target=self.server.serve_forever)
        self.thread.start()

    def tearDown(self):
        self.server.shutdown()
        self.thread.join()
        self.server.server_close()

    def post(self, operation, parameters=None, raw=None):
        connection = http.client.HTTPConnection(*self.server.server_address, timeout=2)
        connection.request('POST', '/v1/' + operation,
                           raw if raw is not None else urlencode(parameters or {}),
                           {'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8'})
        response = connection.getresponse()
        result = response.status, response.read().decode()
        connection.close()
        return result

    def init(self):
        status, session = self.post('init', {'source': 'example.client', 'flow': 'pia_attest_e1',
                                            'request-clientVersion': '123'})
        self.assertEqual(status, 200)
        return session

    def test_multiple_snapshots_retain_handle_and_metadata(self):
        session = self.init()
        for step in (1, 2):
            status, body = self.post('snapshot', {'session': session, 'data-input': 'a+b & ü'})
            self.assertEqual(status, 200)
            self.assertEqual(base64.urlsafe_b64decode(body + '='), bytes([step, 0, 255]))
        self.assertEqual(len(self.created), 1)
        flow, source, request, handle = self.created[0]
        self.assertEqual((flow, source, request), ('pia_attest_e1', 'example.client', {'clientVersion': '123'}))
        self.assertEqual(handle.steps, [{'input': 'a+b & ü'}] * 2)
        self.assertEqual(self.post('close', {'session': session})[0], 200)
        self.assertTrue(handle.closed)
        self.assertEqual(self.post('snapshot', {'session': session})[0], 410)
        self.assertEqual(self.post('close', {'session': session})[0], 410)

    def test_isolation_and_shutdown_cleanup(self):
        first, second = self.init(), self.init()
        self.assertNotEqual(first, second)
        self.post('snapshot', {'session': first})
        self.assertEqual(self.created[1][3].steps, [])
        self.post('close', {'session': first})
        self.assertFalse(self.created[1][3].closed)
        self.server.close_sessions()
        self.assertTrue(self.created[1][3].closed)

    def test_expiration_and_capacity(self):
        session = self.init()
        self.server.capacity = 1
        self.assertEqual(self.post('init', {'source': 'another'})[0], 503)
        self.server.ttl = 0
        self.assertEqual(self.post('snapshot', {'session': session})[0], 410)
        self.assertTrue(self.created[0][3].closed)

    def test_failure_invalidates_session(self):
        session = self.init()
        self.assertEqual(self.post('snapshot', {'session': session, 'data-fail': 'yes'})[0], 500)
        self.assertTrue(self.created[0][3].closed)
        self.assertEqual(self.post('snapshot', {'session': session})[0], 410)

    def test_malformed_requests(self):
        self.assertEqual(self.post('unknown')[0], 404)
        self.assertEqual(self.post('init', {})[0], 400)
        self.assertEqual(self.post('init', raw='source=a&source=b')[0], 400)
        self.assertEqual(self.post('init', raw='invalid')[0], 400)
        self.assertEqual(self.created, [])


if __name__ == '__main__':
    unittest.main()
