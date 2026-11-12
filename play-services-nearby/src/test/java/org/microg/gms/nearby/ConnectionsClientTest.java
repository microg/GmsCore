/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby;
import android.content.Context;
import android.content.ContextWrapper;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.gms.tasks.Task;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConnectionsClientTest {
    private Context createMockContext() {
        return new ContextWrapper(null) {
            @Override
            public Context getApplicationContext() {
                return this;
            }
            @Override
            public String getPackageName() {
                return "com.google.android.gms";
            }
        };
    }

    /**
     * Recording fake for ConnectionsTransport. In immediate mode every
     * call completes with STATUS_OK (unless its endpoint is in
     * failEndpoints); in manual mode the ResultCallback is stored so a
     * test can prove the Task stays incomplete until the transport
     * really reports, then complete or fail it on demand.
     */
    static class FakeTransport implements ConnectionsTransport {
        boolean manual = false;
        final Set<String> failEndpoints = new HashSet<>();
        final Map<String, ResultCallback> pending = new HashMap<>();
        final List<String> singleSendEndpoints = new ArrayList<>();

        String lastNickname;
        String lastServiceId;
        String lastEndpointId;
        byte[] lastEndpointInfo;
        AdvertisingOptions lastAdvertisingOptions;
        DiscoveryOptions lastDiscoveryOptions;
        Payload lastPayload;
        long lastPayloadId = -1;
        boolean disconnectCalled = false;
        boolean stopAllCalled = false;
        boolean listSendCalled = false;

        void complete(String method, int statusCode) {
            ResultCallback callback = pending.remove(method);
            Assert.assertNotNull("No pending callback for " + method, callback);
            callback.onResult(statusCode);
        }

        private void finish(String method, ResultCallback result, String endpointId) {
            if (manual) {
                pending.put(method, result);
            } else if (endpointId != null && failEndpoints.contains(endpointId)) {
                result.onResult(ConnectionsStatusCodes.STATUS_ENDPOINT_IO_ERROR);
            } else {
                result.onResult(ConnectionsStatusCodes.STATUS_OK);
            }
        }

        @Override
        public void startAdvertising(String userNickName, String serviceId, ConnectionLifecycleCallback lifecycle, AdvertisingOptions options, ResultCallback result) {
            lastNickname = userNickName;
            lastServiceId = serviceId;
            lastAdvertisingOptions = options;
            finish("startAdvertising", result, null);
        }

        @Override
        public void stopAdvertising(ResultCallback result) {
            finish("stopAdvertising", result, null);
        }

        @Override
        public void startDiscovery(String serviceId, EndpointDiscoveryCallback discovery, DiscoveryOptions options, ResultCallback result) {
            lastServiceId = serviceId;
            lastDiscoveryOptions = options;
            finish("startDiscovery", result, null);
        }

        @Override
        public void stopDiscovery(ResultCallback result) {
            finish("stopDiscovery", result, null);
        }

        @Override
        public void requestConnection(String userNickName, String endpointId, byte[] endpointInfo, ConnectionLifecycleCallback lifecycle, ResultCallback result) {
            lastNickname = userNickName;
            lastEndpointId = endpointId;
            lastEndpointInfo = endpointInfo;
            finish("requestConnection", result, endpointId);
        }

        @Override
        public void acceptConnection(String endpointId, PayloadCallback payload, ResultCallback result) {
            lastEndpointId = endpointId;
            finish("acceptConnection", result, endpointId);
        }

        @Override
        public void rejectConnection(String endpointId, ResultCallback result) {
            lastEndpointId = endpointId;
            finish("rejectConnection", result, endpointId);
        }

        @Override
        public void sendPayload(String endpointId, Payload payload, ResultCallback result) {
            singleSendEndpoints.add(endpointId);
            lastEndpointId = endpointId;
            lastPayload = payload;
            finish("sendPayload", result, endpointId);
        }

        @Override
        public void sendPayload(List<String> endpointIds, Payload payload, ResultCallback result) {
            listSendCalled = true;
            lastPayload = payload;
            finish("sendPayloadList", result, null);
        }

        @Override
        public void cancelPayload(long payloadId, ResultCallback result) {
            lastPayloadId = payloadId;
            finish("cancelPayload", result, null);
        }

        @Override
        public void disconnectFromEndpoint(String endpointId) {
            lastEndpointId = endpointId;
            disconnectCalled = true;
        }

        @Override
        public void stopAllEndpoints() {
            stopAllCalled = true;
        }
    }

    @Test
    public void testGetConnectionsClient() {
        ConnectionsClient client = Nearby.getConnectionsClient(createMockContext());
        Assert.assertNotNull(client);
    }

    @Test
    public void testStrategyConstants() {
        Assert.assertNotNull(Strategy.P2P_CLUSTER);
        Assert.assertNotNull(Strategy.P2P_STAR);
        Assert.assertNotNull(Strategy.P2P_POINT_TO_POINT);
        Assert.assertNotEquals(Strategy.P2P_CLUSTER, Strategy.P2P_STAR);
        Assert.assertNotEquals(Strategy.P2P_STAR, Strategy.P2P_POINT_TO_POINT);
    }

    @Test
    public void testAdvertisingOptionsBuilder() {
        AdvertisingOptions options = new AdvertisingOptions.Builder()
                .setStrategy(Strategy.P2P_STAR)
                .build();
        Assert.assertNotNull(options);
        Assert.assertEquals(Strategy.P2P_STAR, options.getStrategy());
    }

    @Test
    public void testDiscoveryOptionsBuilder() {
        DiscoveryOptions options = new DiscoveryOptions.Builder()
                .setStrategy(Strategy.P2P_POINT_TO_POINT)
                .build();
        Assert.assertNotNull(options);
        Assert.assertEquals(Strategy.P2P_POINT_TO_POINT, options.getStrategy());
    }

    @Test
    public void testPayloadFromBytes() {
        byte[] data = "Quick Share Test Payload".getBytes(StandardCharsets.UTF_8);
        Payload payload = Payload.fromBytes(data);
        Assert.assertNotNull(payload);
        Assert.assertEquals(Payload.Type.BYTES, payload.getType());
        Assert.assertArrayEquals(data, payload.asBytes());
    }

    @Test
    public void testConnectionsStatusCodes() {
        Assert.assertEquals("STATUS_OK", ConnectionsStatusCodes.getStatusCodeString(ConnectionsStatusCodes.STATUS_OK));
        Assert.assertEquals("STATUS_NETWORK_NOT_CONNECTED", ConnectionsStatusCodes.getStatusCodeString(ConnectionsStatusCodes.STATUS_NETWORK_NOT_CONNECTED));
        Assert.assertEquals("STATUS_ALREADY_ADVERTISING", ConnectionsStatusCodes.getStatusCodeString(ConnectionsStatusCodes.STATUS_ALREADY_ADVERTISING));
        Assert.assertEquals("STATUS_ALREADY_DISCOVERING", ConnectionsStatusCodes.getStatusCodeString(ConnectionsStatusCodes.STATUS_ALREADY_DISCOVERING));
        Assert.assertEquals("STATUS_CONNECTION_REJECTED", ConnectionsStatusCodes.getStatusCodeString(ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED));
    }

    @Test
    public void testStartAdvertisingDrivesTransportAndCompletesOnlyOnRealResult() {
        FakeTransport fake = new FakeTransport();
        fake.manual = true;
        ConnectionsClient client = new ConnectionsClientImpl(createMockContext(), fake);
        AdvertisingOptions options = new AdvertisingOptions.Builder().setStrategy(Strategy.P2P_STAR).build();
        Task<Void> task = client.startAdvertising("User", "serviceId", null, options);
        // The call reached the transport with the right arguments...
        Assert.assertEquals("User", fake.lastNickname);
        Assert.assertEquals("serviceId", fake.lastServiceId);
        Assert.assertEquals(Strategy.P2P_STAR, fake.lastAdvertisingOptions.getStrategy());
        // ...but the Task must NOT complete until the transport reports.
        Assert.assertFalse(task.isComplete());
        fake.complete("startAdvertising", ConnectionsStatusCodes.STATUS_OK);
        Assert.assertTrue(task.isSuccessful());
    }

    @Test
    public void testFailedTransportFailsTaskInsteadOfFakeSuccess() {
        FakeTransport fake = new FakeTransport();
        fake.manual = true;
        ConnectionsClient client = new ConnectionsClientImpl(createMockContext(), fake);
        Task<Void> task = client.startDiscovery("serviceId", null, new DiscoveryOptions.Builder().build());
        Assert.assertFalse(task.isComplete());
        fake.complete("startDiscovery", ConnectionsStatusCodes.STATUS_ENDPOINT_IO_ERROR);
        Assert.assertTrue(task.isComplete());
        Assert.assertFalse(task.isSuccessful());
        Assert.assertTrue(task.getException() instanceof ApiException);
        Assert.assertEquals(ConnectionsStatusCodes.STATUS_ENDPOINT_IO_ERROR, ((ApiException) task.getException()).getStatusCode());
    }

    @Test
    public void testRequestConnectionBytesOverloadForwardsEndpointInfo() {
        FakeTransport fake = new FakeTransport();
        ConnectionsClient client = new ConnectionsClientImpl(createMockContext(), fake);
        byte[] endpointInfo = new byte[]{9, 8, 7};
        Task<Void> task = client.requestConnection(endpointInfo, "endpointId", null);
        Assert.assertEquals("endpointId", fake.lastEndpointId);
        Assert.assertArrayEquals(endpointInfo, fake.lastEndpointInfo);
        Assert.assertTrue(task.isSuccessful());
    }

    @Test
    public void testSendPayloadForwardsBytesAndType() {
        FakeTransport fake = new FakeTransport();
        ConnectionsClient client = new ConnectionsClientImpl(createMockContext(), fake);
        byte[] data = new byte[]{1, 2, 3};
        Task<Void> task = client.sendPayload("endpointId", Payload.fromBytes(data));
        Assert.assertEquals("endpointId", fake.lastEndpointId);
        Assert.assertEquals(Payload.Type.BYTES, fake.lastPayload.getType());
        Assert.assertArrayEquals(data, fake.lastPayload.asBytes());
        Assert.assertTrue(task.isSuccessful());
    }

    @Test
    public void testSendPayloadListFansOutAndFailsFast() {
        FakeTransport fake = new FakeTransport();
        ConnectionsClient client = new ConnectionsClientImpl(createMockContext(), fake);
        Task<Void> ok = client.sendPayload(Arrays.asList("ep1", "ep2"), Payload.fromBytes(new byte[]{1}));
        Assert.assertFalse(fake.listSendCalled);
        Assert.assertEquals(Arrays.asList("ep1", "ep2"), fake.singleSendEndpoints);
        Assert.assertTrue(ok.isSuccessful());

        fake.singleSendEndpoints.clear();
        fake.failEndpoints.add("ep2");
        Task<Void> failed = client.sendPayload(Arrays.asList("ep1", "ep2"), Payload.fromBytes(new byte[]{1}));
        Assert.assertEquals(Arrays.asList("ep1", "ep2"), fake.singleSendEndpoints);
        Assert.assertTrue(failed.isComplete());
        Assert.assertFalse(failed.isSuccessful());
    }

    @Test
    public void testAcceptRejectCancelDisconnectStopAllDelegate() {
        FakeTransport fake = new FakeTransport();
        ConnectionsClient client = new ConnectionsClientImpl(createMockContext(), fake);
        Assert.assertTrue(client.acceptConnection("endpointId", null).isSuccessful());
        Assert.assertEquals("endpointId", fake.lastEndpointId);
        Assert.assertTrue(client.rejectConnection("endpointId").isSuccessful());
        Assert.assertTrue(client.cancelPayload(12345L).isSuccessful());
        Assert.assertEquals(12345L, fake.lastPayloadId);
        client.disconnectFromEndpoint("endpointId");
        Assert.assertTrue(fake.disconnectCalled);
        client.stopAllEndpoints();
        Assert.assertTrue(fake.stopAllCalled);
    }

    @Test
    public void testNullCallbacksAndOptionsDoNotCrash() {
        FakeTransport fake = new FakeTransport();
        ConnectionsClient client = new ConnectionsClientImpl(createMockContext(), fake);
        Assert.assertTrue(client.startAdvertising("User", "serviceId", null, null).isSuccessful());
        Assert.assertNotNull(fake.lastAdvertisingOptions);
        Assert.assertTrue(client.startDiscovery("serviceId", null, null).isSuccessful());
        Assert.assertNotNull(fake.lastDiscoveryOptions);
    }
}
