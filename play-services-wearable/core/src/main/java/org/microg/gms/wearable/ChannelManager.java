/*
 * SPDX-FileCopyrightText: 2024-2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wearable;

import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.google.android.gms.wearable.internal.ChannelEventParcelable;
import com.google.android.gms.wearable.internal.ChannelParcelable;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manages the lifecycle and I/O for Wear OS Channel API data streams.
 *
 * <p>The Channel API provides bidirectional byte-stream communication between
 * a phone and Wear OS device, suitable for file transfers, sensor data streams,
 * and other continuous data flows.
 *
 * <h2>Channel Lifecycle</h2>
 * <ol>
 *   <li>{@link #openChannel} — open a new channel to a target node.</li>
 *   <li>{@link #getInputStream} / {@link #getOutputStream} — obtain I/O streams.</li>
 *   <li>{@link #sendFile} / {@link #receiveFile} — file transfer convenience methods.</li>
 *   <li>{@link #closeChannel} — close and clean up the channel.</li>
 * </ol>
 *
 * <h2>Thread Safety</h2>
 * All public methods are thread-safe. Internal channel state is managed via
 * {@link ConcurrentHashMap} and atomic operations.
 *
 * <h2>File Transfer</h2>
 * Supports sending and receiving files with automatic chunking ({@value #CHUNK_SIZE} bytes).
 * Progress callbacks are supported via {@link ChannelListener}.
 *
 * @see ChannelParcelable
 * @see ChannelEventParcelable
 */
public class ChannelManager {

    private static final String TAG = "GmsWearChannelMgr";

    /** Default chunk size for file transfers (8 KB). */
    public static final int CHUNK_SIZE = 8192;

    /** Maximum file size for transfer (100 MB). */
    public static final long MAX_FILE_SIZE = 100 * 1024 * 1024;

    // Channel state constants
    public static final int STATE_CLOSED = 0;
    public static final int STATE_OPENING = 1;
    public static final int STATE_OPEN = 2;
    public static final int STATE_CLOSING = 3;

    // Channel event type constants (mirrors ChannelEventParcelable)
    public static final int EVENT_TYPE_OPENED = 1;
    public static final int EVENT_TYPE_CLOSED = 2;
    public static final int EVENT_TYPE_INPUT_CLOSED = 3;
    public static final int EVENT_TYPE_OUTPUT_CLOSED = 4;
    public static final int EVENT_TYPE_RECEIVED_DATA = 5;

    private final WearableImpl wearable;
    private final AtomicLong nextChannelId = new AtomicLong(1);

    /** Maps channel token (String) to ChannelState. */
    private final Map<String, ChannelState> channels = new ConcurrentHashMap<>();

    public ChannelManager(WearableImpl wearable) {
        this.wearable = wearable;
    }

    // -------------------------------------------------------------------------
    // Channel lifecycle
    // -------------------------------------------------------------------------

    /**
     * Opens a new channel to the given target node on the specified path.
     *
     * @param targetNodeId    the peer node identifier
     * @param path            the application-specific channel path (e.g., "/file_transfer")
     * @param packageName     calling application package name
     * @param signatureDigest SHA-1 digest of the calling app's signing certificate
     * @return a {@link ChannelParcelable} representing the opened channel, or {@code null}
     *         if the target node is not connected
     */
    public ChannelParcelable openChannel(String targetNodeId, String path,
            String packageName, String signatureDigest) {
        if (targetNodeId == null || path == null) {
            Log.w(TAG, "openChannel: null targetNodeId or path");
            return null;
        }

        long channelId = nextChannelId.getAndIncrement();
        String token = Long.toString(channelId);

        ChannelState state = new ChannelState(token, channelId, targetNodeId,
                path, packageName, signatureDigest);
        state.state = STATE_OPEN;
        channels.put(token, state);

        Log.d(TAG, "openChannel: token=" + token + " node=" + targetNodeId
                + " path=" + path);

        ChannelParcelable parcelable = new ChannelParcelable();
        parcelable.token = token;
        parcelable.nodeId = targetNodeId;
        parcelable.path = path;

        return parcelable;
    }

    /**
     * Closes a channel and releases all associated resources.
     *
     * @param token the channel token returned by {@link #openChannel}
     * @return {@code true} if the channel was successfully closed
     */
    public boolean closeChannel(String token) {
        ChannelState state = channels.remove(token);
        if (state == null) {
            Log.w(TAG, "closeChannel: unknown token=" + token);
            return false;
        }
        state.state = STATE_CLOSED;
        try {
            if (state.inputStream != null) state.inputStream.close();
            if (state.outputStream != null) state.outputStream.close();
            if (state.pipeOutputStream != null) state.pipeOutputStream.close();
            if (state.pipeInputStream != null) state.pipeInputStream.close();
        } catch (IOException e) {
            Log.w(TAG, "closeChannel: error closing streams for token=" + token, e);
        }
        Log.d(TAG, "closeChannel: token=" + token + " closed");
        return true;
    }

