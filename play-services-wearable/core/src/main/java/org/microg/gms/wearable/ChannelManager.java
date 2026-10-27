/*
 * SPDX-FileCopyrightText: 2026 microG contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.wearable;

import android.content.Context;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.wearable.internal.ChannelEventParcelable;
import com.google.android.gms.wearable.internal.ChannelParcelable;
import com.google.android.gms.wearable.internal.CloseChannelResponse;
import com.google.android.gms.wearable.internal.GetChannelInputStreamResponse;
import com.google.android.gms.wearable.internal.GetChannelOutputStreamResponse;
import com.google.android.gms.wearable.internal.IChannelStreamCallbacks;
import com.google.android.gms.wearable.internal.IWearableCallbacks;
import com.google.android.gms.wearable.internal.OpenChannelResponse;

import org.microg.gms.common.PackageUtils;
import org.microg.wearable.ChannelFlow;
import org.microg.wearable.WearableConnection;
import org.microg.wearable.proto.ChannelControlRequest;
import org.microg.wearable.proto.ChannelDataAckRequest;
import org.microg.wearable.proto.ChannelDataHeader;
import org.microg.wearable.proto.ChannelDataRequest;
import org.microg.wearable.proto.ChannelRequest;
import org.microg.wearable.proto.Request;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** Channel API service. Never runs a blocking pipe read/write on the transport reader. */
final class ChannelManager {
    private static final String TAG = "GmsWearChannel";
    private static final int OPEN = 1, OPEN_ACK = 2, CLOSE = 3;
    private static final int MAX_CHANNELS = 8;
    private static final long TIMEOUT_MS = 15000;
    private final Context context;
    private final WearableImpl wearable;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Session> sessions = new HashMap<>();
    private final ThreadPoolExecutor workers = new ThreadPoolExecutor(0, MAX_CHANNELS * 2,
            60, TimeUnit.SECONDS, new SynchronousQueue<>(), runnable -> {
                Thread thread = new Thread(runnable, "wearable-channel-stream");
                thread.setDaemon(true);
                return thread;
            });
    private boolean stopped;

    ChannelManager(Context context, WearableImpl wearable) {
        this.context = context;
        this.wearable = wearable;
    }

    private final class Session {
        final String packageName, signature, node;
        final WearableConnection connection;
        final long id;
        final boolean opener;
        final ChannelParcelable channel;
        final ChannelFlow flow;
        IWearableCallbacks opening;
        boolean established, closed, inputRequested, outputRequested, inputClosed, outputClosed;
        ParcelFileDescriptor inputWriter, outputReader;
        IChannelStreamCallbacks inputCallbacks, outputCallbacks;
        Runnable timeout;

        Session(String packageName, String signature, String node, WearableConnection connection,
                long id, boolean opener, String path) {
            this.packageName = packageName;
            this.signature = signature;
            this.node = node;
            this.connection = connection;
            this.id = id;
            this.opener = opener;
            this.channel = new ChannelParcelable(UUID.randomUUID().toString(), node, path);
            this.flow = new ChannelFlow(id, opener);
        }

        ChannelControlRequest control(int type, int error) {
            return new ChannelControlRequest.Builder().type(type).channelId(id)
                    .fromChannelOperator(opener).packageName(packageName).signatureDigest(signature)
                    .path(channel.path).closeErrorCode(error).build();
        }
    }

    void open(String packageName, String node, String path, IWearableCallbacks callbacks) throws RemoteException {
        WearableConnection connection = wearable.connectionForNode(node);
        if (node == null || path == null || !path.startsWith("/") || path.length() > 4096) {
            callbacks.onOpenChannelResponse(new OpenChannelResponse(10, null));
            return;
        }
        if (connection == null) {
            callbacks.onOpenChannelResponse(new OpenChannelResponse(4000, null));
            return;
        }
        Session session;
        synchronized (this) {
            if (stopped || sessions.size() >= MAX_CHANNELS) {
                callbacks.onOpenChannelResponse(new OpenChannelResponse(8, null));
                return;
            }
            session = new Session(packageName, PackageUtils.firstSignatureDigest(context, packageName),
                    node, connection, random.nextLong() & Long.MAX_VALUE, true, path);
            session.opening = callbacks;
            sessions.put(session.channel.token, session);
        }
        session.timeout = () -> terminate(session, 1, 0, 14, true);
        wearable.networkHandler.postDelayed(session.timeout, TIMEOUT_MS);
        try {
            send(session, new ChannelRequest.Builder().channelControlRequest(session.control(OPEN, 0)).build());
            Log.d(TAG, "Channel open sent");
        } catch (IOException e) {
            terminate(session, 1, 0, 8, false);
        }
    }

