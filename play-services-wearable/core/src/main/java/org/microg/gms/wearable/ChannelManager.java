/*
 * Copyright (C) 2026 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.microg.gms.wearable;

import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.google.android.gms.wearable.internal.ChannelEventParcelable;
import com.google.android.gms.wearable.internal.ChannelParcelable;

import org.microg.wearable.proto.ChannelControlRequest;
import org.microg.wearable.proto.ChannelDataAckRequest;
import org.microg.wearable.proto.ChannelDataHeader;
import org.microg.wearable.proto.ChannelDataRequest;
import org.microg.wearable.proto.ChannelRequest;
import org.microg.wearable.proto.Request;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import okio.ByteString;

/**
 * Manages the lifecycle and I/O for Wearable Channel API channels.
 * <p>
 * Uses the {@link ChannelControlRequest}, {@link ChannelDataRequest}, and
 * {@link ChannelDataAckRequest} protocol messages to open, transfer data through,
 * and close channels with peer WearOS nodes.
 */
public class ChannelManager {
    private static final String TAG = "GmsWearChannelMgr";

    /** ChannelControlRequest.type values */
    private static final int CONTROL_TYPE_OPEN = 1;
    private static final int CONTROL_TYPE_CLOSE = 2;

    /** ChannelEventParcelable.eventType values */
    private static final int EVENT_TYPE_OPENED = 1;
    private static final int EVENT_TYPE_CLOSED = 2;
    private static final int EVENT_TYPE_INPUT_CLOSED = 3;
    private static final int EVENT_TYPE_OUTPUT_CLOSED = 4;

    private static final int CHUNK_SIZE = 8192;

    /** Maps channelId (Long) to ChannelState */
    private final Map<Long, ChannelState> channels = new ConcurrentHashMap<>();
    /** Maps String token (= Long.toString(channelId)) to channelId for the public API */
    private final Map<String, Long> tokenToId = new ConcurrentHashMap<>();

    private final WearableImpl wearable;
    private final AtomicLong nextChannelId = new AtomicLong(1);

    public ChannelManager(WearableImpl wearable) {
        this.wearable = wearable;
    }

    // -------------------------------------------------------------------------
    // Public API called from WearableServiceImpl
    // -------------------------------------------------------------------------

    /**
     * Opens a new channel to the given node on the given path.
     * Sends a {@link ChannelControlRequest} OPEN message to the peer and returns a
     * {@link ChannelParcelable} token the caller can use for subsequent operations.
     *
     * @param targetNodeId    the peer node to open the channel to
     * @param path            the application-specific channel path
     * @param packageName     calling app's package name (included in the protocol message)
     * @param signatureDigest SHA-1 digest of the calling app's signing certificate
     * @return a {@link ChannelParcelable}, or {@code null} on failure
     */
    public ChannelParcelable openChannel(String targetNodeId, String path,
            String packageName, String signatureDigest) {
        if (!wearable.hasActiveConnection(targetNodeId)) {
            Log.w(TAG, "openChannel: node " + targetNodeId + " is not connected");
            return null;
        }

        long channelId = nextChannelId.getAndIncrement();
        String token = Long.toString(channelId);

        ChannelState state = new ChannelState(token, channelId, targetNodeId, path);
        channels.put(channelId, state);
        tokenToId.put(token, channelId);

        try {
            sendChannelControl(targetNodeId, new ChannelControlRequest.Builder()
                    .type(CONTROL_TYPE_OPEN)
                    .channelId(channelId)
                    .fromChannelOperator(true)
                    .packageName(packageName)
                    .signatureDigest(signatureDigest)
                    .path(path)
                    .build());
        } catch (IOException e) {
            Log.e(TAG, "openChannel: failed to send OPEN for channel " + channelId, e);
            channels.remove(channelId);
            tokenToId.remove(token);
            return null;
        }

        Log.d(TAG, "Opened channel: token=" + token + ", node=" + targetNodeId + ", path=" + path);
        return new ChannelParcelable(token, targetNodeId, path);
    }

