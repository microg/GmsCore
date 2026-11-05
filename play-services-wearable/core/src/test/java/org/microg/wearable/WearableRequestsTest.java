/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.wearable;

import org.junit.Test;
import org.microg.wearable.proto.Request;
import org.microg.wearable.proto.ChannelControlRequest;
import org.microg.wearable.proto.ChannelDataHeader;
import okio.ByteString;
import static org.junit.Assert.*;

public class WearableRequestsTest {
    @Test public void serializesAllRequiredFieldsEvenWhenZeroOrEmpty() throws Exception {
        Request sent = WearableRequests.envelope("package", "signature", "watch", "phone", "", 0, 0).build();
        Request received = Request.ADAPTER.decode(Request.ADAPTER.encode(sent));
        assertEquals(Integer.valueOf(0), received.requestId);
        assertEquals("package", received.packageName);
        assertEquals("signature", received.signatureDigest);
        assertEquals("watch", received.targetNodeId);
        assertEquals(Integer.valueOf(0), received.unknown5);
        assertEquals("", received.path);
        assertEquals(Integer.valueOf(0), received.generation);
    }

    @Test public void rejectsMissingRequiredStrings() {
        assertThrows(IllegalArgumentException.class, () -> WearableRequests.envelope(
                "package", "signature", "watch", "phone", null, 0, 0));
    }

    @Test public void channelIdsUseFixed64WireType() throws Exception {
        long id = 0x0102030405060708L;
        assertEquals("110807060504030201", ByteString.of(ChannelControlRequest.ADAPTER.encode(
                new ChannelControlRequest.Builder().channelId(id).build())).hex());
        assertEquals("090807060504030201", ByteString.of(ChannelDataHeader.ADAPTER.encode(
                new ChannelDataHeader.Builder().channelId(id).build())).hex());
        assertEquals(Long.valueOf(id), ChannelControlRequest.ADAPTER.decode(
                ByteString.decodeHex("110807060504030201")).channelId);
    }
}