    private synchronized Session find(String node, long id, boolean localOpener) {
        for (Session session : sessions.values()) {
            if (session.id == id && session.opener == localOpener && session.node.equals(node)) return session;
        }
        return null;
    }

    void receive(WearableConnection connection, String node, Request envelope) {
        ChannelRequest request = envelope.request;
        if (request == null || (request.version != null && request.version > 1)
                || (request.origin != null && request.origin != 0)
                || (envelope.targetNodeId != null && !wearable.getLocalNodeId().equals(envelope.targetNodeId))
                || (envelope.sourceNodeId != null && !node.equals(envelope.sourceNodeId))
                || !wearable.isCurrentConnection(node, connection)) return;
        int count = (request.channelControlRequest == null ? 0 : 1)
                + (request.channelDataRequest == null ? 0 : 1) + (request.channelDataAckRequest == null ? 0 : 1);
        if (count != 1) return;
        Session session = null;
        try {
            ChannelControlRequest control = request.channelControlRequest;
            ChannelDataHeader header = request.channelDataRequest != null ? request.channelDataRequest.header
                    : request.channelDataAckRequest != null ? request.channelDataAckRequest.header : null;
            Long id = control != null ? control.channelId : header != null ? header.channelId : null;
            Boolean peerOpener = control != null ? control.fromChannelOperator
                    : header != null ? header.fromChannelOperator : null;
            // Channel IDs are unsigned fixed64 values; peers may set the high bit.
            if (id == null || peerOpener == null) return;
            session = find(node, id, !peerOpener);
            if (control != null && Integer.valueOf(OPEN).equals(control.type)) {
                if (!peerOpener || !wearable.matchesInstalledSignature(envelope.packageName, envelope.signatureDigest)
                        || !envelope.packageName.equals(control.packageName)
                        || !envelope.signatureDigest.equals(control.signatureDigest)
                        || control.path == null || !control.path.startsWith("/") || control.path.length() > 4096) return;
                if (session == null) {
                    synchronized (this) {
                        if (stopped || sessions.size() >= MAX_CHANNELS) return;
                        session = new Session(envelope.packageName, envelope.signatureDigest, node,
                                connection, id, false, control.path);
                        sessions.put(session.channel.token, session);
                    }
                    establish(session);
                    send(session, new ChannelRequest.Builder().channelControlRequest(session.control(OPEN_ACK, 0)).build());
                    event(session, 1, 0, 0);
                } else if (authorized(session, connection, envelope)) {
                    send(session, new ChannelRequest.Builder().channelControlRequest(session.control(OPEN_ACK, 0)).build());
                }
                return;
            }
            if (session == null || !authorized(session, connection, envelope)) return;
            if (control != null) {
                if (Integer.valueOf(OPEN_ACK).equals(control.type) && session.opener) {
                    boolean first;
                    IWearableCallbacks callback;
                    synchronized (session) {
                        first = !session.established && !session.closed;
                        callback = session.opening;
                        session.opening = null;
                        if (first) establish(session);
                    }
                    if (first) {
                        wearable.networkHandler.removeCallbacks(session.timeout);
                        if (callback != null) callback.onOpenChannelResponse(new OpenChannelResponse(0, session.channel));
                        event(session, 1, 0, 0);
                        Log.d(TAG, "Channel open acknowledged");
                    }
                } else if (Integer.valueOf(CLOSE).equals(control.type)) {
                    terminate(session, 2, control.closeErrorCode == null ? 0 : control.closeErrorCode, 16, false);
                }
            } else if (request.channelDataRequest != null) {
                ChannelDataAckRequest duplicate = session.flow.receive(request.channelDataRequest);
                if (duplicate != null) send(session, new ChannelRequest.Builder().channelDataAckRequest(duplicate).build());
            } else {
                session.flow.acknowledge(request.channelDataAckRequest);
            }
        } catch (IOException | RemoteException e) {
            Log.w(TAG, "Channel operation failed");
            if (session != null) terminate(session, 3, 0, 8, true);
        }
    }

