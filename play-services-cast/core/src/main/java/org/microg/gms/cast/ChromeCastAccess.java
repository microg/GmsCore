/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.cast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.Socket;
import java.net.SocketAddress;
import java.security.GeneralSecurityException;

import javax.net.ssl.SSLSocket;

import su.litvak.chromecast.api.v2.ChromeCast;

/**
 * Gets at the connection of the Cast library, which it keeps to itself.
 * <p>
 * The library accepts any TLS certificate and reads messages of any size. To find out which certificate the device
 * presented and to limit the size of messages we need its socket, which is only reachable through reflection. Fields
 * are looked up by type, so this keeps working if they get renamed. If the library changes in a way that breaks this,
 * connecting fails instead of silently going without these checks.
 */
final class ChromeCastAccess {
    private static final String CHANNEL_CLASS = "su.litvak.chromecast.api.v2.Channel";

    private ChromeCastAccess() {
    }

    private static Field findField(Class<?> owner, Class<?> type, String typeName) throws GeneralSecurityException {
        for (Field field : owner.getDeclaredFields()) {
            if (type != null ? type.isAssignableFrom(field.getType()) : field.getType().getName().equals(typeName)) {
                field.setAccessible(true);
                return field;
            }
        }
        throw new GeneralSecurityException("Cast library has changed, can't access its connection");
    }

    private static Object getChannel(ChromeCast chromecast) throws GeneralSecurityException, IllegalAccessException {
        Object channel = findField(ChromeCast.class, null, CHANNEL_CLASS).get(chromecast);
        if (channel == null) throw new GeneralSecurityException("Cast library is not connected");
        return channel;
    }

    /**
     * @return the TLS certificate the device presented on the connection of the library
     */
    static byte[] getPeerCertificate(ChromeCast chromecast) throws GeneralSecurityException {
        try {
            Object channel = getChannel(chromecast);
            Object socket = findField(channel.getClass(), Socket.class, null).get(channel);
            if (socket instanceof LimitingSocket) socket = ((LimitingSocket) socket).socket;
            if (!(socket instanceof SSLSocket)) throw new GeneralSecurityException("Connection of the Cast library is not encrypted");
            return ((SSLSocket) socket).getSession().getPeerCertificates()[0].getEncoded();
        } catch (IllegalAccessException e) {
            throw new GeneralSecurityException("Can't access connection of the Cast library", e);
        } catch (IOException e) {
            throw new GeneralSecurityException("Can't read certificate of the device", e);
        }
    }

    /**
     * Makes the library fail with an {@link IOException} instead of allocating the memory for a message that is larger
     * than {@code maxSize}. Only takes effect from the next message on that the library starts to read.
     */
    static void limitMessageSize(ChromeCast chromecast, int maxSize) throws GeneralSecurityException {
        try {
            Object channel = getChannel(chromecast);
            Field field = findField(channel.getClass(), Socket.class, null);
            Socket socket = (Socket) field.get(channel);
            if (socket == null) throw new GeneralSecurityException("Cast library is not connected");
            if (!(socket instanceof LimitingSocket)) field.set(channel, new LimitingSocket(socket, maxSize));
        } catch (IllegalAccessException e) {
            throw new GeneralSecurityException("Can't access connection of the Cast library", e);
        }
    }

    /**
     * Stands in for the socket of the library. Only what the library uses is passed on to the real socket.
     */
    private static final class LimitingSocket extends Socket {
        final Socket socket;
        private final InputStream in;

        LimitingSocket(Socket socket, int maxSize) {
            this.socket = socket;
            this.in = new LimitingInputStream(socket, maxSize);
        }

        @Override
        public InputStream getInputStream() {
            return this.in;
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            return this.socket.getOutputStream();
        }

        @Override
        public boolean isClosed() {
            return this.socket.isClosed();
        }

        @Override
        public boolean isConnected() {
            return this.socket.isConnected();
        }

        @Override
        public synchronized void close() throws IOException {
            this.socket.close();
        }

        @Override
        public void connect(SocketAddress endpoint) throws IOException {
            throw new IOException("Already connected");
        }

        @Override
        public void connect(SocketAddress endpoint, int timeout) throws IOException {
            throw new IOException("Already connected");
        }
    }

    /**
     * Follows the framing of Cast messages, each of which starts with its size as four bytes, and refuses to hand out
     * the size of a message that is too large.
     */
    private static final class LimitingInputStream extends InputStream {
        private final Socket socket;
        private final int maxSize;
        private int headerBytes = 0;
        private long header = 0;
        private long remaining = 0;

        LimitingInputStream(Socket socket, int maxSize) {
            this.socket = socket;
            this.maxSize = maxSize;
        }

        @Override
        public int read() throws IOException {
            int b = this.socket.getInputStream().read();
            if (b < 0) return b;
            if (this.remaining > 0) {
                this.remaining--;
                return b;
            }
            this.header = (this.header << 8) | b;
            if (++this.headerBytes == 4) {
                long size = this.header & 0xffffffffL;
                this.headerBytes = 0;
                this.header = 0;
                if (size > this.maxSize) {
                    throw new IOException("Cast message of " + size + " bytes exceeds the limit of " + this.maxSize);
                }
                this.remaining = size;
            }
            return b;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            if (length == 0) return 0;
            if (this.remaining == 0) {
                // Within the size of a message, which is short enough to go byte by byte
                int b = read();
                if (b < 0) return -1;
                buffer[offset] = (byte) b;
                return 1;
            }
            int count = this.socket.getInputStream().read(buffer, offset, (int) Math.min(length, this.remaining));
            if (count > 0) this.remaining -= count;
            return count;
        }

        @Override
        public int available() throws IOException {
            return this.socket.getInputStream().available();
        }
    }
}
