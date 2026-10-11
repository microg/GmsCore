/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.gms.wearable;

import android.content.Context;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;

import com.google.android.gms.wearable.MessageOptions;
import com.google.android.gms.wearable.internal.IRpcResponseCallback;
import com.google.android.gms.wearable.internal.IWearableCallbacks;
import com.google.android.gms.wearable.internal.IWearableListener;
import com.google.android.gms.wearable.internal.MessageEventParcelable;
import com.google.android.gms.wearable.internal.RpcResponse;

import org.microg.gms.common.PackageUtils;
import org.microg.wearable.PendingRpcRequests;
import org.microg.wearable.RpcRequestBudget;
import org.microg.wearable.WearableConnection;
import org.microg.wearable.WearableRequests;
import org.microg.wearable.proto.Request;
import org.microg.wearable.proto.RootMessage;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** Request/reply lifecycle, with application identity and transport-session correlation. */
final class RpcRequestManager {
    private static final String TAG = "GmsWearRpc";
    private final Context context;
    private final WearableImpl wearable;
    private final PendingRpcRequests<Completion> outgoing = new PendingRpcRequests<>();
    private final PendingRpcRequests<Reply> incoming = new PendingRpcRequests<>();
    private final AtomicInteger queued = new AtomicInteger();
    private final RpcRequestBudget budget = new RpcRequestBudget();
    private final RpcRequestBudget listenerBudget = new RpcRequestBudget();
    private final ThreadPoolExecutor callbacks = workers("wearable-rpc-callback", PendingRpcRequests.MAX_PENDING);
    private final ThreadPoolExecutor listeners = workers("wearable-rpc-listener", PendingRpcRequests.MAX_PENDING);
    private volatile boolean stopped;

    private static ThreadPoolExecutor workers(String name, int threads) {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(threads, threads, 60, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(PendingRpcRequests.MAX_PENDING), task -> {
                    Thread thread = new Thread(task, name);
                    thread.setDaemon(true);
                    return thread;
                });
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }

    RpcRequestManager(Context context, WearableImpl wearable) {
        this.context = context;
        this.wearable = wearable;
    }

    private final class Completion implements IBinder.DeathRecipient {
        final IWearableCallbacks callback;
        final RpcRequestBudget.Lease lease;
        PendingRpcRequests.Entry<Completion> entry;
        RootMessage message;
        Runnable timeout;
        boolean linked;

        Completion(IWearableCallbacks callback, RpcRequestBudget.Lease lease) {
            this.callback = callback;
            this.lease = lease;
        }

        @Override
        public void binderDied() {
            if (outgoing.remove(entry)) {
                clean();
                lease.release();
            }
        }

        synchronized void clean() {
            wearable.networkHandler.removeCallbacks(timeout);
            if (linked) {
                callback.asBinder().unlinkToDeath(this, 0);
                linked = false;
            }
        }

        synchronized boolean attach() throws RemoteException {
            synchronized (outgoing) {
                if (!outgoing.contains(entry)) return false;
                callback.asBinder().linkToDeath(this, 0);
                linked = true;
                return wearable.networkHandler.postDelayed(timeout, PendingRpcRequests.TIMEOUT_MS);
            }
        }

        void finish(int status, byte[] data) {
            clean();
            Log.d(TAG, "RPC request completed with status " + status);
            // The admission lease bounds both pending requests and blocked callback jobs to 32.
            callbacks.execute(() -> {
                try { deliver(callback, status, entry.requestId, data); }
                finally { lease.release(); }
            });
        }
    }

    private static void deliver(IWearableCallbacks callback, int status, int id, byte[] data) {
        try { callback.onRpcResponse(new RpcResponse(status, id, data)); }
        catch (RemoteException ignored) { }
    }

    private boolean enqueue(Runnable task) {
        if (stopped) return false;
        if (queued.incrementAndGet() > PendingRpcRequests.MAX_PENDING) {
            queued.decrementAndGet();
            return false;
        }
        if (wearable.networkHandler.post(() -> {
            try { task.run(); }
            finally { queued.decrementAndGet(); }
        })) return true;
        queued.decrementAndGet();
        return false;
    }

    void send(String packageName, String node, String path, byte[] data,
              MessageOptions options, IWearableCallbacks callback) {
        if (callback == null) throw new IllegalArgumentException("RPC callback is required");
        if (node == null || node.isEmpty() || !WearableRequests.validRpcPath(path) || data == null
                || data.length > PendingRpcRequests.MAX_PAYLOAD_BYTES || options == null
                || options.priority < 0 || options.priority > 1) {
            deliver(callback, 10, -1, null);
            return;
        }
        String signature = PackageUtils.firstSignatureDigest(context, packageName);
        if (signature == null || signature.isEmpty()) {
            deliver(callback, 10, -1, null);
            return;
        }
        RpcRequestBudget.Lease lease = budget.acquire(packageName);
        if (lease == null) {
            deliver(callback, 8, -1, null);
            return;
        }
        start(packageName, signature, node, path, data.clone(), options.priority, callback, lease);
    }