    private boolean authorized(Session session, WearableConnection connection, Request request) {
        return session.connection == connection && session.packageName.equals(request.packageName)
                && session.signature.equals(request.signatureDigest)
                && wearable.matchesInstalledSignature(session.packageName, session.signature);
    }

    private void establish(Session session) throws IOException {
        synchronized (session) {
            session.flow.establish();
            session.established = true;
        }
    }

    private void send(Session session, ChannelRequest request) throws IOException {
        wearable.sendChannelRequest(session.connection, session.packageName, session.signature, session.node,
                request.newBuilder().version(1).origin(0).build());
    }

    private synchronized Session owned(String packageName, String token) {
        Session session = sessions.get(token);
        if (session == null || !session.packageName.equals(packageName)
                || !wearable.matchesInstalledSignature(packageName, session.signature)) return null;
        return session;
    }

    void close(String packageName, String token, int error, IWearableCallbacks callbacks) throws RemoteException {
        Session session = owned(packageName, token);
        if (session == null) {
            callbacks.onCloseChannelResponse(new CloseChannelResponse(13));
            return;
        }
        terminate(session, 3, error, 16, true);
        callbacks.onCloseChannelResponse(new CloseChannelResponse(0));
    }

    void input(String packageName, String token, IChannelStreamCallbacks streamCallbacks,
               IWearableCallbacks callbacks) throws RemoteException {
        Session session = owned(packageName, token);
        ParcelFileDescriptor local = null;
        int status = 13;
        if (session != null) {
            synchronized (session) {
                if (session.established && !session.closed && !session.inputRequested) {
                    try {
                        ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
                        local = pipe[0];
                        session.inputWriter = pipe[1];
                        session.inputCallbacks = streamCallbacks;
                        session.inputRequested = true;
                        workers.execute(() -> pumpInput(session));
                        status = 0;
                    } catch (IOException | RejectedExecutionException e) {
                        status = 8;
                        terminate(session, 3, 0, 8, true);
                    }
                }
            }
        }
        try {
            callbacks.onGetChannelInputStreamResponse(new GetChannelInputStreamResponse(status, status == 0 ? local : null));
        } catch (RemoteException e) {
            if (session != null) terminate(session, 3, 0, 16, true);
            throw e;
        } finally {
            closeFd(local);
        }
    }

    void output(String packageName, String token, IChannelStreamCallbacks streamCallbacks,
                IWearableCallbacks callbacks) throws RemoteException {
        Session session = owned(packageName, token);
        ParcelFileDescriptor local = null;
        int status = 13;
        if (session != null) {
            synchronized (session) {
                if (session.established && !session.closed && !session.outputRequested) {
                    try {
                        ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
                        session.outputReader = pipe[0];
                        local = pipe[1];
                        session.outputCallbacks = streamCallbacks;
                        session.outputRequested = true;
                        workers.execute(() -> pumpOutput(session));
                        status = 0;
                    } catch (IOException | RejectedExecutionException e) {
                        status = 8;
                        terminate(session, 3, 0, 8, true);
                    }
                }
            }
        }
        try {
            callbacks.onGetChannelOutputStreamResponse(new GetChannelOutputStreamResponse(status, status == 0 ? local : null));
        } catch (RemoteException e) {
            if (session != null) terminate(session, 3, 0, 16, true);
            throw e;
        } finally {
            closeFd(local);
        }
    }

