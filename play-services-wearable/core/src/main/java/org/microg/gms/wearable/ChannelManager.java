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

import com.google.android.gms.wearable.internal.ChannelParcelable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the lifecycle and I/O for Wearable Channel API channels.
 * <p>
 * Each channel is identified by a unique token and tracks its associated node,
 * path, and pipe-based file descriptors for bidirectional streaming.
 */
public class ChannelManager {
    private static final String TAG = "GmsWearChannelMgr";

    private final Map<String, ChannelState> channels = new ConcurrentHashMap<>();
    private final WearableImpl wearable;

    public ChannelManager(WearableImpl wearable) {
        this.wearable = wearable;
    }

    /**
     * Opens a new channel to the given node on the given path.
     *
     * @param targetNodeId the node to open the channel to
     * @param path         the application-specific path for the channel
     * @return a ChannelParcelable representing the opened channel, or null on failure
     */
    public ChannelParcelable openChannel(String targetNodeId, String path) {
        if (!wearable.getAllConnectedNodes().contains(targetNodeId)) {
            Log.w(TAG, "Cannot open channel: node " + targetNodeId + " is not connected");
            return null;
        }

        String token = UUID.randomUUID().toString();
        ChannelState state = new ChannelState(token, targetNodeId, path);
        channels.put(token, state);

        Log.d(TAG, "Opened channel: token=" + token + ", node=" + targetNodeId + ", path=" + path);
        return new ChannelParcelable(token, targetNodeId, path);
    }

    /**
     * Closes the channel identified by the given token.
     *
     * @param token    the channel token
     * @param errorCode error code (0 for normal close)
     * @return true if the channel was found and closed
     */
    public boolean closeChannel(String token, int errorCode) {
        ChannelState state = channels.remove(token);
        if (state == null) {
            Log.w(TAG, "Cannot close channel: unknown token " + token);
            return false;
        }
        state.close();
        Log.d(TAG, "Closed channel: token=" + token + ", errorCode=" + errorCode);
        return true;
    }

    /**
     * Gets a ParcelFileDescriptor for reading data from the channel (input stream).
     * Creates a pipe pair; the caller reads from the returned PFD, and data received
     * from the peer is written to the other end.
     *
     * @param token the channel token
     * @return the read-end ParcelFileDescriptor, or null if the channel doesn't exist
     */
    public ParcelFileDescriptor getInputStream(String token) {
        ChannelState state = channels.get(token);
        if (state == null) {
            Log.w(TAG, "getInputStream: unknown channel " + token);
            return null;
        }
        try {
            if (state.inputPipe == null) {
                state.inputPipe = ParcelFileDescriptor.createPipe();
            }
            // Return the read end (index 0) to the caller
            return state.inputPipe[0];
        } catch (IOException e) {
            Log.e(TAG, "Failed to create input pipe for channel " + token, e);
            return null;
        }
    }

    /**
     * Gets a ParcelFileDescriptor for writing data to the channel (output stream).
     * Creates a pipe pair; the caller writes to the returned PFD, and data is sent
     * to the peer from the other end.
     *
     * @param token the channel token
     * @return the write-end ParcelFileDescriptor, or null if the channel doesn't exist
     */
    public ParcelFileDescriptor getOutputStream(String token) {
        ChannelState state = channels.get(token);
        if (state == null) {
            Log.w(TAG, "getOutputStream: unknown channel " + token);
            return null;
        }
        try {
            if (state.outputPipe == null) {
                state.outputPipe = ParcelFileDescriptor.createPipe();
            }
            // Return the write end (index 1) to the caller
            return state.outputPipe[1];
        } catch (IOException e) {
            Log.e(TAG, "Failed to create output pipe for channel " + token, e);
            return null;
        }
    }

    /**
     * Writes data received from the channel's incoming stream to the given file descriptor.
     * This bridges the channel's pipe to an external FD for file-based receive operations.
     *
     * @param token the channel token
     * @param fd    the target file descriptor to write to
     * @return true if the operation was initiated
     */
    public boolean writeInputToFd(String token, ParcelFileDescriptor fd) {
        ChannelState state = channels.get(token);
        if (state == null) {
            Log.w(TAG, "writeInputToFd: unknown channel " + token);
            return false;
        }
        try {
            if (state.inputPipe == null) {
                state.inputPipe = ParcelFileDescriptor.createPipe();
            }
            // Copy from the read end of the input pipe to the given FD in a background thread
            ParcelFileDescriptor readEnd = state.inputPipe[0];
            new Thread(() -> {
                try (InputStream in = new ParcelFileDescriptor.AutoCloseInputStream(readEnd);
                     OutputStream out = new ParcelFileDescriptor.AutoCloseOutputStream(fd)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error in writeInputToFd for channel " + token, e);
                }
            }, "ChannelInputToFd-" + token).start();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Failed to create input pipe for writeInputToFd", e);
            return false;
        }
    }