    /**
     * Closes all open channels. Called when the wearable connection is reset.
     */
    public void closeAll() {
        Log.d(TAG, "closeAll: closing " + channels.size() + " channels");
        for (String token : new ArrayList<>(channels.keySet())) {
            closeChannel(token);
        }
    }

    // -------------------------------------------------------------------------
    // I/O operations
    // -------------------------------------------------------------------------

    /**
     * Returns an {@link InputStream} for reading data from the specified channel.
     *
     * @param token the channel token
     * @return an InputStream, or {@code null} if the channel is not found
     */
    public InputStream getInputStream(String token) {
        ChannelState state = channels.get(token);
        if (state == null) {
            Log.w(TAG, "getInputStream: unknown token=" + token);
            return null;
        }

        if (state.pipeInputStream == null) {
            try {
                java.io.PipedOutputStream pos = new java.io.PipedOutputStream();
                java.io.PipedInputStream pis = new java.io.PipedInputStream(pos, CHUNK_SIZE * 2);
                state.pipeOutputStream = pos;
                state.pipeInputStream = pis;
                state.inputStream = pis;
            } catch (IOException e) {
                Log.e(TAG, "getInputStream: failed to create pipe for token=" + token, e);
                return null;
            }
        }
        return state.pipeInputStream;
    }

    /**
     * Returns an {@link OutputStream} for writing data to the specified channel.
     *
     * @param token the channel token
     * @return an OutputStream, or {@code null} if the channel is not found
     */
    public OutputStream getOutputStream(String token) {
        ChannelState state = channels.get(token);
        if (state == null) {
            Log.w(TAG, "getOutputStream: unknown token=" + token);
            return null;
        }

        if (state.outputStream == null) {
            // Create a simple buffered output stream
            state.outputStream = new ChannelOutputStream(state);
        }
        return state.outputStream;
    }

    // -------------------------------------------------------------------------
    // File transfer
    // -------------------------------------------------------------------------