    private void pumpInput(Session session) {
        try (OutputStream stream = new ParcelFileDescriptor.AutoCloseOutputStream(session.inputWriter)) {
            while (true) {
                ChannelDataRequest data = session.flow.awaitIncoming();
                if (data == null) break;
                stream.write(data.payload.toByteArray());
                ChannelDataAckRequest ack = session.flow.consumed(data);
                send(session, new ChannelRequest.Builder().channelDataAckRequest(ack).build());
                if (data.finalMessage) {
                    streamClosed(session, true, 0, 0);
                    break;
                }
            }
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            terminate(session, 3, 0, 8, true);
        }
    }

    private void pumpOutput(Session session) {
        try (InputStream stream = new ParcelFileDescriptor.AutoCloseInputStream(session.outputReader)) {
            byte[] buffer = new byte[ChannelFlow.MAX_PAYLOAD];
            while (true) {
                int read = stream.read(buffer);
                boolean last = read < 0;
                if (read == 0) continue;
                ChannelDataRequest data = session.flow.send(last ? new byte[0] : Arrays.copyOf(buffer, read), last);
                send(session, new ChannelRequest.Builder().channelDataRequest(data).build());
                session.flow.awaitAcknowledgement(TIMEOUT_MS);
                if (last) {
                    streamClosed(session, false, 0, 0);
                    break;
                }
            }
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            terminate(session, 3, 0, 8, true);
        }
    }

    private void streamClosed(Session session, boolean input, int reason, int error) {
        IChannelStreamCallbacks callback;
        synchronized (session) {
            if (input ? session.inputClosed : session.outputClosed) return;
            if (input) session.inputClosed = true; else session.outputClosed = true;
            callback = input ? session.inputCallbacks : session.outputCallbacks;
            if (input) session.inputCallbacks = null; else session.outputCallbacks = null;
        }
        if (callback != null) {
            try { callback.onStreamClosed(reason, error); } catch (RemoteException ignored) { }
        }
        event(session, input ? 3 : 4, reason, error);
    }

    private void event(Session session, int type, int reason, int error) {
        ChannelEventParcelable event = new ChannelEventParcelable();
        event.channel = session.channel;
        event.eventType = type;
        event.closeReason = reason;
        event.appSpecificErrorCode = error;
        wearable.networkHandler.post(() -> wearable.channelEvent(session.packageName, event));
    }

    private void terminate(Session session, int reason, int error, int openingStatus, boolean sendClose) {
        IWearableCallbacks opening;
        synchronized (session) {
            if (session.closed) return;
            session.closed = true;
            opening = session.opening;
            session.opening = null;
            session.flow.close();
            closeFd(session.inputWriter);
            closeFd(session.outputReader);
        }
        synchronized (this) { sessions.remove(session.channel.token, session); }
        if (session.timeout != null) wearable.networkHandler.removeCallbacks(session.timeout);
        if (sendClose) {
            try { send(session, new ChannelRequest.Builder().channelControlRequest(session.control(CLOSE, error)).build()); }
            catch (IOException ignored) { }
        }
        if (opening != null) {
            try { opening.onOpenChannelResponse(new OpenChannelResponse(openingStatus, null)); }
            catch (RemoteException ignored) { }
        }
        streamClosed(session, true, reason, error);
        streamClosed(session, false, reason, error);
        if (session.established) event(session, 2, reason, error);
        Log.d(TAG, "Channel closed, reason=" + reason);
    }

    void disconnected(WearableConnection connection) {
        ArrayList<Session> snapshot;
        synchronized (this) { snapshot = new ArrayList<>(sessions.values()); }
        for (Session session : snapshot) if (session.connection == connection) terminate(session, 1, 0, 4000, false);
    }

    void stop() {
        ArrayList<Session> snapshot;
        synchronized (this) {
            stopped = true;
            snapshot = new ArrayList<>(sessions.values());
        }
        for (Session session : snapshot) terminate(session, 1, 0, 16, false);
        workers.shutdownNow();
    }

    private static void closeFd(ParcelFileDescriptor fd) {
        if (fd == null) return;
        try { fd.close(); } catch (IOException ignored) { }
    }
}
