/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.wearable;

import org.junit.Test;
import org.microg.wearable.proto.RootMessage;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class SocketConnectionLifecycleTest {
    @Test(timeout = 10000) public void listenerAcceptsNewPeerAfterMalformedConnection() throws Exception {
        int port;
        try (ServerSocket reservation = SocketConnectionThread.createEmulatorListener(0, () -> { })) {
            port = reservation.getLocalPort();
        }
        CountDownLatch firstClosed = new CountDownLatch(1);
        CountDownLatch secondClosed = new CountDownLatch(2);
        AtomicInteger accepted = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();
        SocketConnectionThread server = SocketConnectionThread.serverListen(port, new WearableConnection.Listener() {
            @Override public void onConnected(WearableConnection connection) { accepted.incrementAndGet(); }
            @Override public void onMessage(WearableConnection connection, RootMessage message) { fail("Invalid frame delivered"); }
            @Override public void onDisconnected() { firstClosed.countDown(); secondClosed.countDown(); }
            @Override public void onConnectionError(String type, String location) { failed.incrementAndGet(); }
        }, () -> { });
        server.start();
        try {
            try (Socket first = connect(port)) {
                new DataOutputStream(first.getOutputStream()).writeInt(0);
                assertTrue(firstClosed.await(3, TimeUnit.SECONDS));
            }
            assertTrue(server.isAlive());
            try (Socket second = connect(port)) {
                new DataOutputStream(second.getOutputStream()).writeInt(-1);
                assertTrue(secondClosed.await(3, TimeUnit.SECONDS));
            }
            assertEquals(2, accepted.get());
            assertEquals(2, failed.get());
            assertTrue(server.isAlive());
        } finally {
            server.close();
            server.join(3000);
        }
        assertFalse(server.isAlive());
        try (ServerSocket rebound = SocketConnectionThread.createEmulatorListener(port, () -> { })) {
            assertEquals(port, rebound.getLocalPort());
        }
    }

    @Test(timeout = 5000) public void listenerCanCloseBeforeAcceptingAnyPeer() throws Exception {
        SocketConnectionThread server = SocketConnectionThread.serverListen(0, new WearableConnection.Listener() {
            @Override public void onConnected(WearableConnection connection) { fail("No peer was expected"); }
            @Override public void onMessage(WearableConnection connection, RootMessage message) { fail("No message was expected"); }
            @Override public void onDisconnected() { fail("No peer was expected"); }
        }, () -> { });
        server.close();
        server.start();
        server.join(3000);
        assertFalse(server.isAlive());
        assertNull(server.getWearableConnection());
    }

    private static Socket connect(int port) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        IOException last = null;
        while (System.nanoTime() < deadline) {
            try { return new Socket(InetAddress.getByName("127.0.0.1"), port); }
            catch (IOException failure) { last = failure; Thread.sleep(20); }
        }
        throw new IOException("Test listener did not become available", last);
    }
}
