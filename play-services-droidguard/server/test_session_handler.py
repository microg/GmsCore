# SPDX-FileCopyrightText: 2026 microG Project Team
# SPDX-License-Identifier: Apache-2.0
"""Exercise real HTTP parsing/serialization with in-memory socket streams."""
import io
from urllib.parse import urlencode
import protocol_cases
from session_server import Handler, SessionServer


class Connection:
    def __init__(self, request):
        self.input = io.BytesIO(request)
        self.output = io.BytesIO()

    def makefile(self, mode, *_):
        return self.input

    def sendall(self, data):
        self.output.write(data)

    def settimeout(self, timeout):
        pass


class SessionHandlerTest(protocol_cases.SessionProtocolTest):
    def setUp(self):
        self.created = []

        def factory(flow, source, request):
            handle = protocol_cases.Handle()
            self.created.append((flow, source, request, handle))
            return handle

        self.server = SessionServer.__new__(SessionServer)
        self.server.factory = factory
        self.server.ttl = 120
        self.server.capacity = 32
        self.server.sessions = {}

    def tearDown(self):
        self.server.close_sessions()

    def post(self, operation, parameters=None, raw=None):
        body = (raw if raw is not None else urlencode(parameters or {})).encode()
        request = (f'POST /v1/{operation} HTTP/1.0\r\n'
                   'Content-Type: application/x-www-form-urlencoded; charset=UTF-8\r\n'
                   f'Content-Length: {len(body)}\r\n\r\n').encode() + body
        connection = Connection(request)
        Handler(connection, ('127.0.0.1', 0), self.server)
        header, result = connection.output.getvalue().split(b'\r\n\r\n', 1)
        return int(header.split()[1]), result.decode()
