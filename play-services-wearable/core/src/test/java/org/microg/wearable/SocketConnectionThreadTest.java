/*
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.wearable;

import org.junit.Test;
import org.microg.wearable.proto.RootMessage;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class SocketConnectionThreadTest {
    @Test public void emulatorListenerIsBoundOnlyToIpv4Loopback() throws Exception {
        try (ServerSocket listener = SocketConnectionThread.createEmulatorListener(0, () -> { })) {
            assertEquals("127.0.0.1", listener.getInetAddress().getHostAddress());
            assertTrue(listener.getInetAddress().isLoopbackAddress());
            assertFalse(listener.getInetAddress().isAnyLocalAddress());
            try (Socket client = new Socket("127.0.0.1", listener.getLocalPort());
                 Socket accepted = listener.accept()) {
                assertTrue(accepted.getLocalAddress().isLoopbackAddress());
            }
        }
    }

    @Test public void closesAcceptedClientAndListener() throws Exception {
        int port;
        try (ServerSocket reservation = new ServerSocket(0)) { port = reservation.getLocalPort(); }
        CountDownLatch accepted = new CountDownLatch(1);
        CountDownLatch disconnected = new CountDownLatch(1);
        SocketConnectionThread server = SocketConnectionThread.serverListen(port, new WearableConnection.Listener() {
            @Override public void onConnected(WearableConnection connection) { accepted.countDown(); }
            @Override public void onMessage(WearableConnection connection, RootMessage message) {}
            @Override public void onDisconnected() { disconnected.countDown(); }
        }, () -> { });
        server.start();
        Socket client = null;
        try {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (client == null && System.nanoTime() < deadline) {
                try { client = new Socket("127.0.0.1", port); }
                catch (IOException e) { Thread.sleep(10); }
            }
            assertNotNull("Test TCP listener did not start", client);
            assertTrue(accepted.await(5, TimeUnit.SECONDS));
            server.close();
            assertTrue(disconnected.await(5, TimeUnit.SECONDS));
            server.join(5000);
            assertFalse(server.isAlive());
            client.setSoTimeout(1000);
            assertEquals(-1, client.getInputStream().read());
        } finally {
            if (client != null) client.close();
            server.close();
            server.join(5000);
        }
    }

    @Test public void deniedPolicyCannotBindAListener() throws Exception {
        int port;
        try (ServerSocket reservation = new ServerSocket(0)) { port = reservation.getLocalPort(); }
        try {
            SocketConnectionThread.createEmulatorListener(port, () -> { throw new SecurityException("Denied test policy"); });
            fail("A denied policy must not bind a socket");
        } catch (SecurityException expected) {
            try (ServerSocket rebound = new ServerSocket(port)) { assertEquals(port, rebound.getLocalPort()); }
        }
    }

    @Test public void deniedPolicyCannotCreateAServerThread() {
        try {
            SocketConnectionThread.serverListen(0, null, () -> { throw new SecurityException("Denied test policy"); });
            fail("A denied policy must reject before creating a server thread");
        } catch (SecurityException expected) { }
    }

    @Test public void closeBeforeStartNeverBindsAfterAReplacementOwnsThePort() throws Exception {
        AtomicInteger authorizations = new AtomicInteger();
        try (ServerSocket replacement = SocketConnectionThread.createEmulatorListener(0, () -> { })) {
            SocketConnectionThread old = SocketConnectionThread.serverListen(replacement.getLocalPort(), null,
                    authorizations::incrementAndGet);
            old.close();
            old.start();
            old.join(5000);
            assertFalse(old.isAlive());
            assertEquals("A cancelled thread must not attempt to create its listener", 1, authorizations.get());
            try (Socket client = new Socket("127.0.0.1", replacement.getLocalPort());
                 Socket accepted = replacement.accept()) {
                assertTrue(accepted.isConnected());
            }
        }
    }

    private WearableConnection.Listener listener(CountDownLatch accepted) {
        return new WearableConnection.Listener() {
            @Override public void onConnected(WearableConnection connection) { accepted.countDown(); }
            @Override public void onMessage(WearableConnection connection, RootMessage message) { }
            @Override public void onDisconnected() { }
        };
    }

    private Socket connect(int port) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        do {
            try { return new Socket("127.0.0.1", port); }
            catch (IOException e) { Thread.sleep(10); }
        } while (System.nanoTime() < deadline);
        throw new AssertionError("The replacement listener did not start");
    }

    @Test public void replacingListenerClosesOldClientAndLateCloseCannotStopTheNewOne() throws Exception {
        int port;
        try (ServerSocket reservation = new ServerSocket(0)) { port = reservation.getLocalPort(); }
        CountDownLatch firstAccepted = new CountDownLatch(1), nextAccepted = new CountDownLatch(1);
        SocketConnectionThread first = SocketConnectionThread.serverListen(port, listener(firstAccepted), () -> { });
        SocketConnectionThread next = SocketConnectionThread.serverListen(port, listener(nextAccepted), () -> { });
        first.start();
        try (Socket firstClient = connect(port)) {
            assertTrue(firstAccepted.await(5, TimeUnit.SECONDS));
            first.close();
            // Do not join first: production must not wait while holding the wearable state monitor.
            next.start();
            try (Socket nextClient = connect(port)) {
                assertTrue(nextAccepted.await(5, TimeUnit.SECONDS));
                first.close();
                firstClient.setSoTimeout(1000);
                assertEquals(-1, firstClient.getInputStream().read());
                nextClient.setSoTimeout(100);
                try {
                    nextClient.getInputStream().read();
                    fail("The new client must stay open when the previous listener closes again");
                } catch (SocketTimeoutException expected) { }
                assertTrue(next.isAlive());
            }
        } finally {
            first.close();
            next.close();
            first.join(5000);
            next.join(5000);
        }
        assertFalse(first.isAlive());
        assertFalse(next.isAlive());
    }
}
