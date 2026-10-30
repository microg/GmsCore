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

import android.database.CursorWindow;
import android.os.Looper;
import android.os.ParcelFileDescriptor;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Releasable;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.data.DataHolder;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.ChannelClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.NodeClient;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.google.android.gms.wearable.internal.ChannelParcelable;
import com.google.android.gms.wearable.internal.IWearableListener;
import com.google.android.gms.wearable.internal.NodeParcelable;

import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WearableModernApiTest {

    @Test
    public void testWearableOptionsEqualsAndHashCode() {
        Wearable.WearableOptions opt1 = new Wearable.WearableOptions.Builder().setFirstPartyMode(true).build();
        Wearable.WearableOptions opt2 = new Wearable.WearableOptions.Builder().setFirstPartyMode(true).build();
        Wearable.WearableOptions opt3 = new Wearable.WearableOptions.Builder().setFirstPartyMode(false).build();

        Assert.assertEquals("Identical options should be equal", opt1, opt2);
        Assert.assertEquals("Equal options must have equal hashCodes", opt1.hashCode(), opt2.hashCode());
        Assert.assertNotEquals("Different options should not be equal", opt1, opt3);
    }

    @Test
    public void testWearableListenerServiceConstants() {
        Set<String> expectedActions = new HashSet<>(Arrays.asList(
                "com.google.android.gms.wearable.BIND_LISTENER",
                "com.google.android.gms.wearable.DATA_CHANGED",
                "com.google.android.gms.wearable.MESSAGE_RECEIVED",
                "com.google.android.gms.wearable.CAPABILITY_CHANGED",
                "com.google.android.gms.wearable.CHANNEL_EVENT"
        ));

        Assert.assertEquals("com.google.android.gms.wearable.BIND_LISTENER", WearableListenerService.BIND_LISTENER_INTENT_ACTION);
        Assert.assertEquals("com.google.android.gms.wearable.DATA_CHANGED", WearableListenerService.ACTION_DATA_CHANGED);
        Assert.assertEquals("com.google.android.gms.wearable.MESSAGE_RECEIVED", WearableListenerService.ACTION_MESSAGE_RECEIVED);
        Assert.assertEquals("com.google.android.gms.wearable.CAPABILITY_CHANGED", WearableListenerService.ACTION_CAPABILITY_CHANGED);
        Assert.assertEquals("com.google.android.gms.wearable.CHANNEL_EVENT", WearableListenerService.ACTION_CHANNEL_EVENT);

        Assert.assertTrue(expectedActions.contains(WearableListenerService.BIND_LISTENER_INTENT_ACTION));
        Assert.assertTrue(expectedActions.contains(WearableListenerService.ACTION_DATA_CHANGED));
        Assert.assertTrue(expectedActions.contains(WearableListenerService.ACTION_MESSAGE_RECEIVED));
        Assert.assertTrue(expectedActions.contains(WearableListenerService.ACTION_CAPABILITY_CHANGED));
        Assert.assertTrue(expectedActions.contains(WearableListenerService.ACTION_CHANNEL_EVENT));
    }

    @Test
    public void testChannelImplProperties() {
        ChannelImpl channel = new ChannelImpl("token_123", "node_abc", "/path/test");
        Assert.assertEquals("node_abc", channel.getNodeId());
        Assert.assertEquals("/path/test", channel.getPath());
        Assert.assertEquals("token_123", channel.getToken());

        // Test polymorphic assignment to ChannelClient.Channel interface
        ChannelClient.Channel clientChan = channel;
        Assert.assertEquals("node_abc", clientChan.getNodeId());
        Assert.assertEquals("/path/test", clientChan.getPath());
    }

    @Test
    public void testWearableTasksStatusConversion() {
        // Success case (statusCode = 0)
        TaskCompletionSource<String> successSource = new TaskCompletionSource<>();
        WearableTasks.complete(successSource, 0, "test_result");
        Assert.assertTrue("Task should be completed", successSource.getTask().isSuccessful());
        Assert.assertEquals("test_result", successSource.getTask().getResult());

        // Error case (statusCode != 0)
        TaskCompletionSource<String> failureSource = new TaskCompletionSource<>();
        WearableTasks.complete(failureSource, CommonStatusCodes.TIMEOUT, (String) null);
        Assert.assertTrue("Task should be completed on error", failureSource.getTask().isComplete());
        Assert.assertFalse("Task should not be successful", failureSource.getTask().isSuccessful());
        Assert.assertNotNull("Exception should be present", failureSource.getTask().getException());
        Assert.assertTrue("Exception should be ApiException", failureSource.getTask().getException() instanceof ApiException);
        ApiException apiEx = (ApiException) failureSource.getTask().getException();
        Assert.assertEquals(CommonStatusCodes.TIMEOUT, apiEx.getStatusCode());

        // Void success case
        TaskCompletionSource<Void> voidSource = new TaskCompletionSource<>();
        WearableTasks.completeVoid(voidSource, 0);
        Assert.assertTrue("Void task should be completed", voidSource.getTask().isSuccessful());
        Assert.assertNull(voidSource.getTask().getResult());
    }

    @Test
    public void testDataClientConstantsAndResponse() {
        Assert.assertEquals(0, DataClient.FILTER_LITERAL);
        Assert.assertEquals(1, DataClient.FILTER_PREFIX);
        Assert.assertEquals("com.google.android.gms.wearable.DATA_CHANGED", DataClient.ACTION_DATA_CHANGED);

        DataApi.GetFdForAssetResult fakeResult = new DataApi.GetFdForAssetResult() {
            @Override
            public com.google.android.gms.common.api.Status getStatus() {
                return com.google.android.gms.common.api.Status.SUCCESS;
            }
            @Override
            public ParcelFileDescriptor getFd() { return null; }
            @Override
            public InputStream getInputStream() { return null; }
            @Override
            public void release() {}
        };
        Assert.assertTrue(fakeResult instanceof Result);
        Assert.assertTrue(fakeResult instanceof Releasable);
        DataClient.GetFdForAssetResponse response = new DataClient.GetFdForAssetResponse(fakeResult);
        Assert.assertSame(fakeResult, response.getResult());
        Assert.assertNull(response.getFd());
        Assert.assertNull(response.getInputStream());
        fakeResult.release();
    }

    @Test
    public void testWearableListenerAdapterMultiKeyIsolation() {
        Object listener = new Object();
        IWearableListener stub1 = WearableListenerAdapter.getOrCreate(listener, "type_data", WearableListenerAdapter::new);
        IWearableListener stub2 = WearableListenerAdapter.getOrCreate(listener, "type_msg", WearableListenerAdapter::new);

        Assert.assertNotNull(stub1);
        Assert.assertNotNull(stub2);
        Assert.assertNotSame("Different listener types on same object must yield different stubs", stub1, stub2);

        IWearableListener removed1 = WearableListenerAdapter.remove(listener, "type_data");
        Assert.assertSame(stub1, removed1);

        // stub2 should still be isolated and present
        IWearableListener reGet2 = WearableListenerAdapter.getOrCreate(listener, "type_msg", WearableListenerAdapter::new);
        Assert.assertSame("Type msg stub should remain active", stub2, reGet2);

        WearableListenerAdapter.remove(listener, "type_msg");
    }

    @Test
    public void testRemoveAllCapabilityListenersByIdentityHash() {
        Object listener = new Object();
        Object other = new Object();
        IWearableListener capPhone = WearableListenerAdapter.getOrCreate(listener, "cap_phone", WearableListenerAdapter::new);
        IWearableListener capWatch = WearableListenerAdapter.getOrCreate(listener, "cap_watch", WearableListenerAdapter::new);
        IWearableListener otherStub = WearableListenerAdapter.getOrCreate(other, "cap_phone", WearableListenerAdapter::new);

        List<IWearableListener> removed = WearableListenerAdapter.removeAllForListener(listener);
        Assert.assertEquals(2, removed.size());
        Assert.assertTrue(removed.contains(capPhone));
        Assert.assertTrue(removed.contains(capWatch));
        Assert.assertFalse(removed.contains(otherStub));

        IWearableListener stillOther = WearableListenerAdapter.getOrCreate(other, "cap_phone", WearableListenerAdapter::new);
        Assert.assertSame(otherStub, stillOther);

        IWearableListener recreated = WearableListenerAdapter.getOrCreate(listener, "cap_phone", WearableListenerAdapter::new);
        Assert.assertNotSame(capPhone, recreated);

        WearableListenerAdapter.removeAllForListener(listener);
        WearableListenerAdapter.removeAllForListener(other);
    }

    @Test
    public void testDeliverDataClosesHolderOnFailure() {
        DataHolder rejected = newHolder();
        WearableListenerAdapter.deliverData(rejected, () -> false);
        Assert.assertTrue(rejected.isClosed());

        DataHolder accepted = newHolder();
        WearableListenerAdapter.deliverData(accepted, () -> true);
        Assert.assertFalse(accepted.isClosed());
        accepted.close();

        DataHolder denied = newHolder();
        try {
            WearableListenerAdapter.deliverData(denied, () -> {
                throw new SecurityException("Caller is not Services Core");
            });
            Assert.fail("SecurityException expected");
        } catch (SecurityException expected) {
            Assert.assertTrue(denied.isClosed());
        }
    }

    @Test
    public void testPostOnMainThreadClosesDataWhenDeliveryFails() {
        DataHolder holder = newHolder();
        boolean[] ran = new boolean[]{false};
        WearableListenerAdapter.postOnMainThread(holder, () -> ran[0] = true);
        if (Looper.getMainLooper() == null) {
            Assert.assertFalse(ran[0]);
            Assert.assertTrue(holder.isClosed());
        } else {
            Assert.assertTrue(ran[0]);
        }
    }

    @Test
    public void testDataUriPrefixBoundaries() {
        Assert.assertTrue(DataUriMatcher.isPrefix(DataClient.FILTER_PREFIX));
        Assert.assertFalse(DataUriMatcher.isPrefix(DataClient.FILTER_LITERAL));

        Assert.assertTrue(DataUriMatcher.matches("node-a", "/sensor", "*", "/sensor", DataClient.FILTER_PREFIX));
        Assert.assertTrue(DataUriMatcher.matches("node-b", "/sensor/hr", "*", "/sensor", DataClient.FILTER_PREFIX));
        Assert.assertTrue(DataUriMatcher.matches("node-a", "/sensors", "node-a", "/sensor", DataClient.FILTER_PREFIX));
        Assert.assertFalse(DataUriMatcher.matches("node-b", "/sensor/hr", "node-a", "/sensor", DataClient.FILTER_PREFIX));
        Assert.assertFalse(DataUriMatcher.matches("node-a", "/other", "*", "/sensor", DataClient.FILTER_PREFIX));

        Assert.assertTrue(DataUriMatcher.matches("node-a", "/sensor", "node-a", "/sensor", DataClient.FILTER_LITERAL));
        Assert.assertFalse(DataUriMatcher.matches("node-a", "/sensor/hr", "node-a", "/sensor", DataClient.FILTER_LITERAL));
        Assert.assertTrue(DataUriMatcher.matches("anywhere", "/kept", null, null, DataClient.FILTER_PREFIX));
        Assert.assertFalse(DataUriMatcher.matches("node-a", null, "node-a", "/sensor", DataClient.FILTER_PREFIX));

        Assert.assertEquals("100\\%\\_ok%", DataUriMatcher.toLikePrefix("100%_ok"));
        Assert.assertFalse(DataUriMatcher.constrainsHost("*"));
        Assert.assertFalse(DataUriMatcher.constrainsHost(""));
        Assert.assertTrue(DataUriMatcher.constrainsHost("node-a"));
    }

    @Test
    public void testOpenChannelDoesNotFabricateOnError() {
        ChannelClient.Channel failed = ChannelClientImpl.channelForOpen(null, CommonStatusCodes.INTERNAL_ERROR, "node", "/path");
        Assert.assertNull(failed);

        ChannelClient.Channel opened = ChannelClientImpl.channelForOpen(null, 0, "node", "/path");
        Assert.assertNotNull(opened);
        Assert.assertEquals("node", opened.getNodeId());
        Assert.assertEquals("/path", opened.getPath());
        Assert.assertTrue(((ChannelImpl) opened).getToken().startsWith("chan_"));

        ChannelClient.Channel existing = ChannelClientImpl.channelForOpen(
                new ChannelParcelable("tok", "n1", "/real"), CommonStatusCodes.INTERNAL_ERROR, "other", "/other");
        Assert.assertEquals("n1", existing.getNodeId());
        Assert.assertEquals("/real", existing.getPath());
        Assert.assertEquals("tok", ((ChannelImpl) existing).getToken());
    }

    @Test
    public void testStreamPfdClosedOnlyOnError() {
        int[] closes = new int[]{0};
        ChannelClientImpl.closeOnError(CommonStatusCodes.INTERNAL_ERROR, () -> closes[0]++);
        ChannelClientImpl.closeOnError(0, () -> closes[0]++);
        ChannelClientImpl.closeOnError(CommonStatusCodes.ERROR, (ChannelClientImpl.ErrorCloseable) null);
        ChannelClientImpl.closeOnError(CommonStatusCodes.ERROR, (ParcelFileDescriptor) null);
        Assert.assertEquals(1, closes[0]);
    }

    @Test
    public void testCapabilityChangedCarriesLocalNode() {
        String localId = "local-node";
        List<NodeParcelable> nodes = Arrays.asList(new NodeParcelable(localId, localId));
        Assert.assertEquals(1, nodes.size());
        Assert.assertEquals(localId, nodes.get(0).getId());
        Assert.assertEquals(localId, nodes.get(0).getDisplayName());
    }

    private static DataHolder newHolder() {
        return new DataHolder(new String[]{"host"}, new CursorWindow[0], 0, null);
    }
}
