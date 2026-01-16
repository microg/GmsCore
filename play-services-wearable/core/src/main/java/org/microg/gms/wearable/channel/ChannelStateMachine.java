package org.microg.gms.wearable.channel;

import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.wearable.internal.IChannelStreamCallbacks;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ChannelStateMachine {
    private static final String TAG = "ChannelStateMachine";

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

    private final ChannelManager channelManager;
    private final ChannelCallbacks callbacks;
    private final IBinder.DeathRecipient deathRecipient;

    private int connectionState = CONNECTION_STATE_NOT_STARTED;
    private int sendingState = SENDING_STATE_NOT_STARTED;
    private int receivingState = RECEIVING_STATE_WAITING_FOR_DATA;

    private String path;
    private long sequenceNumberIn;
    private long sequenceNumberOut;

    private ParcelFileDescriptor inputFd;
    private IChannelStreamCallbacks inputCallbacks;
    private ByteBuffer receiveBuffer;

    private ParcelFileDescriptor outputFd;
    private IChannelStreamCallbacks outputCallbacks;
    private ByteBuffer sendBuffer;
    private long sendOffset;
    private long sendMaxLength;
    private int pendingCloseErrorCode;

    private OpenChannelCallback openCallback;

    public ChannelStateMachine(
            ChannelToken token,
            ChannelManager channelManager,
            ChannelCallbacks callbacks,
            boolean isLocalOpener,
            IBinder.DeathRecipient deathRecipient) {

        this.token = token;
        this.channelManager = channelManager;
        this.callbacks = callbacks;
        this.isLocalOpener = isLocalOpener;
        this.deathRecipient = deathRecipient;
    }

    public int getConnectionState() { return connectionState; }
    public int getSendingState() { return sendingState; }
    public int getReceivingState() { return receivingState; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public void setConnectionState(int newState) {
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, String.format("Channel(%s): %s -> %s",
                    token, getConnectionStateString(connectionState), getConnectionStateString(newState)));
        }
        this.connectionState = newState;
    }

    public void setSendingState(int newState) {
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, String.format("Channel(%s): Sender %s -> %s",
                    token, getSendingStateString(sendingState), getSendingStateString(newState)));
        }
        this.sendingState = newState;
    }

    public void setReceivingState(int newState) {
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, String.format("Channel(%s): Receiver %s -> %s",
                    token, getReceivingStateString(receivingState), getReceivingStateString(newState)));
        }
        this.receivingState = newState;
    }

    public boolean hasInputStream() { return inputFd != null; }
    public boolean hasOutputStream() { return outputFd != null; }
    public boolean isInputClosed() { return receivingState == RECEIVING_STATE_CLOSED; }
    public boolean isOutputClosed() { return sendingState == SENDING_STATE_CLOSED; }

    public void setOpenCallback(OpenChannelCallback callback) {
        this.openCallback = callback;
    }

    public void onChannelEstablished() {
        setConnectionState(CONNECTION_STATE_ESTABLISHED);
        if (openCallback != null) {
            openCallback.onResult(ChannelStatusCodes.SUCCESS, token, path);
            openCallback = null;
        }
    }

    public void onOpenFailed(int errorCode) {
        if (openCallback != null) {
            Log.w(TAG, "openChannel failed with error: " + errorCode);
            openCallback.onResult(errorCode, null, path);
            openCallback = null;
        }
        setConnectionState(CONNECTION_STATE_CLOSED);
    }

    public void onDataReceived(byte[] data, boolean isFinal, long requestId) throws IOException {
        if (inputFd == null) {
            Log.w(TAG, "Received data but no input FD set");
            return;
        }

        try {
            FileOutputStream fos = new FileOutputStream(inputFd.getFileDescriptor());
            fos.write(data);
        } catch (IOException e) {
            Log.e(TAG, "Failed to write received data", e);
            throw e;
        }

        if (isFinal) {
            closeInputStream(ChannelStatusCodes.CLOSE_REASON_NORMAL, 0);
        }
    }

    public void onDataAckReceived(long requestId, boolean isFinal) {
        if (sendingState == SENDING_STATE_WAITING_FOR_ACK) {
            if (isFinal) {
                setSendingState(SENDING_STATE_CLOSED);
            } else {
                setSendingState(SENDING_STATE_WAITING_TO_READ);
            }
        }
    }

    public void onRemoteClose(int errorCode) throws IOException {
        closeInputStream(ChannelStatusCodes.CLOSE_REASON_REMOTE_CLOSE, errorCode);
        closeOutputStream(ChannelStatusCodes.CLOSE_REASON_REMOTE_CLOSE, errorCode);
        setConnectionState(CONNECTION_STATE_CLOSED);

        if (callbacks != null) {
            callbacks.onChannelClosed(token, path,
                    ChannelStatusCodes.CLOSE_REASON_REMOTE_CLOSE, errorCode);
        }
    }

    public void close() throws IOException {
        setConnectionState(CONNECTION_STATE_CLOSED);

        if (openCallback != null) {
            openCallback.onResult(ChannelStatusCodes.CHANNEL_CLOSED, null, path);
            openCallback = null;
        }

        closeInputStream(ChannelStatusCodes.CLOSE_REASON_LOCAL_CLOSE, 0);
        closeOutputStream(ChannelStatusCodes.CLOSE_REASON_LOCAL_CLOSE, 0);

        receiveBuffer = null;
        sendBuffer = null;
    }

    public void closeInputStream(int closeReason, int errorCode) throws IOException {
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

        try {
            inputFd.close();
        } catch (IOException e) {
            Log.w(TAG, "Failed to close receiving FD", e);
        }

        inputFd = null;
        inputCallbacks = null;
        setReceivingState(RECEIVING_STATE_CLOSED);

        if (callbacks != null) {
            callbacks.onChannelInputClosed(token, path, closeReason, errorCode);
        }
    }

    public void closeOutputStream(int closeReason, int errorCode) throws IOException {
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

        try {
            outputFd.close();
        } catch (IOException e) {
            Log.w(TAG, "Failed to close sending FD", e);
        }

        outputFd = null;
        outputCallbacks = null;
        setSendingState(SENDING_STATE_CLOSED);

        if (callbacks != null) {
            callbacks.onChannelOutputClosed(token, path, closeReason, errorCode);
        }
    }

    public void clearOpenCallback() {
        this.openCallback = null;
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

        linkToDeath(callbacks);
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

        setSendingState(SENDING_STATE_WAITING_TO_READ);
        linkToDeath(callbacks);
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
                ", path='" + path + "'" +
                ", connection=" + getConnectionStateString(connectionState) +
                ", sending=" + getSendingStateString(sendingState) +
                ", receiving=" + getReceivingStateString(receivingState) + "}";
    }
}