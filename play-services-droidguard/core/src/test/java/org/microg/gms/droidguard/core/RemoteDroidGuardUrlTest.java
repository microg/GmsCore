/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard.core;

import org.junit.Test;

import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class RemoteDroidGuardUrlTest {
    @Test
    public void appendsToQuerylessEndpoint() throws Exception {
        assertEquals("https://example.com/dg?flow=attest&source=app",
                RemoteDroidGuardUrl.appendQuery("https://example.com/dg", "flow=attest&source=app").toExternalForm());
    }

    @Test
    public void preservesExistingEscapesAndParameters() throws Exception {
        assertEquals("https://example.com/dg?key=a%2Bb%26c&flow=x%2Fy",
                RemoteDroidGuardUrl.appendQuery("https://example.com/dg?key=a%2Bb%26c", "flow=x%2Fy").toExternalForm());
    }

    @Test
    public void handlesEmptyQuery() throws Exception {
        assertEquals("https://example.com/dg?flow=attest",
                RemoteDroidGuardUrl.appendQuery("https://example.com/dg?", "flow=attest").toExternalForm());
    }

    @Test
    public void removesFragmentBeforeAppending() throws Exception {
        assertEquals("https://example.com/dg?key=a&flow=attest",
                RemoteDroidGuardUrl.appendQuery("https://example.com/dg?key=a#help?ignored", "flow=attest").toExternalForm());
    }

    @Test
    public void preservesQueryWhenThereAreNoNewParameters() throws Exception {
        assertEquals("https://example.com/dg?key=a",
                RemoteDroidGuardUrl.appendQuery("https://example.com/dg?key=a#help", "").toExternalForm());
    }

    @Test
    public void sendsSeparateServerAndDroidGuardParametersOverHttp() throws Exception {
        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            server.setSoTimeout(5000);
            FutureTask<String> request = new FutureTask<>(() -> {
                try (Socket socket = server.accept()) {
                    socket.setSoTimeout(5000);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
                    String requestLine = reader.readLine();
                    while (!reader.readLine().isEmpty()) { /* consume headers */ }
                    socket.getOutputStream().write("HTTP/1.1 200 OK\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
                    return requestLine;
                }
            });
            Thread worker = new Thread(request);
            worker.setDaemon(true);
            worker.start();
            HttpURLConnection connection = (HttpURLConnection) RemoteDroidGuardUrl.appendQuery(
                    "http://127.0.0.1:" + server.getLocalPort() + "/dg?key=a%2Bb#help",
                    "flow=attest&source=com.example.app").openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestMethod("POST");
            try {
                assertEquals(200, connection.getResponseCode());
                assertEquals("POST /dg?key=a%2Bb&flow=attest&source=com.example.app HTTP/1.1", request.get(5, TimeUnit.SECONDS));
            } finally {
                connection.disconnect();
            }
        }
    }
}
