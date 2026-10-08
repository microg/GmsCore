package org.microg.gms.wearable.channel;

import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.wearable.internal.IChannelStreamCallbacks;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChannelStateMachine {
    private static final String TAG = "ChannelStateMachine";

    private static final int DEFAULT_CHUNK_SIZE = 8192;
    private static final long DATA_ACK_TIMEOUT_MS = 2000;
    private static final int MAX_CHUNK_SIZE = 65536;
    private static final int MAX_PENDING_CHUNKS = 64;

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
    private final Deque<PendingChunk> pendingChunks = new ArrayDeque<>();

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

    public final ChannelAssetApiEnum apiOrigin;

    public final boolean isReliable;

    private static final class PendingChunk {
        final byte[] data;
        final long reqId;
        final boolean isFinal;
        int written;

        PendingChunk(byte[] data, long reqId, boolean isFinal) {
            this.data = data;
            this.reqId = reqId;
            this.isFinal = isFinal;
        }
    }

    public ChannelStateMachine(
            ChannelToken token,
            ChannelManager channelManager,
            ChannelTransport transport,
            ChannelCallbacks callbacks,
            ChannelAssetApiEnum apiOrigin,
            boolean isReliable,
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
        this.apiOrigin = Objects.requireNonNull(apiOrigin, "apiOrigin");
        this.creationTime = System.currentTimeMillis();
        this.isReliable = isReliable;
    }

    private ChannelCallbacks resolveCallbacks() {
        if (callbacks != null) return callbacks;
        return channelManager.getCallbacksOrNull(apiOrigin);
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
                Log.w(TAG, String.format("Channel(%s): Unexpected state transition %s -> %s (allowing)",
                        token, getConnectionStateString(connectionState),
                        getConnectionStateString(newState)));
            } else {
                Log.v(TAG, String.format("Channel(%s): %s -> %s",
                        token, getConnectionStateString(connectionState),
                        getConnectionStateString(newState)));
            }

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

            int lastRead = 0;
            boolean gotAnyData = false;
            boolean hitEof = false;
            while (sendBuffer.hasRemaining())
            {
                lastRead = transport.read(outputFd, sendBuffer.array(),
                        sendBuffer.position(), sendBuffer.remaining());
                if (lastRead == 0) break;
                if (lastRead < 0) {
                    hitEof = true;
                    break;
                }
                sendBuffer.position(sendBuffer.position() + lastRead);
                gotAnyData = true;
            }

            int bytesRead = sendBuffer.hasRemaining() ? transport.read(outputFd,
                    sendBuffer.array(), sendBuffer.position(), 0) : 0;

            if (!gotAnyData && !hitEof)
            {
                sendBuffer.clear();
                return;
            }

            sendBuffer.flip();

            if (sendMaxLength >= 0)
                sendMaxLength -= sendBuffer.remaining();

            byte[] data = new byte[sendBuffer.remaining()];
            sendBuffer.get(data);

            boolean isFinal = hitEof || (sendMaxLength == 0);

            if (data.length == 0 && !isFinal)
                return;

            long requestId = sequenceNumber++;

            Log.d(TAG, String.format("Sending chunk: size=%d, isFinal=%b, seq=%d",
                    data.length, isFinal, requestId));

            if (channelManager.sendData(this, data, isFinal, requestId)) {
                setSendingState(SENDING_STATE_WAITING_FOR_ACK);

                sendPendingOp = new PendingOperation(handler,
                        this::onAckTimeout, DATA_ACK_TIMEOUT_MS, "Send data chunk");
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

    private void onAckTimeout() {
        Log.d(TAG, "No DATA_ACK within " + DATA_ACK_TIMEOUT_MS + " ms,"
                + "assuming delivered, resuming writes");
        sendPendingOp = null;

        if (sendingState == SENDING_STATE_WAITING_FOR_ACK) {
            setSendingState(SENDING_STATE_WAITING_TO_READ);
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

//        lastAckedOffset = ackOffset;

        if (isFinal) {
            if (connectionState == CONNECTION_STATE_CLOSING) {
                Log.d(TAG, "onDataAckReceived: Received final ACK during channel close — closing output");
                try {
                    onChannelOutputClosed(ChannelStatusCodes.CLOSE_REASON_REMOTE_CLOSE, closeReason);
                } catch (IOException e) {
                    Log.e(TAG, "Error closing output during channel close", e);
                }
            } else {
                Log.d(TAG, "onDataAckReceived: Remote node closed input stream before all data was sent");
                try {
                    onChannelOutputClosed(ChannelStatusCodes.CLOSE_REASON_NORMAL, 0);
                } catch (IOException e) {
                    Log.e(TAG, "Error closing output after remote input close", e);
                }
            }
        } else {
            setSendingState(SENDING_STATE_WAITING_TO_READ);
        }
    }

    public void onDataReceived(byte[] data, boolean isFinal, long requestId) throws ChannelException {
        synchronized (stateLock) {
            if (connectionState != CONNECTION_STATE_ESTABLISHED &&
                    connectionState != CONNECTION_STATE_CLOSING) {
                Log.w(TAG, "onDataReceived: bad connection state " +
                        getConnectionStateString(connectionState) + " — dropping");
                return;
            }
        }

        if (receivingState == RECEIVING_STATE_CLOSED) {
            Log.w(TAG, "onDataReceived: receiver closed — dropping requestId=" + requestId);
            return;
        }

        if (data.length > MAX_CHUNK_SIZE) {
            Log.w(TAG, "onDataReceived: payload " + data.length + " > MAX_CHUNK_SIZE — dropping");
            return;
        }

        synchronized (pendingChunks) {
            for (PendingChunk pc : pendingChunks) {
                if (pc.reqId == requestId) {
                    Log.d(TAG, "onDataReceived: duplicate requestId=" + requestId + " — ignoring");
                    return;
                }
            }

            if (pendingChunks.size() >= MAX_PENDING_CHUNKS) {
                Log.w(TAG, "onDataReceived: onDataReceived: queue full ("
                        + MAX_PENDING_CHUNKS + ") dropping requestId=" + requestId);
                return;
            }
            
            pendingChunks.addLast(new PendingChunk(data, requestId, isFinal));
            Log.d(TAG, "onDataReceived: queued requestId=" + requestId + " size=" + data.length
                    + " isFinal=" + isFinal + " queueDepth=" + pendingChunks.size());
        }

        if (inputFd != null)
            transport.setMode(inputFd, ChannelTransport.IOMode.WRITE);

        setReceivingState(RECEIVING_STATE_WAITING_TO_WRITE);
    }

    public void processIncomingBuffer() throws IOException {
        if (receivingState != RECEIVING_STATE_WAITING_TO_WRITE) {
            return;
        }

        if (inputFd == null) {
            return;
        }

        boolean appClosedInput = false;
        long lastDrainedRequestId = -1;
        boolean lastDrainedFinal = false;

        while (true) {
            PendingChunk chunk;
            synchronized (pendingChunks) {
                chunk = pendingChunks.peekFirst();
            }

            if (chunk == null) break;

            int remaining = chunk.data.length - chunk.written;

            int written = 0;
            if (remaining > 0)
                written = transport.write(inputFd, chunk.data, chunk.written, remaining);

            if (written == -1) {
                Log.d(TAG, "processIncomingBuffer: app closed input for " + token);
                appClosedInput = true;
                break;
            }

            if (written > 0)
                chunk.written += written;

            if (chunk.written < chunk.data.length) {
                Log.d(TAG, "processIncomingBuffer: pipe full, " + (chunk.data.length - chunk.written)
                        + " bytes left on requestId=" + chunk.reqId);
                break;
            }

            synchronized (pendingChunks) {
                pendingChunks.pollFirst();
            }

            lastAckedOffset = chunk.reqId + 1;
            lastDrainedRequestId = chunk.reqId;
            lastDrainedFinal = chunk.isFinal;
            channelManager.sendDataAck(this, chunk.reqId, chunk.isFinal);
            Log.d(TAG, "processIncomingBuffer: drained+ack requestId="
                    + chunk.reqId + " isFinal=" + chunk.isFinal);
        }

        if (appClosedInput) {
            long ackId = lastDrainedRequestId >= 0
                    ? lastDrainedRequestId : Math.max(0, lastAckedOffset - 1);
            channelManager.sendDataAck(this, ackId, true);
            synchronized (pendingChunks) {
                pendingChunks.clear();
            }
            onChannelInputClosed(ChannelStatusCodes.CLOSE_REASON_NORMAL, 0);
            return;
        }

        boolean queueEmpty;
        synchronized (pendingChunks) {
            queueEmpty = pendingChunks.isEmpty();
        }
        if (queueEmpty) {
            transport.setMode(inputFd, ChannelTransport.IOMode.NONE);
        }
    }

    public boolean hasPendingIncoming() {
        synchronized (pendingChunks) {
            return !pendingChunks.isEmpty();
        }
    }

    public void sendCloseRequest(int errorCode) throws IOException {
        Log.d(TAG, "Sending close request: errorCode=" + errorCode);
        channelManager.sendCloseRequest(this, errorCode);
    }

    public void onRemoteCloseReceived(int errorCode) throws IOException {
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
            setConnectionState(CONNECTION_STATE_CLOSED);
            channelManager.removeChannel(token);
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
                long fileSize = fd.getStatSize();
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
        int prevState;
        synchronized (stateLock) {
            if (connectionState == CONNECTION_STATE_CLOSED) return;
            prevState = connectionState;
            this.connectionState = CONNECTION_STATE_CLOSED;
        }

        if (prevState == CONNECTION_STATE_ESTABLISHED
                || prevState == CONNECTION_STATE_OPEN_SENT
                || prevState == CONNECTION_STATE_CLOSING) {
            try {
                channelManager.sendCloseRequest(this, ChannelStatusCodes.CLOSE_REASON_LOCAL_CLOSE);
            } catch (IOException e) {
                Log.w(TAG, "forceClose: peer notify failed: " + e.getMessage());
            }
        }

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
                Log.e(TAG, "Error in open result callback during forceClose", e);
            } finally {
                openResultDispatcher = null;
            }
        }

        synchronized (pendingChunks) {
            pendingChunks.clear();
        }

        try {
            onChannelInputClosed(ChannelStatusCodes.CLOSE_REASON_LOCAL_CLOSE, 0);
        } catch (Exception e) {
            Log.w(TAG, "Error closing input during forceClose", e);
        }
        onChannelOutputClosed(ChannelStatusCodes.CLOSE_REASON_LOCAL_CLOSE, 0);
        sendBuffer = null;
    }

    public void onChannelInputClosed(int closeReason, int errorCode) throws IOException {
        if (inputFd == null) return;

        if (inputCallbacks != null) {
            unlinkToDeath(inputCallbacks.asBinder());
            if (closeReason != ChannelStatusCodes.CLOSE_REASON_NORMAL || errorCode != 0) {
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

        ChannelCallbacks cb = resolveCallbacks();
        if (cb != null) {
            try {
                cb.onChannelInputClosed(token, channelPath, closeReason, errorCode);
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

        ChannelCallbacks cb = resolveCallbacks();
        if (cb != null) {
            cb.onChannelOutputClosed(token, channelPath, closeReason, errorCode);
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

    @NonNull
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