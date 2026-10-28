/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby;

import android.app.Activity;
import android.content.Context;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.internal.IConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.internal.IConnectionsResultCallback;
import com.google.android.gms.nearby.connection.internal.IEndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.internal.IPayloadCallback;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;

import org.microg.gms.common.api.PendingGoogleApiCall;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Real ConnectionsClient: every method drives the GmsCore Nearby service
 * through ConnectionsTransport instead of returning fake success. The
 * production transport binds to NearbySharingService via the standard
 * GmsClient/GoogleApi machinery (see ServiceTransport); unit tests inject
 * a fake transport through the package-private constructor.
 *
 * Task semantics: a Task completes successfully only when the service
 * reports STATUS_OK for that operation; any other status becomes an
 * ApiException. Callbacks arrive on Binder threads and are forwarded
 * directly to the app callbacks.
 */
public class ConnectionsClientImpl extends GoogleApi<Api.ApiOptions.NoOptions> implements ConnectionsClient {
    private static final String TAG = "ConnectionsClientImpl";
    private static final Api<Api.ApiOptions.NoOptions> API = new Api<>((options, context, looper, clientSettings, callbacks, connectionFailedListener) -> new ConnectionsApiClient(context, callbacks, connectionFailedListener));

    private final ConnectionsTransport transport;

    public ConnectionsClientImpl(Context context) {
        super(context, API, Api.ApiOptions.NO_OPTIONS);
        this.transport = new ServiceTransport();
    }

    public ConnectionsClientImpl(Activity activity) {
        super(activity, API, Api.ApiOptions.NO_OPTIONS);
        this.transport = new ServiceTransport();
    }

    ConnectionsClientImpl(Context context, ConnectionsTransport transport) {
        super(context, API, Api.ApiOptions.NO_OPTIONS);
        this.transport = transport;
    }

    private static ConnectionsTransport.ResultCallback once(final ConnectionsTransport.ResultCallback result) {
        final AtomicBoolean done = new AtomicBoolean(false);
        return statusCode -> {
            if (done.compareAndSet(false, true)) {
                result.onResult(statusCode);
            }
        };
    }

    private Task<Void> run(TransportCall call) {
        final TaskCompletionSource<Void> source = new TaskCompletionSource<>();
        ConnectionsTransport.ResultCallback result = once(statusCode -> {
            if (statusCode == ConnectionsStatusCodes.STATUS_OK) {
                source.setResult(null);
            } else {
                source.setException(new ApiException(new Status(statusCode)));
            }
        });
        try {
            call.execute(result);
        } catch (Exception e) {
            source.setException(e);
        }
        return source.getTask();
    }

    private interface TransportCall {
        void execute(ConnectionsTransport.ResultCallback result) throws Exception;
    }

    @Override
    public Task<Void> startAdvertising(String userNickName, String serviceId, ConnectionLifecycleCallback connectionLifecycleCallback, AdvertisingOptions advertisingOptions) {
        final AdvertisingOptions options = advertisingOptions != null ? advertisingOptions : new AdvertisingOptions.Builder().build();
        return run(result -> transport.startAdvertising(userNickName, serviceId, connectionLifecycleCallback, options, result));
    }

    @Override
    public Task<Void> stopAdvertising() {
        return run(result -> transport.stopAdvertising(result));
    }

    @Override
    public Task<Void> startDiscovery(String serviceId, EndpointDiscoveryCallback endpointDiscoveryCallback, DiscoveryOptions discoveryOptions) {
        final DiscoveryOptions options = discoveryOptions != null ? discoveryOptions : new DiscoveryOptions.Builder().build();
        return run(result -> transport.startDiscovery(serviceId, endpointDiscoveryCallback, options, result));
    }

    @Override
    public Task<Void> stopDiscovery() {
        return run(result -> transport.stopDiscovery(result));
    }

    @Override
    public Task<Void> requestConnection(String userNickName, String endpointId, ConnectionLifecycleCallback connectionLifecycleCallback) {
        return run(result -> transport.requestConnection(userNickName, endpointId, null, connectionLifecycleCallback, result));
    }

