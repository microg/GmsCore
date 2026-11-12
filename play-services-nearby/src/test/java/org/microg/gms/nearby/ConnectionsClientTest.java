/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby;
import android.content.Context;
import android.content.ContextWrapper;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.gms.tasks.Task;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

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
    public void testConnectionsClientMethodsReturnCompletedTasks() {
        ConnectionsClient client = new ConnectionsClientImpl(createMockContext());
        Task<Void> startAdv = client.startAdvertising("User", "serviceId", null, new AdvertisingOptions.Builder().build());
        Assert.assertNotNull(startAdv);
        Assert.assertTrue(startAdv.isSuccessful());

        Task<Void> stopAdv = client.stopAdvertising();
        Assert.assertNotNull(stopAdv);
        Assert.assertTrue(stopAdv.isSuccessful());

        Task<Void> startDisc = client.startDiscovery("serviceId", null, new DiscoveryOptions.Builder().build());
        Assert.assertNotNull(startDisc);
        Assert.assertTrue(startDisc.isSuccessful());

        Task<Void> stopDisc = client.stopDiscovery();
        Assert.assertNotNull(stopDisc);
        Assert.assertTrue(stopDisc.isSuccessful());

        Task<Void> reqConn = client.requestConnection("User", "endpointId", null);
        Assert.assertNotNull(reqConn);
        Assert.assertTrue(reqConn.isSuccessful());

        Task<Void> acceptConn = client.acceptConnection("endpointId", null);
        Assert.assertNotNull(acceptConn);
        Assert.assertTrue(acceptConn.isSuccessful());

        Task<Void> rejectConn = client.rejectConnection("endpointId");
        Assert.assertNotNull(rejectConn);
        Assert.assertTrue(rejectConn.isSuccessful());

        Task<Void> sendPayload = client.sendPayload("endpointId", Payload.fromBytes(new byte[]{1, 2, 3}));
        Assert.assertNotNull(sendPayload);
        Assert.assertTrue(sendPayload.isSuccessful());

        Task<Void> sendPayloadList = client.sendPayload(Collections.singletonList("endpointId"), Payload.fromBytes(new byte[]{1, 2, 3}));
        Assert.assertNotNull(sendPayloadList);
        Assert.assertTrue(sendPayloadList.isSuccessful());

        Task<Void> cancelPayload = client.cancelPayload(12345L);
        Assert.assertNotNull(cancelPayload);
        Assert.assertTrue(cancelPayload.isSuccessful());

        client.disconnectFromEndpoint("endpointId");
        client.stopAllEndpoints();
    }
}