    private void start(String packageName, String signature, String node, String path, byte[] data,
                       int priority, IWearableCallbacks callback, RpcRequestBudget.Lease lease) {
        if (stopped) {
            lease.release();
            deliver(callback, 16, -1, null);
            return;
        }
        WearableConnection connection = wearable.connectionForNode(node);
        if (connection == null) {
            lease.release();
            deliver(callback, 4000, -1, null);
            return;
        }
        Completion completion = new Completion(callback, lease);
        completion.timeout = () -> {
            if (outgoing.remove(completion.entry)) completion.finish(14, null);
        };
        synchronized (outgoing) {
            completion.entry = stopped ? null : outgoing.reserve(connection, node, packageName, signature, path,
                    completion, SystemClock.elapsedRealtime(), priority);
        }
        if (completion.entry == null) {
            lease.release();
            deliver(callback, 8, -1, null);
            return;
        }
        try {
            if (!completion.attach()) {
                if (outgoing.remove(completion.entry)) completion.finish(16, null);
                return;
            }
            if (!callback.asBinder().isBinderAlive()) {
                completion.binderDied();
                return;
            }
            if (!enqueue(() -> {
                try {
                    if (stopped || !wearable.isCurrentConnection(node, connection)) throw new IOException("RPC disconnected");
                    if (!outgoing.activate(completion.entry, SystemClock.elapsedRealtime(), () -> {
                        completion.message = WearableRequests.rpcRequest(
                                wearable.newRpcEnvelope(packageName, signature, node, path, priority), data, priority);
                        return WearableRequests.rpcId(completion.message.rpcRequestWithResponse);
                    })) {
                        if (outgoing.remove(completion.entry)) completion.finish(14, null);
                        return;
                    }
                    // Do not skip a committed frame if the callback dies now: that would leave a
                    // missing sequence number and delay unrelated requests to the same peer.
                    connection.writeMessage(completion.message);
                    Log.d(TAG, "RPC request sent");
                } catch (IOException | RuntimeException e) {
                    if (outgoing.remove(completion.entry)) completion.finish(8, null);
                }
            })) {
                if (outgoing.remove(completion.entry)) completion.finish(16, null);
            }
        } catch (RemoteException e) {
            if (outgoing.remove(completion.entry)) completion.finish(8, null);
        }
    }

    void receive(WearableConnection connection, String peer, Request request) {
        if (!wearable.isCurrentConnection(peer, connection)
                || !WearableRequests.validRpcEnvelope(request, wearable.getLocalNodeId(), peer)) return;
        if (request.responseRequestId != null) {
            if (Boolean.TRUE.equals(request.requiresResponse)) return;
            enqueue(() -> receiveReply(connection, peer, request));
            return;
        }
        if (!Boolean.TRUE.equals(request.requiresResponse)) return;
        enqueue(() -> incoming(connection, peer, request));
    }

    private void receiveReply(WearableConnection connection, String peer, Request request) {
        if (!wearable.isCurrentConnection(peer, connection)) return;
        PendingRpcRequests.Entry<Completion> entry = outgoing.takeReply(connection, peer, request.packageName,
                request.signatureDigest, request.path, request.responseRequestId,
                request.rawData == null ? 0 : request.rawData.size(), SystemClock.elapsedRealtime(),
                WearableRequests.rpcPriority(request));
        if (entry != null) entry.callback.finish(request.rawData == null ? 8 : 0,
                request.rawData == null ? null : request.rawData.toByteArray());
    }

    private void incoming(WearableConnection connection, String peer, Request request) {
        if (!wearable.isCurrentConnection(peer, connection)
                || !wearable.matchesInstalledSignature(request.packageName, request.signatureDigest)) return;
        RpcRequestBudget.Lease lease = listenerBudget.acquire(request.packageName);
        if (lease == null) return;
        Reply reply = new Reply(lease);
        reply.timeout = () -> reply.complete(false, null);
        synchronized (incoming) {
            reply.entry = stopped ? null : incoming.add(connection, peer, request.packageName, request.signatureDigest,
                    request.path, WearableRequests.rpcId(request), reply, SystemClock.elapsedRealtime(),
                    WearableRequests.rpcPriority(request));
            if (reply.entry == null) {
                lease.release();
                return;
            }
            if (!wearable.networkHandler.postDelayed(reply.timeout, PendingRpcRequests.TIMEOUT_MS)) {
                incoming.remove(reply.entry);
                lease.release();
                return;
            }
        }
        MessageEventParcelable event = new MessageEventParcelable();
        event.requestId = reply.entry.requestId;
        event.path = request.path;
        event.sourceNodeId = peer;
        event.data = request.rawData == null ? null : request.rawData.toByteArray();
        Log.d(TAG, "RPC request received");
        IWearableListener listener = wearable.requestListener(request.packageName, event);
        if (listener != null) reply.dispatch(listener, event);
        else reply.bind(event);
    }