    @Override
    public Task<Void> requestConnection(byte[] endpointInfo, String endpointId, ConnectionLifecycleCallback connectionLifecycleCallback) {
        return run(result -> transport.requestConnection(null, endpointId, endpointInfo, connectionLifecycleCallback, result));
    }

    @Override
    public Task<Void> acceptConnection(String endpointId, PayloadCallback payloadCallback) {
        return run(result -> transport.acceptConnection(endpointId, payloadCallback, result));
    }

    @Override
    public Task<Void> rejectConnection(String endpointId) {
        return run(result -> transport.rejectConnection(endpointId, result));
    }

    @Override
    public Task<Void> sendPayload(String endpointId, Payload payload) {
        return run(result -> transport.sendPayload(endpointId, payload, result));
    }

    @Override
    public Task<Void> sendPayload(List<String> endpointIds, Payload payload) {
        if (endpointIds == null || endpointIds.isEmpty()) {
            return Tasks.forResult(null);
        }
        final TaskCompletionSource<Void> source = new TaskCompletionSource<>();
        final Iterator<String> remaining = endpointIds.iterator();
        sendNextPayload(remaining, payload, source);
        return source.getTask();
    }

    private void sendNextPayload(final Iterator<String> remaining, final Payload payload, final TaskCompletionSource<Void> source) {
        if (!remaining.hasNext()) {
            source.setResult(null);
            return;
        }
        final String endpointId = remaining.next();
        transport.sendPayload(endpointId, payload, once(statusCode -> {
            if (statusCode == ConnectionsStatusCodes.STATUS_OK) {
                sendNextPayload(remaining, payload, source);
            } else {
                source.setException(new ApiException(new Status(statusCode)));
            }
        }));
    }

    @Override
    public Task<Void> cancelPayload(long payloadId) {
        return run(result -> transport.cancelPayload(payloadId, result));
    }

    @Override
    public void disconnectFromEndpoint(String endpointId) {
        transport.disconnectFromEndpoint(endpointId);
    }

    @Override
    public void stopAllEndpoints() {
        transport.stopAllEndpoints();
    }

    /**
     * Production transport: forwards every call to the GmsCore service
     * through the GoogleApi task machinery, translating AIDL callbacks
     * back into app callbacks and Task outcomes.
     *
     * Dispatch lifecycle, following the Exposure Notification client:
     * the dispatch source is completed from the result callback itself,
     * never at dispatch, so a call stays pending in the manager until
     * the service reports. A manager failure of the call (for example a
     * connection failure while the call was queued) is translated to
     * the method's failure status, so the app Task fails instead of
     * hanging. Residual gap, deliberately not bundled here: if the
     * service process dies after a successful dispatch but before
     * reporting, the app Task can still hang in this manager snapshot,
     * because GoogleApiManager does not track executed calls and its
     * onInstanceSuspended is empty; fixing that needs a manager-level
     * change to shared machinery.
     */
    private class ServiceTransport implements ConnectionsTransport {
        private interface ResultCall {
            void execute(ConnectionsApiClient client, IConnectionsResultCallback result) throws Exception;
        }

        private void scheduleResultCall(ResultCall call, ResultCallback result, int failureStatus) {
            scheduleTask((PendingGoogleApiCall<Void, ConnectionsApiClient>) (client, completionSource) -> {
                AtomicBoolean reported = new AtomicBoolean(false);
                IConnectionsResultCallback callback = new IConnectionsResultCallback.Stub() {
                    @Override
                    public void onResult(int statusCode) throws RemoteException {
                        if (reported.compareAndSet(false, true)) {
                            completionSource.setResult(null);
                        }
                        result.onResult(statusCode);
                    }
                };
                try {
                    call.execute(client, callback);
                } catch (Exception e) {
                    if (reported.compareAndSet(false, true)) {
                        completionSource.setException(e);
                    }
                    result.onResult(failureStatus);
                }
            }).addOnFailureListener(e -> result.onResult(failureStatus));
        }