    /**
     * Reads data from the given file descriptor and sends it through the channel's output stream.
     * This bridges an external FD to the channel's pipe for file-based send operations.
     *
     * @param token       the channel token
     * @param fd          the source file descriptor to read from
     * @param startOffset the offset to start reading from (-1 for beginning)
     * @param length      the number of bytes to read (-1 for entire file)
     * @return true if the operation was initiated
     */
    public boolean readOutputFromFd(String token, ParcelFileDescriptor fd, long startOffset, long length) {
        ChannelState state = channels.get(token);
        if (state == null) {
            Log.w(TAG, "readOutputFromFd: unknown channel " + token);
            return false;
        }
        try {
            if (state.outputPipe == null) {
                state.outputPipe = ParcelFileDescriptor.createPipe();
            }
            // Copy from the given FD to the write end of the output pipe in a background thread
            ParcelFileDescriptor writeEnd = state.outputPipe[1];
            new Thread(() -> {
                try (InputStream in = new ParcelFileDescriptor.AutoCloseInputStream(fd);
                     OutputStream out = new ParcelFileDescriptor.AutoCloseOutputStream(writeEnd)) {
                    if (startOffset > 0) {
                        long skipped = in.skip(startOffset);
                        if (skipped < startOffset) {
                            Log.w(TAG, "Could only skip " + skipped + "/" + startOffset + " bytes");
                        }
                    }
                    byte[] buffer = new byte[8192];
                    long totalRead = 0;
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        if (length > 0 && totalRead + bytesRead > length) {
                            out.write(buffer, 0, (int) (length - totalRead));
                            break;
                        }
                        out.write(buffer, 0, bytesRead);
                        totalRead += bytesRead;
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error in readOutputFromFd for channel " + token, e);
                }
            }, "ChannelOutputFromFd-" + token).start();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Failed to create output pipe for readOutputFromFd", e);
            return false;
        }
    }

    /**
     * Handles an incoming channel request from a peer node.
     * Called by MessageHandler when the peer sends a channel-related message.
     *
     * @param sourceNodeId the node that sent the request
     * @param path         the channel path
     * @param token        the channel token (may be null for new channel open requests)
     */
    public void handleIncomingChannelRequest(String sourceNodeId, String path, String token) {
        if (token == null) {
            // Peer is opening a channel to us
            token = UUID.randomUUID().toString();
            ChannelState state = new ChannelState(token, sourceNodeId, path);
            channels.put(token, state);
            Log.d(TAG, "Accepted incoming channel from " + sourceNodeId + ": path=" + path + ", token=" + token);
        }
        // TODO: Notify listeners about the channel event
    }

    /**
     * Returns true if the given token corresponds to an open channel.
     */
    public boolean isChannelOpen(String token) {
        return channels.containsKey(token);
    }

    /**
     * Closes all open channels. Called during service shutdown.
     */
    public void closeAll() {
        for (Map.Entry<String, ChannelState> entry : channels.entrySet()) {
            entry.getValue().close();
        }
        channels.clear();
        Log.d(TAG, "All channels closed");
    }

    /**
     * Tracks the state of a single open channel, including its pipe file descriptors.
     */
    private static class ChannelState {
        final String token;
        final String nodeId;
        final String path;
        ParcelFileDescriptor[] inputPipe;  // [0]=read, [1]=write
        ParcelFileDescriptor[] outputPipe; // [0]=read, [1]=write

        ChannelState(String token, String nodeId, String path) {
            this.token = token;
            this.nodeId = nodeId;
            this.path = path;
        }

        void close() {
            closePipe(inputPipe);
            closePipe(outputPipe);
            inputPipe = null;
            outputPipe = null;
        }

        private static void closePipe(ParcelFileDescriptor[] pipe) {
            if (pipe != null) {
                try {
                    if (pipe[0] != null) pipe[0].close();
                } catch (IOException ignored) {
                }
                try {
                    if (pipe[1] != null) pipe[1].close();
                } catch (IOException ignored) {
                }
            }
        }
    }
}