    /**
     * Closes the channel identified by {@code token}.
     * Sends a {@link ChannelControlRequest} CLOSE message to the peer and notifies
     * registered listeners with a {@link ChannelEventParcelable} CLOSED event.
     *
     * @param token     the channel token returned from {@link #openChannel}
     * @param errorCode 0 for a normal close, non-zero for an error close
     * @return {@code true} if the channel was found and closed
     */
    public boolean closeChannel(String token, int errorCode) {
        Long channelId = tokenToId.remove(token);
        if (channelId == null) {
            Log.w(TAG, "closeChannel: unknown token " + token);
            return false;
        }
        ChannelState state = channels.remove(channelId);
        if (state == null) {
            Log.w(TAG, "closeChannel: no state for channelId " + channelId);
            return false;
        }

        try {
            sendChannelControl(state.nodeId, new ChannelControlRequest.Builder()
                    .type(CONTROL_TYPE_CLOSE)
                    .channelId(channelId)
                    .fromChannelOperator(true)
                    .closeErrorCode(errorCode)
                    .build());
        } catch (IOException e) {
            Log.w(TAG, "closeChannel: failed to send CLOSE for channel " + channelId, e);
        }

        state.close();
        dispatchChannelEvent(state, EVENT_TYPE_CLOSED, errorCode, 0);
        Log.d(TAG, "Closed channel: token=" + token + ", errorCode=" + errorCode);
        return true;
    }

    /**
     * Returns the read-end {@link ParcelFileDescriptor} for the channel's input stream.
     * The caller reads data from it; incoming data from the peer is written to the
     * write-end by {@link #handleIncomingData}.
     *
     * @param token the channel token
     * @return the read-end PFD, or {@code null} if the channel does not exist
     */
    public ParcelFileDescriptor getInputStream(String token) {
        ChannelState state = stateForToken(token);
        if (state == null) {
            Log.w(TAG, "getInputStream: unknown channel " + token);
            return null;
        }
        try {
            if (state.inputPipe == null) {
                state.inputPipe = ParcelFileDescriptor.createPipe();
            }
            return state.inputPipe[0]; // read end
        } catch (IOException e) {
            Log.e(TAG, "getInputStream: failed to create pipe for channel " + token, e);
            return null;
        }
    }

    /**
     * Returns the write-end {@link ParcelFileDescriptor} for the channel's output stream
     * and starts a background forwarder thread that reads from the pipe and sends
     * {@link ChannelDataRequest} messages to the peer.
     *
     * @param token the channel token
     * @return the write-end PFD, or {@code null} if the channel does not exist
     */
    public ParcelFileDescriptor getOutputStream(String token) {
        ChannelState state = stateForToken(token);
        if (state == null) {
            Log.w(TAG, "getOutputStream: unknown channel " + token);
            return null;
        }
        try {
            if (state.outputPipe == null) {
                state.outputPipe = ParcelFileDescriptor.createPipe();
                startOutputForwarder(state);
            }
            return state.outputPipe[1]; // write end for caller
        } catch (IOException e) {
            Log.e(TAG, "getOutputStream: failed to create pipe for channel " + token, e);
            return null;
        }
    }