        @Override
        public void startAdvertising(String userNickName, String serviceId, ConnectionLifecycleCallback lifecycle, AdvertisingOptions options, ResultCallback result) {
            scheduleResultCall((client, callback) -> client.startAdvertising(userNickName, serviceId, wrapLifecycle(lifecycle), callback), result, ConnectionsStatusCodes.STATUS_ENDPOINT_IO_ERROR);
        }

        @Override
        public void stopAdvertising(ResultCallback result) {
            scheduleResultCall((client, callback) -> client.stopAdvertising(callback), result, ConnectionsStatusCodes.STATUS_ENDPOINT_IO_ERROR);
        }

        @Override
        public void startDiscovery(String serviceId, EndpointDiscoveryCallback discovery, DiscoveryOptions options, ResultCallback result) {
            scheduleResultCall((client, callback) -> client.startDiscovery(serviceId, wrapDiscovery(discovery), callback), result, ConnectionsStatusCodes.STATUS_ENDPOINT_IO_ERROR);
        }

        @Override
        public void stopDiscovery(ResultCallback result) {
            scheduleResultCall((client, callback) -> client.stopDiscovery(callback), result, ConnectionsStatusCodes.STATUS_ENDPOINT_IO_ERROR);
        }

        @Override
        public void requestConnection(String userNickName, String endpointId, byte[] endpointInfo, ConnectionLifecycleCallback lifecycle, ResultCallback result) {
            scheduleResultCall((client, callback) -> client.requestConnection(userNickName, endpointId, endpointInfo, wrapLifecycle(lifecycle), callback), result, ConnectionsStatusCodes.STATUS_ENDPOINT_IO_ERROR);
        }

        @Override
        public void acceptConnection(String endpointId, PayloadCallback payload, ResultCallback result) {
            scheduleResultCall((client, callback) -> client.acceptConnection(endpointId, wrapPayload(payload), callback), result, ConnectionsStatusCodes.STATUS_ENDPOINT_IO_ERROR);
        }

        @Override
        public void rejectConnection(String endpointId, ResultCallback result) {
            scheduleResultCall((client, callback) -> client.rejectConnection(endpointId, callback), result, ConnectionsStatusCodes.STATUS_ENDPOINT_IO_ERROR);
        }

        @Override
        public void sendPayload(String endpointId, Payload payload, ResultCallback result) {
            scheduleResultCall((client, callback) -> {
                ParcelFileDescriptor pfdToClose = null;
                try {
                    byte[] bytes = null;
                    ParcelFileDescriptor pfd = null;
                    if (payload.getType() == Payload.Type.BYTES) {
                        bytes = payload.asBytes();
                    } else if (payload.getType() == Payload.Type.FILE) {
                        pfd = payload.asFile().asParcelFileDescriptor();
                    } else if (payload.getType() == Payload.Type.STREAM) {
                        ParcelFileDescriptor appPfd = payload.asStream().asParcelFileDescriptor();
                        if (appPfd != null) {
                            pfd = appPfd;
                        } else {
                            pfd = streamToPipe(payload.asStream().asInputStream());
                            pfdToClose = pfd;
                        }
                    }
                    client.sendPayload(endpointId, payload.getId(), payload.getType(), bytes, pfd, callback);
                } finally {
                    if (pfdToClose != null) {
                        try {
                            pfdToClose.close();
                        } catch (IOException ignored) {
                        }
                    }
                }
            }, result, ConnectionsStatusCodes.STATUS_PAYLOAD_IO_ERROR);
        }

        @Override
        public void sendPayload(List<String> endpointIds, Payload payload, ResultCallback result) {
            // The impl fans out list sends one endpoint at a time; this
            // transport only ever receives single-endpoint sends.
            throw new UnsupportedOperationException("List sends are fanned out by ConnectionsClientImpl");
        }

        @Override
        public void cancelPayload(long payloadId, ResultCallback result) {
            scheduleResultCall((client, callback) -> client.cancelPayload(payloadId, callback), result, ConnectionsStatusCodes.STATUS_PAYLOAD_IO_ERROR);
        }

