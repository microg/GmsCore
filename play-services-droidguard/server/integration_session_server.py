# SPDX-FileCopyrightText: 2026 microG Project Team
# SPDX-License-Identifier: Apache-2.0
"""Loopback integration gate; requires permission to create TCP sockets."""
import unittest
import protocol_cases


class SessionIntegrationTest(protocol_cases.SessionProtocolTest):
    pass


if __name__ == '__main__':
    unittest.main()