    /**
     * Pipes data from the channel's input stream into the given file descriptor.
     * A background thread copies data written to the input pipe (by the peer) into {@code fd}.
     *
     * @param token the channel token
     * @param fd    the destination file descriptor
     * @return {@code true} if the background copy was started
     */
    public boolean writeInputToFd(String token, ParcelFileDescriptor fd) {
        ChannelState state = stateForToken(token);
        if (state == null) {
            Log.w(TAG, "writeInputToFd: unknown channel " + token);
            return false;
        }
        try {
            if (state.inputPipe == null) {
                state.inputPipe = ParcelFileDescriptor.createPipe();
            }
            final ParcelFileDescriptor readEnd = state.inputPipe[0];
            new Thread(() -> {
                try (InputStream in = new ParcelFileDescriptor.AutoCloseInputStream(readEnd);
                     OutputStream out = new ParcelFileDescriptor.AutoCloseOutputStream(fd)) {
                    byte[] buf = new byte[CHUNK_SIZE];
                    int n;
                    while ((n = in.read(buf)) != -1) {
                        out.write(buf, 0, n);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "writeInputToFd error for channel " + token, e);
                }
            }, "WearChanIn-" + token).start();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "writeInputToFd: failed to create pipe", e);
            return false;
        }
    }

    /**
     * Reads from the given file descriptor (with optional offset/length) and sends the
     * data through the channel's output stream to the peer.
     *
     * @param token       the channel token
     * @param fd          the source file descriptor
     * @param startOffset byte offset to start reading from (0 = beginning)
     * @param length      maximum bytes to send (-1 or 0 = until EOF)
     * @return {@code true} if the background send was started
     */
    public boolean readOutputFromFd(String token, ParcelFileDescriptor fd,
            long startOffset, long length) {
        ChannelState state = stateForToken(token);
        if (state == null) {
            Log.w(TAG, "readOutputFromFd: unknown channel " + token);
            return false;
        }
        try {
            if (state.outputPipe == null) {
                state.outputPipe = ParcelFileDescriptor.createPipe();
                startOutputForwarder(state);
            }
            final ParcelFileDescriptor writeEnd = state.outputPipe[1];
            new Thread(() -> {
                try (InputStream in = new ParcelFileDescriptor.AutoCloseInputStream(fd);
                     OutputStream out = new ParcelFileDescriptor.AutoCloseOutputStream(writeEnd)) {
                    if (startOffset > 0) {
                        long skipped = in.skip(startOffset);
                        if (skipped < startOffset) {
                            Log.w(TAG, "readOutputFromFd: only skipped " + skipped + "/" + startOffset);
                        }
                    }
                    byte[] buf = new byte[CHUNK_SIZE];
                    long total = 0;
                    int n;
                    while ((n = in.read(buf)) != -1) {
                        if (length > 0 && total + n > length) {
                            out.write(buf, 0, (int) (length - total));
                            break;
                        }
                        out.write(buf, 0, n);
                        total += n;
                    }
                } catch (IOException e) {
                    Log.e(TAG, "readOutputFromFd error for channel " + token, e);
                }
            }, "WearChanOut-" + token).start();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "readOutputFromFd: failed to create pipe", e);
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Incoming message handling (called from MessageHandler)
    // -------------------------------------------------------------------------

    /**
     * Dispatches an incoming {@link Request} from the peer that was received on the
     * {@code channelRequest} field of a {@link org.microg.wearable.proto.RootMessage}.
     * <p>
     * Handles channel open, close, data, and data-ack sub-messages.
     *
     * @param sourceNodeId the peer node that sent the message
     * @param request      the channel request proto
     */
    public void handleIncomingChannelMessage(String sourceNodeId, Request request) {
        if (request == null || request.request == null) {
            Log.w(TAG, "handleIncomingChannelMessage: null or empty request from " + sourceNodeId);
            return;
        }
        ChannelRequest cr = request.request;
        if (cr.channelControlRequest != null) {
            handleIncomingControl(sourceNodeId, request.path, cr.channelControlRequest);
        } else if (cr.channelDataRequest != null) {
            handleIncomingData(cr.channelDataRequest);
        } else if (cr.channelDataAckRequest != null) {
            handleIncomingDataAck(cr.channelDataAckRequest);
        } else {
            Log.w(TAG, "handleIncomingChannelMessage: unrecognised sub-message from " + sourceNodeId);
        }
    }

    /**
     * Legacy entry point preserved for callers that do not yet pass the full
     * {@link Request}. Treats every call as a new channel-open request from the peer.
     *
     * @deprecated Use {@link #handleIncomingChannelMessage(String, Request)} instead.
     */
    @Deprecated
    public void handleIncomingChannelRequest(String sourceNodeId, String path, String token) {
        long channelId = nextChannelId.getAndIncrement();
        String newToken = Long.toString(channelId);
        ChannelState state = new ChannelState(newToken, channelId, sourceNodeId, path);
        channels.put(channelId, state);
        tokenToId.put(newToken, channelId);
        Log.d(TAG, "Accepted incoming channel (legacy) from " + sourceNodeId
                + ": path=" + path + ", token=" + newToken);
        dispatchChannelEvent(state, EVENT_TYPE_OPENED, 0, 0);
    }

    /**
     * Returns {@code true} if the given token corresponds to an open channel.
     */
    public boolean isChannelOpen(String token) {
        Long id = tokenToId.get(token);
        return id != null && channels.containsKey(id);
    }

    /**
     * Closes all open channels. Called during service shutdown.
     */
    public void closeAll() {
        for (ChannelState state : channels.values()) {
            state.close();
        }
        channels.clear();
        tokenToId.clear();
        Log.d(TAG, "All channels closed");
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private ChannelState stateForToken(String token) {
        Long id = tokenToId.get(token);
        return id != null ? channels.get(id) : null;
    }

    private void handleIncomingControl(String sourceNodeId, String requestPath,
            ChannelControlRequest ctrl) {
        int type = ctrl.type != null ? ctrl.type : 0;
        long channelId = ctrl.channelId != null ? ctrl.channelId : -1L;
        String path = ctrl.path != null ? ctrl.path : requestPath;

        if (channelId <= 0) {
            // Channel IDs start at 1 (locally) and must be positive; 0 and negative values
            // indicate a missing/unset field in the proto and should be ignored.
            Log.w(TAG, "handleIncomingControl: invalid channelId=" + channelId
                    + ", type=" + type + ", ignoring");
            return;
        }

        if (type == CONTROL_TYPE_OPEN) {
            // Advance our local counter past any peer-assigned id to prevent collisions.
            // Use a CAS loop instead of updateAndGet (which requires API 24) for API 19 compat.
            long minId = channelId + 1;
            long current;
            do {
                current = nextChannelId.get();
                if (current >= minId) break;
            } while (!nextChannelId.compareAndSet(current, minId));
            String token = Long.toString(channelId);
            ChannelState state = new ChannelState(token, channelId, sourceNodeId, path);
            channels.put(channelId, state);
            tokenToId.put(token, channelId);
            Log.d(TAG, "Peer opened channel: id=" + channelId + ", path=" + path
                    + ", from=" + sourceNodeId);
            dispatchChannelEvent(state, EVENT_TYPE_OPENED, 0, 0);

        } else if (type == CONTROL_TYPE_CLOSE) {
            String token = Long.toString(channelId);
            tokenToId.remove(token);
            ChannelState state = channels.remove(channelId);
            if (state != null) {
                int errorCode = ctrl.closeErrorCode != null ? ctrl.closeErrorCode : 0;
                state.close();
                dispatchChannelEvent(state, EVENT_TYPE_CLOSED, errorCode, 0);
                Log.d(TAG, "Peer closed channel: id=" + channelId + ", errorCode=" + errorCode);
            }
        } else {
            Log.w(TAG, "handleIncomingControl: unknown type=" + type);
        }
    }

    private void handleIncomingData(ChannelDataRequest data) {
        if (data.header == null) {
            Log.w(TAG, "handleIncomingData: null header");
            return;
        }
        long channelId = data.header.channelId != null ? data.header.channelId : -1L;
        ChannelState state = channels.get(channelId);
        if (state == null) {
            Log.w(TAG, "handleIncomingData: unknown channelId " + channelId);
            return;
        }
        try {
            if (state.inputPipe == null) {
                state.inputPipe = ParcelFileDescriptor.createPipe();
                // Open a single OutputStream over the write-end PFD and keep it alive
                // across all chunks. Wrapping the PFD rather than its raw FileDescriptor
                // ensures the FD is NOT closed when the stream would otherwise be closed.
                state.inputPipeWriter = new ParcelFileDescriptor.AutoCloseOutputStream(
                        state.inputPipe[1]);
            }
            if (data.payload != null && data.payload.size() > 0) {
                try {
                    state.inputPipeWriter.write(data.payload.toByteArray());
                } catch (IOException e) {
                    // Reset on write error so the next message re-creates the pipe
                    try { state.inputPipeWriter.close(); } catch (IOException ignored) { }
                    state.inputPipeWriter = null;
                    state.inputPipe = null;
                    Log.e(TAG, "handleIncomingData: write error for channel " + channelId, e);
                    return;
                }
            }
            if (Boolean.TRUE.equals(data.finalMessage)) {
                // Peer has finished sending; close the write-end to signal EOF to the reader.
                // The read-end (inputPipe[0]) is kept alive so the app can drain remaining data;
                // it will be released when the channel itself is closed via state.close().
                if (state.inputPipeWriter != null) {
                    try { state.inputPipeWriter.close(); } catch (IOException ignored) { }
                    state.inputPipeWriter = null;
                }
                if (state.inputPipe != null) {
                    state.inputPipe[1] = null; // write-end closed by inputPipeWriter above
                }
                dispatchChannelEvent(state, EVENT_TYPE_INPUT_CLOSED, 0, 0);
            }
        } catch (IOException e) {
            Log.e(TAG, "handleIncomingData: pipe creation failed for channel " + channelId, e);
        }
    }

    private void handleIncomingDataAck(ChannelDataAckRequest ack) {
        if (ack.header == null) return;
        long channelId = ack.header.channelId != null ? ack.header.channelId : -1L;
        Log.d(TAG, "handleIncomingDataAck: channelId=" + channelId
                + ", final=" + ack.finalMessage);
        if (Boolean.TRUE.equals(ack.finalMessage)) {
            ChannelState state = channels.get(channelId);
            if (state != null) {
                dispatchChannelEvent(state, EVENT_TYPE_OUTPUT_CLOSED, 0, 0);
            }
        }
    }

    /**
     * Starts a background thread that reads from {@code state.outputPipe[0]} and
     * forwards data to the peer as {@link ChannelDataRequest} messages.
     * <p>
     * Precondition: {@code state.outputPipe} must already be initialised.
     */
    private void startOutputForwarder(ChannelState state) {
        final ParcelFileDescriptor readEnd = state.outputPipe[0];
        new Thread(() -> {
            try (InputStream in = new ParcelFileDescriptor.AutoCloseInputStream(readEnd)) {
                byte[] buf = new byte[CHUNK_SIZE];
                int n;
                long requestId = 0;
                while ((n = in.read(buf)) != -1) {
                    byte[] chunk = new byte[n];
                    System.arraycopy(buf, 0, chunk, 0, n);
                    ChannelDataRequest dataMsg = new ChannelDataRequest.Builder()
                            .header(new ChannelDataHeader.Builder()
                                    .channelId(state.channelId)
                                    .fromChannelOperator(true)
                                    .requestId(requestId++)
                                    .build())
                            .payload(ByteString.of(chunk))
                            .finalMessage(false)
                            .build();
                    try {
                        wearable.sendChannelMessage(state.nodeId,
                                new Request.Builder()
                                        .path(state.path)
                                        .sourceNodeId(wearable.getLocalNodeId())
                                        .targetNodeId(state.nodeId)
                                        .request(new ChannelRequest.Builder()
                                                .channelDataRequest(dataMsg)
                                                .build())
                                        .build());
                    } catch (IOException e) {
                        Log.e(TAG, "Output forwarder: send error for channel "
                                + state.channelId, e);
                        break;
                    }
                }
                // EOF — send a final message to signal the peer that the stream is done
                try {
                    wearable.sendChannelMessage(state.nodeId,
                            new Request.Builder()
                                    .path(state.path)
                                    .sourceNodeId(wearable.getLocalNodeId())
                                    .targetNodeId(state.nodeId)
                                    .request(new ChannelRequest.Builder()
                                            .channelDataRequest(new ChannelDataRequest.Builder()
                                                    .header(new ChannelDataHeader.Builder()
                                                            .channelId(state.channelId)
                                                            .fromChannelOperator(true)
                                                            .requestId(-1L)
                                                            .build())
                                                    .payload(ByteString.EMPTY)
                                                    .finalMessage(true)
                                                    .build())
                                            .build())
                                    .build());
                } catch (IOException e) {
                    Log.w(TAG, "Output forwarder: failed to send final for channel "
                            + state.channelId, e);
                }
                dispatchChannelEvent(state, EVENT_TYPE_OUTPUT_CLOSED, 0, 0);
            } catch (IOException e) {
                Log.e(TAG, "Output forwarder: pipe read error for channel "
                        + state.channelId, e);
            }
        }, "WearChanFwd-" + state.channelId).start();
    }

    private void sendChannelControl(String targetNodeId, ChannelControlRequest ctrl)
            throws IOException {
        wearable.sendChannelMessage(targetNodeId,
                new Request.Builder()
                        .path(ctrl.path != null ? ctrl.path : "")
                        .sourceNodeId(wearable.getLocalNodeId())
                        .targetNodeId(targetNodeId)
                        .request(new ChannelRequest.Builder()
                                .channelControlRequest(ctrl)
                                .build())
                        .build());
    }

    private void dispatchChannelEvent(ChannelState state, int eventType,
            int closeReason, int appSpecificErrorCode) {
        ChannelEventParcelable event = new ChannelEventParcelable();
        event.channel = new ChannelParcelable(state.token, state.nodeId, state.path);
        event.eventType = eventType;
        event.closeReason = closeReason;
        event.appSpecificErrorCode = appSpecificErrorCode;
        wearable.invokeChannelEvent(event);
    }

    // -------------------------------------------------------------------------
    // ChannelState
    // -------------------------------------------------------------------------

    private static class ChannelState {
        final String token;
        final long channelId;
        final String nodeId;
        final String path;
        ParcelFileDescriptor[] inputPipe;  // [0]=read end (given to app), [1]=write end (filled by network)
        ParcelFileDescriptor[] outputPipe; // [0]=read end (read by forwarder), [1]=write end (given to app)
        /** Kept open across chunks so the write-end FD is never closed between messages. */
        OutputStream inputPipeWriter;

        ChannelState(String token, long channelId, String nodeId, String path) {
            this.token = token;
            this.channelId = channelId;
            this.nodeId = nodeId;
            this.path = path;
        }

        void close() {
            if (inputPipeWriter != null) {
                try { inputPipeWriter.close(); } catch (IOException ignored) { }
                inputPipeWriter = null;
            }
            closePipe(inputPipe);
            closePipe(outputPipe);
            inputPipe = null;
            outputPipe = null;
        }

        private static void closePipe(ParcelFileDescriptor[] pipe) {
            if (pipe == null) return;
            for (ParcelFileDescriptor fd : pipe) {
                if (fd != null) {
                    try { fd.close(); } catch (IOException ignored) { }
                }
            }
        }
    }
}