        @Override
        public void disconnectFromEndpoint(String endpointId) {
            scheduleTask((PendingGoogleApiCall<Void, ConnectionsApiClient>) (client, completionSource) -> {
                try {
                    client.disconnectFromEndpoint(endpointId);
                    completionSource.setResult(null);
                } catch (Exception e) {
                    completionSource.setException(e);
                }
            });
        }

        @Override
        public void stopAllEndpoints() {
            scheduleTask((PendingGoogleApiCall<Void, ConnectionsApiClient>) (client, completionSource) -> {
                try {
                    client.stopAllEndpoints();
                    completionSource.setResult(null);
                } catch (Exception e) {
                    completionSource.setException(e);
                }
            });
        }

        private IConnectionLifecycleCallback wrapLifecycle(final ConnectionLifecycleCallback lifecycle) {
            if (lifecycle == null) return null;
            return new IConnectionLifecycleCallback.Stub() {
                @Override
                public void onConnectionInitiated(String endpointId, String endpointName, String authenticationToken, byte[] endpointInfo, boolean isIncoming) throws RemoteException {
                    lifecycle.onConnectionInitiated(endpointId, new ConnectionInfo(endpointName, authenticationToken, endpointInfo, isIncoming));
                }

                @Override
                public void onConnectionResult(String endpointId, int statusCode) throws RemoteException {
                    lifecycle.onConnectionResult(endpointId, new ConnectionResolution(new Status(statusCode)));
                }

                @Override
                public void onDisconnected(String endpointId) throws RemoteException {
                    lifecycle.onDisconnected(endpointId);
                }
            };
        }

        private IEndpointDiscoveryCallback wrapDiscovery(final EndpointDiscoveryCallback discovery) {
            if (discovery == null) return null;
            return new IEndpointDiscoveryCallback.Stub() {
                @Override
                public void onEndpointFound(String endpointId, String serviceId, String endpointName, byte[] endpointInfo) throws RemoteException {
                    discovery.onEndpointFound(endpointId, new DiscoveredEndpointInfo(serviceId, endpointName, endpointInfo));
                }

                @Override
                public void onEndpointLost(String endpointId) throws RemoteException {
                    discovery.onEndpointLost(endpointId);
                }
            };
        }

        private IPayloadCallback wrapPayload(final PayloadCallback payload) {
            if (payload == null) return null;
            return new IPayloadCallback.Stub() {
                @Override
                public void onPayloadReceived(String endpointId, long payloadId, int payloadType, byte[] bytes, ParcelFileDescriptor pfd) throws RemoteException {
                    Payload received;
                    if (payloadType == Payload.Type.FILE && pfd != null) {
                        received = Payload.fromFile(pfd);
                    } else if (payloadType == Payload.Type.STREAM && pfd != null) {
                        received = Payload.fromStream(pfd);
                    } else if (bytes != null) {
                        received = Payload.fromBytes(bytes);
                    } else {
                        Log.w(TAG, "Dropping payload " + payloadId + " with no bytes and no file descriptor");
                        return;
                    }
                    payload.onPayloadReceived(endpointId, received);
                }

                @Override
                public void onPayloadTransferUpdate(String endpointId, long payloadId, int status, long bytesTransferred, long totalBytes) throws RemoteException {
                    payload.onPayloadTransferUpdate(endpointId, new PayloadTransferUpdate(payloadId, status, totalBytes, bytesTransferred));
                }
            };
        }

        /**
         * STREAM payloads backed by an InputStream cannot cross Binder
         * directly, so pump the stream into a pipe and hand the read end
         * to the service. The caller closes the read end after the call.
         */
        private ParcelFileDescriptor streamToPipe(InputStream in) throws IOException {
            if (in == null) {
                throw new IOException("Stream payload has neither InputStream nor ParcelFileDescriptor");
            }
            ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
            final ParcelFileDescriptor.AutoCloseOutputStream out = new ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]);
            Thread pump = new Thread(() -> {
                try {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                } catch (IOException ignored) {
                } finally {
                    try {
                        out.close();
                    } catch (IOException ignored) {
                    }
                }
            });
            pump.setDaemon(true);
            pump.start();
            return pipe[0];
        }
    }
}