    private final class Reply extends IRpcResponseCallback.Stub implements ServiceConnection {
        final RpcRequestBudget.Lease lease;
        PendingRpcRequests.Entry<Reply> entry;
        Runnable timeout;
        volatile MessageEventParcelable event;
        boolean bound;
        boolean delivering;
        boolean dispatched;

        Reply(RpcRequestBudget.Lease lease) { this.lease = lease; }

        void dispatch(IWearableListener listener, MessageEventParcelable event) {
            synchronized (this) {
                if (dispatched || !incoming.contains(entry)) return;
                dispatched = true;
            }
            try {
                listeners.execute(() -> {
                    synchronized (this) {
                        if (!incoming.contains(entry)) return;
                        delivering = true;
                    }
                    try { listener.onRequestReceived(event, this); }
                    catch (RemoteException e) { enqueue(() -> complete(false, null)); }
                    finally {
                        synchronized (this) {
                            delivering = false;
                            if (!incoming.contains(entry)) lease.release();
                        }
                    }
                });
            } catch (RejectedExecutionException e) {
                enqueue(() -> complete(false, null));
            }
        }

        void bind(MessageEventParcelable event) {
            Intent search = new Intent("com.google.android.gms.wearable.REQUEST_RECEIVED")
                    .setPackage(entry.packageName)
                    .setData(new Uri.Builder().scheme("wear").authority(entry.nodeId).path(entry.path).build());
            ResolveInfo resolved = context.getPackageManager().resolveService(search, 0);
            if (resolved != null && resolved.serviceInfo != null
                    && entry.packageName.equals(resolved.serviceInfo.packageName)) {
                synchronized (this) {
                    if (!incoming.contains(entry)) return;
                    this.event = event;
                    Intent bind = new Intent("com.google.android.gms.wearable.BIND_LISTENER")
                            .setClassName(entry.packageName, resolved.serviceInfo.name);
                    try { bound = context.bindService(bind, this, Context.BIND_AUTO_CREATE); }
                    catch (SecurityException | IllegalArgumentException ignored) { }
                    if (bound) return;
                }
            }
            complete(false, null);
        }

        @Override public void onServiceConnected(ComponentName name, IBinder service) {
            // Android invokes this on the main thread. Never invoke the synchronous RPC there.
            if (incoming.contains(entry)) dispatch(IWearableListener.Stub.asInterface(service), event);
            else unbind();
        }

        @Override public void onServiceDisconnected(ComponentName name) {
            enqueue(() -> complete(false, null));
        }

        @Override public void onBindingDied(ComponentName name) {
            enqueue(() -> complete(false, null));
        }

        @Override public void onNullBinding(ComponentName name) {
            enqueue(() -> complete(false, null));
        }

        synchronized void unbind() {
            if (!delivering) lease.release();
            if (!bound) return;
            bound = false;
            try { context.unbindService(this); } catch (IllegalArgumentException ignored) { }
        }

        @Override
        public void onResponse(boolean success, byte[] data) {
            PackageUtils.getAndCheckCallingPackage(context, entry.packageName);
            if (!wearable.matchesInstalledSignature(entry.packageName, entry.signature)) {
                throw new SecurityException("RPC application signature changed");
            }
            if (data != null && data.length > PendingRpcRequests.MAX_PAYLOAD_BYTES) {
                enqueue(() -> complete(false, null));
                return;
            }
            byte[] copy = data == null ? null : data.clone();
            enqueue(() -> complete(success, copy));
        }

        void complete(boolean success, byte[] data) {
            if (!incoming.remove(entry)) return;
            wearable.networkHandler.removeCallbacks(timeout);
            unbind();
            WearableConnection connection = (WearableConnection) entry.connection;
            if (!wearable.isCurrentConnection(entry.nodeId, connection)) return;
            RootMessage response = WearableRequests.rpcResponse(wearable.newRpcEnvelope(entry.packageName, entry.signature,
                    entry.nodeId, entry.path, entry.priority), entry.requestId, success ? data : null, entry.priority);
            try { connection.writeMessage(response); }
            catch (IOException e) { Log.d(TAG, "RPC response transport disconnected"); }
        }
    }

    void disconnected(WearableConnection connection) {
        for (PendingRpcRequests.Entry<Completion> entry : outgoing.removeConnection(connection)) {
            entry.callback.finish(4000, null);
        }
        for (PendingRpcRequests.Entry<Reply> entry : incoming.removeConnection(connection)) {
            wearable.networkHandler.removeCallbacks(entry.callback.timeout);
            entry.callback.unbind();
        }
    }

    void stop() {
        stopped = true;
        for (PendingRpcRequests.Entry<Completion> entry : outgoing.removeAll()) entry.callback.finish(16, null);
        for (PendingRpcRequests.Entry<Reply> entry : incoming.removeAll()) {
            wearable.networkHandler.removeCallbacks(entry.callback.timeout);
            entry.callback.unbind();
        }
    }
}
