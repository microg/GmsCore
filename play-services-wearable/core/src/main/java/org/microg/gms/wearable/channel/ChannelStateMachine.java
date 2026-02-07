package org.microg.gms.wearable.channel;

import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.wearable.internal.IChannelStreamCallbacks;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class ChannelStateMachine {
    private static final String TAG = "ChannelStateMachine";

    private static final int DEFAULT_CHUNK_SIZE = 8192;
    private static final int MAX_CHUNK_SIZE = 65536;
    private static final int RECEIVE_BUFFER_SIZE = 65536;

    public static final int CONNECTION_STATE_NOT_STARTED = 0;
    public static final int CONNECTION_STATE_OPEN_SENT = 1;
    public static final int CONNECTION_STATE_ESTABLISHED = 2;
    public static final int CONNECTION_STATE_CLOSING = 3;
    public static final int CONNECTION_STATE_CLOSED = 4;

    public static final int SENDING_STATE_NOT_STARTED = 5;
    public static final int SENDING_STATE_WAITING_TO_READ = 6;
    public static final int SENDING_STATE_WAITING_FOR_ACK = 7;
    public static final int SENDING_STATE_CLOSED = 8;

    public static final int RECEIVING_STATE_WAITING_FOR_DATA = 9;
    public static final int RECEIVING_STATE_WAITING_TO_WRITE = 10;
    public static final int RECEIVING_STATE_CLOSED = 11;

    public final ChannelToken token;
    public final boolean isLocalOpener;
    public final ChannelTransport transport;

    private final ChannelManager channelManager;
    private final ChannelCallbacks callbacks;
    private final IBinder.DeathRecipient deathRecipient;
    private final Handler handler;

    public int connectionState = CONNECTION_STATE_NOT_STARTED;
    public int sendingState = SENDING_STATE_NOT_STARTED;
    public int receivingState = RECEIVING_STATE_WAITING_FOR_DATA;

    public String channelPath;

    public long lastAckedOffset = 0;
    public long sequenceNumber = 0;

    public ParcelFileDescriptor inputFd;
    public IChannelStreamCallbacks inputCallbacks;
    public ByteBuffer receiveBuffer;
    public boolean receivePending = false;

    public ParcelFileDescriptor outputFd;
    public IChannelStreamCallbacks outputCallbacks;
    public ByteBuffer sendBuffer;
    private long sendOffset;
    long sendMaxLength;

    public PendingOperation openTimeoutOp;
    public PendingOperation sendPendingOp;

    public int closeReason;

    public OpenChannelCallback openResultDispatcher;

    private final AtomicBoolean sendInProgress = new AtomicBoolean(false);
    private final Object stateLock = new Object();

    public final long creationTime;

    public long totalDataSize = -1;
    public long currentSendOffset = 0;
    public long totalBytesSent = 0;
    public long totalBytesReceived = 0;

    public ChannelStateMachine(
            ChannelToken token,
            ChannelManager channelManager,
            ChannelTransport transport,
            ChannelCallbacks callbacks,
            boolean isLocalOpener,
            IBinder.DeathRecipient deathRecipient,
            Handler handler) {

        this.token = token;
        this.channelManager = channelManager;
        this.transport = transport;
        this.callbacks = callbacks;
        this.isLocalOpener = isLocalOpener;
        this.deathRecipient = deathRecipient;
        this.handler = handler;
        this.creationTime = System.currentTimeMillis();
    }

    public boolean hasInputStream() {
        return inputFd != null;
    }

    public boolean hasOutputStream() {
        return outputFd != null;
    }

    private boolean isValidConnectionStateTransition(int from, int to) {
        switch (from) {
            case CONNECTION_STATE_NOT_STARTED:
                return to == CONNECTION_STATE_OPEN_SENT || to == CONNECTION_STATE_ESTABLISHED;
            case CONNECTION_STATE_OPEN_SENT:
                return to == CONNECTION_STATE_ESTABLISHED || to == CONNECTION_STATE_CLOSED;
            case CONNECTION_STATE_ESTABLISHED:
                return to == CONNECTION_STATE_CLOSING || to == CONNECTION_STATE_CLOSED;
            case CONNECTION_STATE_CLOSING:
                return to == CONNECTION_STATE_CLOSED;
            default:
                return false;
        }
    }

    public void setConnectionState(int newState) {
        synchronized (stateLock) {
            if (connectionState == newState) {
                Log.v(TAG, String.format("Channel(%s): Already in state %s",
                        token, getConnectionStateString(newState)));
                return;
            }

            if (newState == CONNECTION_STATE_CLOSED) {
                this.connectionState = newState;
                return;
            }

            if (!isValidConnectionStateTransition(connectionState, newState)) {
                Log.e(TAG, String.format("Channel(%s): Invalid state transition %s -> %s",
                        token, getConnectionStateString(connectionState),
                        getConnectionStateString(newState)));
                throw new IllegalStateException("Invalid channel state transition: " +
                        getConnectionStateString(connectionState) + " -> " +
                        getConnectionStateString(newState));
            }

            Log.v(TAG, String.format("Channel(%s): %s -> %s",
                    token, getConnectionStateString(connectionState),
                    getConnectionStateString(newState)));

            this.connectionState = newState;
        }
    }

    public void setSendingState(int newState) {
        Log.v(TAG, String.format("Channel(%s): Sender %s -> %s",
                token, getSendingStateString(sendingState),
                getSendingStateString(newState)));
        this.sendingState = newState;
    }

    public void setReceivingState(int newState) {
        Log.v(TAG, String.format("Channel(%s): Receiver %s -> %s",
                token, getReceivingStateString(receivingState),
                getReceivingStateString(newState)));
        this.receivingState = newState;
    }

    public void sendOpenRequest() throws IOException {
        synchronized (stateLock) {

            if (connectionState != CONNECTION_STATE_NOT_STARTED) {
                throw new IllegalStateException("Cannot send OPEN from state: " +
                        getConnectionStateString(connectionState));
            }

            Log.d(TAG, "Sending open request for channel: " + token);
            setConnectionState(CONNECTION_STATE_OPEN_SENT);
            channelManager.sendOpenRequest(this);
        }
    }

    public void sendOpenAck() throws IOException {
        synchronized (stateLock) {
            if (connectionState != CONNECTION_STATE_ESTABLISHED &&
                    connectionState != CONNECTION_STATE_OPEN_SENT) {
                Log.w(TAG, "Sending ACK from unexpected state: " +
                        getConnectionStateString(connectionState));
            }

            Log.d(TAG, "Sending open ACK for channel: " + token);
            channelManager.sendOpenAck(this);
        }
    }

    public void onChannelOpenAckReceived() throws ChannelException {
        synchronized (stateLock) {
            if (connectionState != CONNECTION_STATE_OPEN_SENT) {
                Log.w(TAG, "Received OPEN_ACK in wrong state: " + connectionState);
                throw new ChannelException(token, "Received OPEN_ACK in wrong state");
            }

            if (openResultDispatcher == null) {
                Log.w(TAG, "Bad state: CONNECTION_STATE_OPEN_SENT but no callbacks");
                throw new ChannelException(token, "No open callback set");
            }

            if (openTimeoutOp == null) {
                Log.w(TAG, "Bad state: CONNECTION_STATE_OPEN_SENT but no timeout operation");
                throw new ChannelException(token, "No timeout operation");
            }

            if (!openTimeoutOp.cancel()) {
                Log.i(TAG, "Received OPEN_ACK but request already timed out");
                return;
            }

            openTimeoutOp = null;
            try {
                openResultDispatcher.onResult(ChannelStatusCodes.SUCCESS, token, channelPath);
            } catch (Exception e) {
                Log.e(TAG, "Error in open result callback", e);
            } finally {
                openResultDispatcher = null;
            }

            setConnectionState(CONNECTION_STATE_ESTABLISHED);
        }
    }

    public void processOutgoingData() throws IOException {
        if (sendingState != SENDING_STATE_WAITING_TO_READ) {
            return;
        }

        if (outputFd == null) {
            Log.e(TAG, "SENDING_STATE_WAITING_TO_READ but no output FD");
            return;
        }

        if (!sendInProgress.compareAndSet(false, true)) {
            return;
        }

        try {
            if (sendBuffer == null) {
                sendBuffer = ByteBuffer.allocate(DEFAULT_CHUNK_SIZE);
                Log.d(TAG, "Created send buffer of size " + DEFAULT_CHUNK_SIZE);
            }

            if (sendOffset > 0) {
                skipBytes(sendOffset);
                sendOffset = 0;
            }

            sendBuffer.clear();

            if (sendMaxLength >= 0 && sendMaxLength < sendBuffer.capacity()) {
                sendBuffer.limit((int) sendMaxLength);
            }

            int bytesRead = transport.read(outputFd, sendBuffer.array(),
                    sendBuffer.position(), sendBuffer.remaining());

            if (bytesRead > 0) {
                sendBuffer.position(sendBuffer.position() + bytesRead);
            }

            sendBuffer.flip();

            if (sendMaxLength >= 0) {
                sendMaxLength -= sendBuffer.remaining();
            }

            byte[] data = new byte[sendBuffer.remaining()];
            sendBuffer.get(data);

            boolean isFinal = (bytesRead < 0) || (sendMaxLength == 0);

            long requestId = ++sequenceNumber;

            Log.d(TAG, String.format("Sending chunk: size=%d, isFinal=%b, seq=%d",
                    data.length, isFinal, requestId));

            if (channelManager.sendData(this, data, isFinal, requestId)) {
                setSendingState(SENDING_STATE_WAITING_FOR_ACK);

                sendPendingOp = new PendingOperation(handler,
                        () -> onSendTimeout(), 30000, "Send data chunk");
            } else {
                Log.e(TAG, "Failed to send data chunk");
                onChannelOutputClosed(ChannelStatusCodes.INTERNAL_ERROR, 0);
            }
            currentSendOffset += data.length;
            totalBytesSent += data.length;

            Log.d(TAG, String.format("Sent chunk: %d bytes, offset=%d/%d (%.1f%%)",
                    data.length,
                    currentSendOffset,
                    totalDataSize,
                    totalDataSize > 0 ? (currentSendOffset * 100.0 / totalDataSize) : 0));

        } finally {
            sendInProgress.set(false);
        }

    }

    private void skipBytes(long skip) throws IOException {
        byte[] temp = new byte[8192];
        long remaining = skip;

        while (remaining > 0) {
            int toRead = (int) Math.min(temp.length, remaining);
            int read = transport.read(outputFd, temp, 0, toRead);
            if (read < 0) {
                throw new IOException("EOF while skipping bytes");
            }
            remaining -= read;
        }
    }

    private void onSendTimeout() {
        Log.w(TAG, "Sending data timed out. Closing channel");
        sendPendingOp = null;

        try {
            onChannelOutputClosed(ChannelStatusCodes.CLOSE_REASON_REMOTE_CLOSE, 0);
            onChannelInputClosed(ChannelStatusCodes.CLOSE_REASON_REMOTE_CLOSE, 0);
            sendCloseRequest(0);
            channelManager.removeChannel(token);
        } catch (IOException e) {
            Log.e(TAG, "Error handling send timeout", e);
        }
    }

    public void onDataAckReceived(long ackOffset, boolean isFinal) {
        if (sendingState != SENDING_STATE_WAITING_FOR_ACK) {
            Log.w(TAG, "Received ACK but not waiting for it");
            return;
        }

        if (sendPendingOp != null) {
            sendPendingOp.cancel();
            sendPendingOp = null;
        }

        lastAckedOffset = ackOffset;

        if (isFinal) {
            Log.d(TAG, "Received final ACK, closing output");
            setSendingState(SENDING_STATE_CLOSED);

            try {
                onChannelOutputClosed(ChannelStatusCodes.CLOSE_REASON_NORMAL, 0);
            } catch (IOException e) {
                Log.e(TAG, "Error closing output after final ACK", e);
            }
        } else {
            setSendingState(SENDING_STATE_WAITING_TO_READ);
        }
    }

    public void onDataReceived(byte[] data, boolean isFinal, long offset) throws ChannelException {
        synchronized (stateLock) {
            if (connectionState != CONNECTION_STATE_ESTABLISHED &&
                    connectionState != CONNECTION_STATE_CLOSING) {
                throw new ChannelException(token, "Received data in wrong connection state: " +
                        getConnectionStateString(connectionState));
            }
        }

        switch (receivingState) {
            case RECEIVING_STATE_WAITING_FOR_DATA:
                if (offset != lastAckedOffset) {
                    Log.w(TAG, "Received data with wrong offset: expected=" +
                            lastAckedOffset + ", got=" + offset);
                    return;
                }

                if (data.length > MAX_CHUNK_SIZE) {
                    Log.w(TAG, "Received payload longer than max buffer size");
                    throw new ChannelException(token, "Payload too large");
                }

                if (receiveBuffer == null) {
                    receiveBuffer = ByteBuffer.allocate(RECEIVE_BUFFER_SIZE);
                }

                receiveBuffer.clear();
                receiveBuffer.put(data);
                receiveBuffer.flip();

                if (isFinal) {
                    receivePending = true;
                }

                if (inputFd != null) {
                    transport.setMode(inputFd, ChannelTransport.IOMode.WRITE);
                }

                setReceivingState(RECEIVING_STATE_WAITING_TO_WRITE);
                break;

            case RECEIVING_STATE_WAITING_TO_WRITE:
                if (offset <= lastAckedOffset) {
                    Log.d(TAG, "Ignoring duplicate data packet");
                    return;
                }

                Log.w(TAG, "Received new data before ACK of last one");
                throw new ChannelException(token, "Data received before ACK");

            case RECEIVING_STATE_CLOSED:
                throw new ChannelException(token, "Received data after close");
        }
    }

    public void processIncomingBuffer() throws IOException {
        if (receivingState != RECEIVING_STATE_WAITING_TO_WRITE) {
            return;
        }

        if (inputFd == null || receiveBuffer == null) {
            return;
        }

        if (receiveBuffer.hasRemaining()) {
            int toWrite = receiveBuffer.remaining();
            int written = transport.write(inputFd, receiveBuffer.array(),
                    receiveBuffer.position(), toWrite);

            if (written > 0) {
                receiveBuffer.position(receiveBuffer.position() + written);
            }
        }

        if (!receiveBuffer.hasRemaining()) {
            lastAckedOffset += receiveBuffer.capacity();

            channelManager.sendDataAck(this, lastAckedOffset, receivePending);

            if (receivePending) {
                onChannelInputClosed(ChannelStatusCodes.CLOSE_REASON_NORMAL, 0);
            } else {
                transport.setMode(inputFd, ChannelTransport.IOMode.NONE);
                setReceivingState(RECEIVING_STATE_WAITING_FOR_DATA);
            }
        }
    }

    public void sendCloseRequest(int errorCode) throws IOException {
        Log.d(TAG, "Sending close request: errorCode=" + errorCode);
        channelManager.sendCloseRequest(this, errorCode);
    }

    public void onRemoteCloseReceived(int errorCode) throws IOException, ChannelException {
        Log.d(TAG, "Received remote close: errorCode=" + errorCode);

        if (openResultDispatcher != null) {
            try {
                openResultDispatcher.onResult(ChannelStatusCodes.CHANNEL_CLOSED, null, channelPath);
            } catch (Exception e) {
                Log.e(TAG, "Error in open result callback", e);
            } finally {
                openResultDispatcher = null;
            }
        }

        if (outputCallbacks != null) {
            try {
                outputCallbacks.onChannelClosed(ChannelStatusCodes.CLOSE_REASON_REMOTE_CLOSE, errorCode);
            } catch (RemoteException e) {
                Log.w(TAG, "Failed to notify output callbacks", e);
            }
        }

        onChannelInputClosed(ChannelStatusCodes.CLOSE_REASON_REMOTE_CLOSE, errorCode);

        if (sendingState == SENDING_STATE_CLOSED || sendingState == SENDING_STATE_NOT_STARTED) {
            sendCloseRequest(errorCode);
            throw new ChannelException(token, "Remote close");
        } else {
            setConnectionState(CONNECTION_STATE_CLOSING);
            closeReason = errorCode;

            if (sendingState == SENDING_STATE_WAITING_TO_READ) {
                processOutgoingData();
            }
        }
    }

    public void abortOpenChannel() throws IOException, ChannelException {
        if (openResultDispatcher != null) {
            Log.w(TAG, "openChannel cancelled - remote node not reachable");
            openResultDispatcher.onResult(ChannelStatusCodes.CHANNEL_NOT_CONNECTED, null, channelPath);
            openResultDispatcher = null;
        }

        onChannelOutputClosed(ChannelStatusCodes.CLOSE_REASON_LOCAL_CLOSE, 0);
        onChannelInputClosed(ChannelStatusCodes.CLOSE_REASON_LOCAL_CLOSE, 0);
        throw new ChannelException(token, "Open channel aborted");
    }

    public void setInputStream(ParcelFileDescriptor fd, IChannelStreamCallbacks callbacks)
            throws RemoteException {
        if (receivingState == RECEIVING_STATE_CLOSED) {
            throw new IllegalStateException("Cannot set input FD after closing");
        }
        if (inputFd != null) {
            throw new IllegalStateException("Input FD already set");
        }
        if (fd == null) {
            throw new NullPointerException("fd is null");
        }

        this.inputFd = fd;
        this.inputCallbacks = callbacks;

        transport.register(fd);
        linkToDeath(callbacks);

        Log.d(TAG, "Input stream configured for channel " + token);
    }

    public void setOutputStream(ParcelFileDescriptor fd, IChannelStreamCallbacks callbacks,
                                long startOffset, long length) throws RemoteException {
        if (startOffset < 0) {
            throw new IllegalArgumentException("invalid startOffset " + startOffset);
        }
        if (length != -1 && length < 0) {
            throw new IllegalArgumentException("invalid length " + length);
        }
        if (sendingState != SENDING_STATE_NOT_STARTED) {
            throw new IllegalStateException("Output FD already set");
        }
        if (outputFd != null) {
            throw new IllegalStateException("Output FD already set");
        }
        if (fd == null) {
            throw new NullPointerException("fd is null");
        }

        this.outputFd = fd;
        this.outputCallbacks = callbacks;
        this.sendOffset = startOffset;
        this.sendMaxLength = length;
        this.currentSendOffset = startOffset;
        if (length >= 0) {
            this.totalDataSize = length;
        } else {
            try {
                long fileSize = 0;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CUPCAKE) {
                    fileSize = fd.getStatSize();
                }
                this.totalDataSize = fileSize - startOffset;
            } catch (Exception e) {
                this.totalDataSize = -1;
            }
        }

        transport.register(fd);
        transport.setMode(fd, ChannelTransport.IOMode.READ);

        setSendingState(SENDING_STATE_WAITING_TO_READ);
        linkToDeath(callbacks);

        Log.d(TAG, String.format("Output stream configured: offset=%d, length=%d",
                startOffset, length));
    }

    public void forceClose() throws IOException {
        if (connectionState == CONNECTION_STATE_CLOSED) return;


        if (openTimeoutOp != null) {
            openTimeoutOp.cancel();
            openTimeoutOp = null;
        }

        if (sendPendingOp != null) {
            sendPendingOp.cancel();
            sendPendingOp = null;
        }

        if (openResultDispatcher != null) {
            try {
                openResultDispatcher.onResult(ChannelStatusCodes.CHANNEL_CLOSED, null, channelPath);
            } catch (Exception e) {
                Log.e(TAG, "Error in open result callback", e);
            } finally {
                openResultDispatcher = null;
            }
        }

        try {
            onChannelInputClosed(ChannelStatusCodes.CLOSE_REASON_LOCAL_CLOSE, 0);
        } catch (Exception e) {
            Log.w(TAG, "Error closing input", e);
        }

        onChannelOutputClosed(ChannelStatusCodes.CLOSE_REASON_LOCAL_CLOSE, 0);

        receiveBuffer = null;
        sendBuffer = null;

        setConnectionState(CONNECTION_STATE_CLOSED);
    }

    public void onChannelInputClosed(int closeReason, int errorCode) throws IOException {
        if (inputFd == null) return;

        if (inputCallbacks != null) {
            unlinkToDeath(inputCallbacks.asBinder());
            if (closeReason != ChannelStatusCodes.CLOSE_REASON_NORMAL) {
                try {
                    inputCallbacks.onChannelClosed(closeReason, errorCode);
                } catch (RemoteException e) {
                    Log.w(TAG, "Failed to notify InputStream of close", e);
                }
            }
        }

        transport.unregister(inputFd);

        try {
            inputFd.close();
        } catch (IOException e) {
            Log.w(TAG, "Failed to close receiving FD", e);
        }

        inputFd = null;
        inputCallbacks = null;
        setReceivingState(RECEIVING_STATE_CLOSED);

        if (callbacks != null) {
            try {
                callbacks.onChannelInputClosed(token, channelPath, closeReason, errorCode);
            } catch (Exception e) {
                Log.e(TAG, "Error in input closed callback", e);
            }
        }
    }

    public void onChannelOutputClosed(int closeReason, int errorCode) throws IOException {
        if (outputFd == null) return;

        if (outputCallbacks != null) {
            unlinkToDeath(outputCallbacks.asBinder());
            if (closeReason != ChannelStatusCodes.CLOSE_REASON_NORMAL) {
                try {
                    outputCallbacks.onChannelClosed(closeReason, errorCode);
                } catch (RemoteException e) {
                    Log.w(TAG, "Failed to notify OutputStream of close", e);
                }
            }
        }

        transport.unregister(outputFd);

        try {
            outputFd.close();
        } catch (IOException e) {
            Log.w(TAG, "Failed to close sending FD", e);
        }

        outputFd = null;
        outputCallbacks = null;
        sendBuffer = null;
        setSendingState(SENDING_STATE_CLOSED);

        if (callbacks != null) {
            callbacks.onChannelOutputClosed(token, channelPath, closeReason, errorCode);
        }
    }

    private void linkToDeath(IChannelStreamCallbacks callbacks) throws RemoteException {
        if (callbacks == null || deathRecipient == null) return;
        try {
            callbacks.asBinder().linkToDeath(deathRecipient, 0);
        } catch (RemoteException e) {
            deathRecipient.binderDied();
        }
    }

    private void unlinkToDeath(IBinder binder) {
        if (binder == null || deathRecipient == null) return;
        try {
            binder.unlinkToDeath(deathRecipient, 0);
        } catch (Exception ignored) {}
    }

    public static String getConnectionStateString(int state) {
        switch (state) {
            case CONNECTION_STATE_NOT_STARTED: return "NOT_STARTED";
            case CONNECTION_STATE_OPEN_SENT: return "OPEN_SENT";
            case CONNECTION_STATE_ESTABLISHED: return "ESTABLISHED";
            case CONNECTION_STATE_CLOSING: return "CLOSING";
            case CONNECTION_STATE_CLOSED: return "CLOSED";
            default: return "UNKNOWN(" + state + ")";
        }
    }

    public static String getSendingStateString(int state) {
        switch (state) {
            case SENDING_STATE_NOT_STARTED: return "NOT_STARTED";
            case SENDING_STATE_WAITING_TO_READ: return "WAITING_TO_READ";
            case SENDING_STATE_WAITING_FOR_ACK: return "WAITING_FOR_ACK";
            case SENDING_STATE_CLOSED: return "CLOSED";
            default: return "UNKNOWN(" + state + ")";
        }
    }

    public static String getReceivingStateString(int state) {
        switch (state) {
            case RECEIVING_STATE_WAITING_FOR_DATA: return "WAITING_FOR_DATA";
            case RECEIVING_STATE_WAITING_TO_WRITE: return "WAITING_TO_WRITE";
            case RECEIVING_STATE_CLOSED: return "CLOSED";
            default: return "UNKNOWN(" + state + ")";
        }
    }

    @Override
    public String toString() {
        return "ChannelStateMachine{token=" + token +
                ", path='" + channelPath + "'" +
                ", connection=" + getConnectionStateString(connectionState) +
                ", sending=" + getSendingStateString(sendingState) +
                ", receiving=" + getReceivingStateString(receivingState) + "}"+
                ", age=" + (System.currentTimeMillis() - creationTime) + "ms}";
    }
}