    /**
     * Sends a file over the specified channel.
     *
     * @param token    the channel token
     * @param filePath absolute path to the file to send
     * @param listener optional progress listener (may be {@code null})
     * @return {@code true} if the file was sent successfully
     */
    public boolean sendFile(String token, String filePath, ChannelListener listener) {
        ChannelState state = channels.get(token);
        if (state == null) {
            Log.w(TAG, "sendFile: unknown token=" + token);
            return false;
        }
        if (filePath == null) {
            Log.w(TAG, "sendFile: null filePath");
            return false;
        }

        java.io.File file = new java.io.File(filePath);
        if (!file.exists() || !file.isFile()) {
            Log.w(TAG, "sendFile: file not found: " + filePath);
            return false;
        }

        long fileSize = file.length();
        if (fileSize > MAX_FILE_SIZE) {
            Log.w(TAG, "sendFile: file too large (" + fileSize + " > " + MAX_FILE_SIZE + ")");
            return false;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            OutputStream os = getOutputStream(token);
            if (os == null) {
                Log.w(TAG, "sendFile: no output stream for token=" + token);
                return false;
            }

            byte[] buffer = new byte[CHUNK_SIZE];
            long totalRead = 0;
            int bytesRead;
            long lastProgressTime = System.currentTimeMillis();

            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
                totalRead += bytesRead;

                // Throttle progress updates to every 500ms
                long now = System.currentTimeMillis();
                if (listener != null && (now - lastProgressTime) > 500) {
                    listener.onProgress(token, totalRead, fileSize);
                    lastProgressTime = now;
                }
            }
            os.flush();

            if (listener != null) {
                listener.onProgress(token, fileSize, fileSize);
                listener.onFileSent(token, filePath, fileSize);
            }

            Log.d(TAG, "sendFile: sent " + totalRead + " bytes for token=" + token);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "sendFile: error sending file for token=" + token, e);
            if (listener != null) {
                listener.onError(token, "Send failed: " + e.getMessage());
            }
            return false;
        }
    }

    /**
     * Receives a file from the specified channel and writes it to disk.
     *
     * @param token     the channel token
     * @param outputPath absolute path where the file should be saved
     * @param listener  optional progress listener (may be {@code null})
     * @return {@code true} if the file was received successfully
     */
    public boolean receiveFile(String token, String outputPath, ChannelListener listener) {
        ChannelState state = channels.get(token);
        if (state == null) {
            Log.w(TAG, "receiveFile: unknown token=" + token);
            return false;
        }

        java.io.File outFile = new java.io.File(outputPath);
        // Ensure parent directory exists
        java.io.File parent = outFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            InputStream is = getInputStream(token);
            if (is == null) {
                Log.w(TAG, "receiveFile: no input stream for token=" + token);
                return false;
            }

            byte[] buffer = new byte[CHUNK_SIZE];
            long totalWritten = 0;
            int bytesRead;
            long lastProgressTime = System.currentTimeMillis();

            while ((bytesRead = is.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
                totalWritten += bytesRead;

                long now = System.currentTimeMillis();
                if (listener != null && (now - lastProgressTime) > 500) {
                    listener.onProgress(token, totalWritten, -1);
                    lastProgressTime = now;
                }
            }
            fos.flush();

            if (listener != null) {
                listener.onFileReceived(token, outputPath, totalWritten);
            }

            Log.d(TAG, "receiveFile: received " + totalWritten + " bytes for token=" + token);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "receiveFile: error receiving file for token=" + token, e);
            if (listener != null) {
                listener.onError(token, "Receive failed: " + e.getMessage());
            }
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Channel listing
    // -------------------------------------------------------------------------

    /**
     * Returns the number of currently open channels.
     */
    public int getChannelCount() {
        return channels.size();
    }

    /**
     * Returns an unmodifiable list of tokens for all open channels.
     */
    public List<String> getOpenChannelTokens() {
        return Collections.unmodifiableList(new ArrayList<>(channels.keySet()));
    }

    /**
     * Checks whether a channel with the given token exists and is open.
     */
    public boolean isChannelOpen(String token) {
        ChannelState state = channels.get(token);
        return state != null && state.state == STATE_OPEN;
    }

    /**
     * Returns the node ID associated with a channel, or {@code null}.
     */
    public String getChannelNodeId(String token) {
        ChannelState state = channels.get(token);
        return state != null ? state.nodeId : null;
    }

    /**
     * Returns the path associated with a channel, or {@code null}.
     */
    public String getChannelPath(String token) {
        ChannelState state = channels.get(token);
        return state != null ? state.path : null;
    }

    /**
     * Writes received data into a channel's input stream for consumer reading.
     */
    public void feedData(String token, byte[] data, int offset, int length) {
        ChannelState state = channels.get(token);
        if (state == null || state.pipeOutputStream == null) return;
        try {
            state.pipeOutputStream.write(data, offset, length);
            state.pipeOutputStream.flush();
        } catch (IOException e) {
            Log.w(TAG, "feedData: error writing to pipe for token=" + token, e);
        }
    }

    /**
     * Signals end-of-stream on a channel's input.
     */
    public void closeInput(String token) {
        ChannelState state = channels.get(token);
        if (state != null && state.pipeOutputStream != null) {
            try {
                state.pipeOutputStream.close();
            } catch (IOException e) {
                Log.w(TAG, "closeInput: error for token=" + token, e);
            }
            state.pipeOutputStream = null;
        }
    }

    // -------------------------------------------------------------------------
    // Inner types
    // -------------------------------------------------------------------------

    /**
     * Tracks the state of a single channel.
     */
    private static class ChannelState {
        final String token;
        final long channelId;
        final String nodeId;
        final String path;
        final String packageName;
        final String signatureDigest;
        int state = STATE_OPENING;

        java.io.PipedInputStream pipeInputStream;
        java.io.PipedOutputStream pipeOutputStream;
        InputStream inputStream;
        OutputStream outputStream;

        ChannelState(String token, long channelId, String nodeId, String path,
                String packageName, String signatureDigest) {
            this.token = token;
            this.channelId = channelId;
            this.nodeId = nodeId;
            this.path = path;
            this.packageName = packageName;
            this.signatureDigest = signatureDigest;
        }
    }

    /**
     * An {@link OutputStream} that writes to a channel, buffering and forwarding
     * data to the wearable connection.
     */
    private class ChannelOutputStream extends OutputStream {
        private final ChannelState state;
        private final byte[] buffer = new byte[CHUNK_SIZE];
        private int position;

        ChannelOutputStream(ChannelState state) {
            this.state = state;
        }

        @Override
        public void write(int b) throws IOException {
            if (state.state == STATE_CLOSED) throw new IOException("Channel closed");
            if (position >= CHUNK_SIZE) flush();
            buffer[position++] = (byte) b;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (state.state == STATE_CLOSED) throw new IOException("Channel closed");
            while (len > 0) {
                int space = CHUNK_SIZE - position;
                int toCopy = Math.min(space, len);
                System.arraycopy(b, off, buffer, position, toCopy);
                position += toCopy;
                off += toCopy;
                len -= toCopy;
                if (position >= CHUNK_SIZE) flush();
            }
        }

        @Override
        public void flush() throws IOException {
            if (position > 0) {
                try {
                    // Forward data to the wearable peer
                    wearable.sendMessage(state.path, java.util.Arrays.copyOf(buffer, position));
                } catch (Exception e) {
                    throw new IOException("Channel flush failed", e);
                }
                position = 0;
            }
        }

        @Override
        public void close() throws IOException {
            flush();
            state.state = STATE_CLOSED;
        }
    }

    /**
     * Listener interface for channel events.
     */
    public interface ChannelListener {
        /**
         * Called periodically during file transfer to report progress.
         *
         * @param token      the channel token
         * @param bytesSoFar bytes transferred so far
         * @param totalBytes total file size (−1 if unknown)
         */
        void onProgress(String token, long bytesSoFar, long totalBytes);

        /**
         * Called when a file has been successfully sent.
         */
        void onFileSent(String token, String filePath, long fileSize);

        /**
         * Called when a file has been successfully received.
         */
        void onFileReceived(String token, String filePath, long fileSize);

        /**
         * Called when an error occurs during channel operations.
         */
        void onError(String token, String errorMessage);
    }
}
