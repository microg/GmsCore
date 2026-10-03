/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.gms.wearable;

import android.app.Service;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import android.os.PatternMatcher;
import android.test.AndroidTestCase;
import android.test.mock.MockPackageManager;

import com.google.android.gms.common.data.DataHolder;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.ConnectionConfiguration;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.internal.*;

import org.microg.gms.common.PackageUtils;
import org.microg.wearable.CapabilityPaths;
import org.microg.wearable.WearableConnection;
import org.microg.wearable.proto.Connect;
import org.microg.wearable.proto.MessagePiece;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/** Real SQLite, handler and manifest binding in this test APK; no sockets or accounts. */
public class CapabilityChangeTest extends AndroidTestCase {
    private static final String ACTION = "com.google.android.gms.wearable.CAPABILITY_CHANGED";
    private static final String PEER = "capability-peer";
    private static final String OTHER_PEER = "other-capability-peer";
    private static final String WRONG_SIGNATURE = "0000000000000000000000000000000000000000";
    private NodeDatabaseHelper nodes;
    private ConfigurationDatabaseHelper configurations;
    private WearableImpl wearable;
    private SignatureContext context;
    private String packageName;
    private String signature;
    private long sequence;
    private ConnectionConfiguration config;
    private final SilentConnection connection = new SilentConnection();

    @Override protected void setUp() throws Exception {
        super.setUp();
        assertEquals("org.microg.gms.wearable.core.test", getContext().getPackageName());
        context = new SignatureContext(getContext());
        packageName = context.getPackageName();
        signature = PackageUtils.firstSignatureDigest(context, packageName);
        getContext().deleteDatabase("node.db");
        getContext().deleteDatabase("connectionconfig.db");
        ManifestService.events.clear();
        nodes = new NodeDatabaseHelper(context);
        configurations = new ConfigurationDatabaseHelper(context);
        wearable = new WearableImpl(context, nodes, configurations);
        drain();
        config = connect(PEER, connection, "AA:BB:CC:DD:EE:31");
    }

    @Override protected void tearDown() throws Exception {
        try {
            if (wearable != null) {
                Thread network = wearable.networkHandler.getLooper().getThread();
                wearable.stop();
                network.join(3000);
                assertFalse(network.isAlive());
            }
        } finally {
            if (configurations != null) configurations.close();
            if (nodes != null) nodes.close();
            getContext().deleteDatabase("connectionconfig.db");
            getContext().deleteDatabase("node.db");
            ManifestService.events.clear();
            super.tearDown();
        }
    }

    public void testArrivalAfterEmptyQueryNotifiesThenDeletionEmptiesNodes() throws Exception {
        assertNodes(snapshot("media", CapabilityApi.FILTER_REACHABLE));
        RecordingListener listener = register("/media", PatternMatcher.PATTERN_LITERAL);
        receive(PEER, "media", false);
        assertNodes(take(listener.events), PEER);
        assertNodes(snapshot("media", CapabilityApi.FILTER_REACHABLE), PEER);
        receive(PEER, "media", true);
        assertNodes(take(listener.events));
        assertNodes(snapshot("media", CapabilityApi.FILTER_ALL));
    }

    public void testDisconnectAndReconnectRecomputeWithoutDeletingKnownCapability() throws Exception {
        RecordingListener listener = register("/media", PatternMatcher.PATTERN_LITERAL);
        receive(PEER, "media", false);
        assertNodes(take(listener.events), PEER);
        wearable.onDisconnectReceived(connection, new Connect.Builder().id(PEER).build());
        drain();
        assertNodes(take(listener.events));
        assertNodes(snapshot("media", CapabilityApi.FILTER_REACHABLE));
        CapabilityInfoParcelable known = snapshot("media", CapabilityApi.FILTER_ALL);
        assertEquals(1, known.getNodes().size());
        assertFalse(known.getNodes().iterator().next().isNearby());
        wearable.onConnectReceived(connection, config, new Connect.Builder().id(PEER).build());
        drain();
        assertNodes(take(listener.events), PEER);
    }

    public void testTwoNodesAggregateAndOnlyDisconnectedNodeIsRemoved() throws Exception {
        SilentConnection second = new SilentConnection();
        connect(OTHER_PEER, second, "AA:BB:CC:DD:EE:32");
        RecordingListener listener = register("/media", PatternMatcher.PATTERN_LITERAL);
        receive(PEER, "media", false);
        take(listener.events);
        receive(OTHER_PEER, "media", false);
        assertNodes(take(listener.events), PEER, OTHER_PEER);
        receive(PEER, "media", false);
        assertNodes(take(listener.events), PEER, OTHER_PEER);
        wearable.onDisconnectReceived(connection, new Connect.Builder().id(PEER).build());
        drain();
        assertNodes(take(listener.events), OTHER_PEER);
    }

    public void testLiteralAndPrefixFiltersUseCapabilityNameNotSystemPath() throws Exception {
        RecordingListener literal = register("/media", PatternMatcher.PATTERN_LITERAL);
        RecordingListener prefix = register("/media", PatternMatcher.PATTERN_PREFIX);
        RecordingListener wrongHost = new RecordingListener();
        // Host-specific capability filters cannot match aggregate capability events.
        IntentFilter onlyPeer = new IntentFilter(ACTION);
        onlyPeer.addDataScheme("wear");
        onlyPeer.addDataAuthority(PEER, null);
        onlyPeer.addDataPath("/media", PatternMatcher.PATTERN_PREFIX);
        wearable.addListener(packageName, wrongHost, new IntentFilter[]{onlyPeer});
        receive(PEER, "media/play", false);
        assertEquals("media/play", take(prefix.events).getName());
        assertTrue(literal.events.isEmpty());
        assertTrue(wrongHost.events.isEmpty());
        receive(PEER, "media", false);
        assertNodes(take(literal.events), PEER);
        assertNodes(take(prefix.events), PEER);
        assertTrue(wrongHost.events.isEmpty());
    }

    public void testPackageCertificateAndSystemNamespaceAreIsolated() throws Exception {
        RecordingListener listener = register("/media", PatternMatcher.PATTERN_LITERAL);
        receive(record(PEER, CapabilityPaths.path("org.example.other", signature, "media"), false));
        receive(record(PEER, CapabilityPaths.path(packageName, WRONG_SIGNATURE, "media"), false));
        DataItemRecord wrongOuter = record(PEER, CapabilityPaths.path(packageName, signature, "media"), false);
        wrongOuter.signatureDigest = WRONG_SIGNATURE;
        receive(wrongOuter);
        receive(record(PEER, CapabilityPaths.prefix(packageName, signature) + "%6Dedia", false));
        assertTrue(listener.events.isEmpty());
        assertNodes(snapshot("media", CapabilityApi.FILTER_ALL));
        receive(PEER, "media", false);
        assertNodes(take(listener.events), PEER);
    }

    public void testLocalNodeNeverAppearsOrNotifiesAsRemoteCapability() throws Exception {
        RecordingListener listener = register("/media", PatternMatcher.PATTERN_LITERAL);
        receive(wearable.getLocalNodeId(), "media", false);
        assertTrue(listener.events.isEmpty());
        assertNodes(snapshot("media", CapabilityApi.FILTER_ALL));
        receive(PEER, "media", false);
        assertNodes(take(listener.events), PEER);
    }

    public void testRemovedListenerDoesNotReceiveFurtherUpdates() throws Exception {
        RecordingListener listener = register("/media", PatternMatcher.PATTERN_LITERAL);
        receive(PEER, "media", false);
        take(listener.events);
        wearable.removeListener(listener);
        receive(PEER, "media", true);
        assertTrue(listener.events.isEmpty());
    }

    public void testListenerCannotInheritReplacementPackageCertificate() throws Exception {
        RecordingListener oldIdentity = register("/media", PatternMatcher.PATTERN_LITERAL);
        context.replacementSignature = new Signature(new byte[]{1, 4, 9, 16});
        String replacement = PackageUtils.firstSignatureDigest(context, packageName);
        assertFalse(signature.equals(replacement));
        receive(record(PEER, CapabilityPaths.path(packageName, replacement, "media"), false));
        assertTrue(oldIdentity.events.isEmpty());
        RecordingListener currentIdentity = register("/media", PatternMatcher.PATTERN_LITERAL);
        receive(record(PEER, CapabilityPaths.path(packageName, replacement, "media"), false));
        assertNodes(take(currentIdentity.events), PEER);
        assertTrue(oldIdentity.events.isEmpty());
    }

    public void testManifestListenerReceivesArrivalAndWithdrawalWithoutDynamicRegistration() throws Exception {
        receive(PEER, "manifest_only", false);
        CapabilityInfoParcelable available = take(ManifestService.events);
        assertEquals("manifest_only", available.getName());
        assertNodes(available, PEER);
        receive(PEER, "manifest_only", true);
        assertNodes(take(ManifestService.events));
    }

    public void testDelayedManifestBindCannotRestoreDisconnectedNode() throws Exception {
        context.deferBinding = true;
        receive(PEER, "manifest_only", false);
        context.awaitBindings(1);
        assertEquals(1, context.pendingBindings.size());
        wearable.onDisconnectReceived(connection, new Connect.Builder().id(PEER).build());
        drain();
        context.awaitBindings(2);
        assertEquals(2, context.pendingBindings.size());
        RecordingListener target = new RecordingListener();
        context.deliver(1, target);
        assertNodes(take(target.events));
        context.deliver(0, target);
        awaitIdle(3000);
        assertTrue(target.events.isEmpty());
        assertEquals(2, context.unbound);
    }

    public void testManifestCertificateIsRecheckedAfterBinding() throws Exception {
        context.deferBinding = true;
        receive(PEER, "manifest_only", false);
        context.awaitBindings(1);
        assertEquals(1, context.pendingBindings.size());
        context.replacementSignature = new Signature(new byte[]{2, 4, 6, 8});
        RecordingListener target = new RecordingListener();
        context.deliver(0, target);
        awaitIdle(3000);
        assertTrue(target.events.isEmpty());
        assertEquals(1, context.unbound);
    }

    public void testManifestResolutionCannotRedirectToAnotherPackage() throws Exception {
        context.deferBinding = true;
        context.resolvedPackageOverride = "org.example.unrelated";
        receive(PEER, "manifest_only", false);
        awaitIdle(3000);
        assertTrue(context.pendingBindings.isEmpty());
        assertTrue(ManifestService.events.isEmpty());
    }

    public void testSlowDynamicListenerDoesNotBlockNetworkOrReorderRemoval() throws Exception {
        BlockingListener listener = new BlockingListener();
        wearable.addListener(packageName, listener, new IntentFilter[]{filter("/media", PatternMatcher.PATTERN_LITERAL)});
        try {
            receive(PEER, "media", false);
            assertTrue(listener.entered.await(3, TimeUnit.SECONDS));
            assertNotSame(wearable.networkHandler.getLooper().getThread(), listener.thread);
            wearable.onDisconnectReceived(connection, new Connect.Builder().id(PEER).build());
            drain(); // Must complete while the client is still blocked.
            listener.release.countDown();
            assertNodes(take(listener.events), PEER);
            assertNodes(take(listener.events));
            awaitIdle(3000);
        } finally { listener.release.countDown(); }
    }

    public void testSlowManifestListenerDoesNotBlockMainCallbackOrNetwork() throws Exception {
        context.deferBinding = true;
        BlockingListener listener = new BlockingListener();
        try {
            receive(PEER, "manifest_only", false);
            context.awaitBindings(1);
            CountDownLatch returned = new CountDownLatch(1);
            new Handler(Looper.getMainLooper()).post(() -> {
                context.deliver(0, listener);
                returned.countDown();
            });
            assertTrue(returned.await(3, TimeUnit.SECONDS));
            assertTrue(listener.entered.await(3, TimeUnit.SECONDS));
            assertNotSame(Looper.getMainLooper().getThread(), listener.thread);
            drain();
            listener.release.countDown();
            assertNodes(take(listener.events), PEER);
            awaitIdle(3000);
            assertEquals(1, context.unbound);
        } finally { listener.release.countDown(); }
    }

    public void testManifestBindTimeoutClosesAndRejectsLateCallback() throws Exception {
        context.deferBinding = true;
        receive(PEER, "manifest_only", false);
        context.awaitBindings(1);
        awaitIdle(12_000);
        assertEquals(1, context.unbound);
        RecordingListener listener = new RecordingListener();
        context.deliver(0, listener);
        awaitIdle(3000);
        assertTrue(listener.events.isEmpty());
    }

    public void testCompletedNamesDoNotAccumulateRevisionState() throws Exception {
        RecordingListener listener = register("/distinct_", PatternMatcher.PATTERN_PREFIX);
        for (int i = 0; i < 40; i++) {
            receive(PEER, "distinct_" + i, false);
            assertNodes(take(listener.events), PEER);
            awaitIdle(3000);
        }
        Object dispatcher = field(wearable, "capabilityListeners");
        synchronized (dispatcher) {
            assertTrue(((java.util.Map<?, ?>) field(dispatcher, "latest")).isEmpty());
        }
    }

    public void testRepeatedUpdatesCoalesceBehindSlowListenerAndKeepFinalRemoval() throws Exception {
        BlockingListener listener = new BlockingListener();
        wearable.addListener(packageName, listener, new IntentFilter[]{filter("/media", PatternMatcher.PATTERN_LITERAL)});
        try {
            receive(PEER, "media", false);
            assertTrue(listener.entered.await(3, TimeUnit.SECONDS));
            for (int i = 0; i < 32; i++) receive(PEER, "media", false);
            receive(PEER, "media", true);
            listener.release.countDown();
            assertNodes(take(listener.events), PEER);
            assertNodes(take(listener.events));
            awaitIdle(3000);
            assertTrue(listener.events.isEmpty());
        } finally { listener.release.countDown(); }
    }

    public void testBlockedRecipientCannotAccumulateUnboundedNamesOrJobs() throws Exception {
        BlockingListener listener = new BlockingListener();
        wearable.addListener(packageName, listener, new IntentFilter[]{filter("/bounded_", PatternMatcher.PATTERN_PREFIX)});
        try {
            receive(PEER, "bounded_first", false);
            assertTrue(listener.entered.await(3, TimeUnit.SECONDS));
            for (int i = 0; i < 40; i++) receive(PEER, "bounded_" + i, false);
            Object dispatcher = field(wearable, "capabilityListeners");
            synchronized (dispatcher) {
                assertEquals(8, ((Set<?>) field(dispatcher, "pending")).size());
                assertTrue(((java.util.Map<?, ?>) field(dispatcher, "latest")).size() <= 8);
            }
            listener.release.countDown();
            awaitIdle(3000);
        } finally { listener.release.countDown(); }
    }

    public void testBlockedCallsKeepProcessQuotaAcrossDispatcherRecreation() throws Exception {
        List<CapabilityListenerDispatcher> generations = new ArrayList<>();
        List<BlockingListener> blocked = new ArrayList<>();
        Intent intent = new Intent(ACTION, android.net.Uri.parse("wear:///recreated")).setPackage(packageName);
        CapabilityInfoParcelable info = new CapabilityInfoParcelable("recreated", Collections.emptyList());
        try {
            for (int i = 0; i < 8; i++) {
                CapabilityListenerDispatcher generation = new CapabilityListenerDispatcher(context, wearable);
                generations.add(generation);
                BlockingListener listener = new BlockingListener();
                blocked.add(listener);
                generation.send("recreated", signature, intent, info, Collections.singletonList(listener));
                assertTrue(listener.entered.await(3, TimeUnit.SECONDS));
                generation.stop(); // A stopped instance's in-flight Binder must retain its lease.
            }
            CapabilityListenerDispatcher replacement = new CapabilityListenerDispatcher(context, wearable);
            generations.add(replacement);
            RecordingListener ninth = new RecordingListener();
            replacement.send("recreated", signature, intent, info, Collections.singletonList(ninth));
            awaitIdle(replacement, 3000);
            assertTrue(ninth.events.isEmpty());
            for (BlockingListener listener : blocked) listener.release.countDown();
            for (CapabilityListenerDispatcher generation : generations) awaitIdle(generation, 3000);
            replacement.send("recreated", signature, intent, info, Collections.singletonList(ninth));
            assertNodes(take(ninth.events));
            awaitIdle(replacement, 3000);
        } finally {
            for (BlockingListener listener : blocked) listener.release.countDown();
            for (CapabilityListenerDispatcher generation : generations) generation.stop();
            for (CapabilityListenerDispatcher generation : generations) awaitIdle(generation, 3000);
        }
    }

    private CapabilityInfoParcelable snapshot(String name, int filter) {
        return CapabilityManager.snapshot(wearable, packageName, signature, name, filter);
    }

    private RecordingListener register(String path, int type) {
        RecordingListener listener = new RecordingListener();
        wearable.addListener(packageName, listener, new IntentFilter[]{filter(path, type)});
        return listener;
    }

    private static IntentFilter filter(String path, int type) {
        IntentFilter filter = new IntentFilter(ACTION);
        filter.addDataScheme("wear");
        filter.addDataAuthority("*", null);
        filter.addDataPath(path, type);
        return filter;
    }

    private ConnectionConfiguration connect(String peer, SilentConnection transport, String address) throws Exception {
        ConnectionConfiguration configuration = configurations.putManagedConfiguration(
                new ConnectionConfiguration(peer, address, 1, 1, true), wearable.getLocalNodeId());
        // Inserting this synthetic transport directly avoids opening a Bluetooth socket.
        // Notify the service through its update API so later peers enter its cached snapshot.
        wearable.updateConnection(configuration);
        wearable.onConnectReceived(transport, configuration, new Connect.Builder().id(peer).build());
        drain();
        return configuration;
    }

    private void receive(String host, String name, boolean deleted) throws Exception {
        receive(record(host, CapabilityPaths.path(packageName, signature, name), deleted));
    }

    private DataItemRecord record(String host, String path, boolean deleted) {
        DataItemRecord record = new DataItemRecord();
        record.packageName = CapabilityPaths.PACKAGE;
        record.signatureDigest = CapabilityPaths.SIGNATURE;
        record.source = host;
        record.seqId = record.v1SeqId = ++sequence;
        record.deleted = deleted;
        record.assetsAreReady = true;
        record.dataItem = new DataItemInternal(host, path);
        record.dataItem.data = new byte[]{'s'};
        return record;
    }

    private void receive(DataItemRecord record) throws Exception {
        wearable.putDataItem(record);
        drain();
    }

    private void drain() throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        assertTrue(wearable.networkHandler.post(done::countDown));
        assertTrue(done.await(3, TimeUnit.SECONDS));
    }

    private void awaitIdle(long timeoutMillis) throws Exception {
        Object dispatcher = field(wearable, "capabilityListeners");
        awaitIdle(dispatcher, timeoutMillis);
    }

    private void awaitIdle(Object dispatcher, long timeoutMillis) throws Exception {
        long deadline = android.os.SystemClock.elapsedRealtime() + timeoutMillis;
        do {
            synchronized (dispatcher) {
                if (((Set<?>) field(dispatcher, "pending")).isEmpty()) return;
            }
            Thread.sleep(5);
        } while (android.os.SystemClock.elapsedRealtime() < deadline);
        fail("Capability deliveries did not retire before their deadline");
    }

    private static Object field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    static CapabilityInfoParcelable take(BlockingQueue<CapabilityInfoParcelable> queue) throws Exception {
        CapabilityInfoParcelable info = queue.poll(3, TimeUnit.SECONDS);
        assertNotNull("Capability callback was not delivered", info);
        return info;
    }

    private static void assertNodes(CapabilityInfoParcelable info, String... expected) {
        Set<String> ids = new HashSet<>();
        for (Node node : info.getNodes()) {
            assertTrue(node.isNearby());
            assertTrue("Duplicate node", ids.add(node.getId()));
        }
        assertEquals(new HashSet<>(Arrays.asList(expected)), ids);
    }

    /** Manifest-only endpoint in the isolated test package. */
    public static final class ManifestService extends Service {
        static final BlockingQueue<CapabilityInfoParcelable> events = new LinkedBlockingQueue<>();
        @Override public IBinder onBind(Intent intent) {
            if (!"com.google.android.gms.wearable.BIND_LISTENER".equals(intent.getAction())) return null;
            return new RecordingListener(events);
        }
    }

    static class RecordingListener extends IWearableListener.Stub {
        final BlockingQueue<CapabilityInfoParcelable> events;
        RecordingListener() { this(new LinkedBlockingQueue<>()); }
        RecordingListener(BlockingQueue<CapabilityInfoParcelable> events) { this.events = events; }
        @Override public void onConnectedCapabilityChanged(CapabilityInfoParcelable info) { events.add(info); }
        @Override public void onDataChanged(DataHolder data) { data.close(); }
        @Override public void onMessageReceived(MessageEventParcelable event) { }
        @Override public void onPeerConnected(NodeParcelable node) { }
        @Override public void onPeerDisconnected(NodeParcelable node) { }
        @Override public void onConnectedNodes(List<NodeParcelable> nodes) { }
        @Override public void onNotificationReceived(AncsNotificationParcelable notification) { }
        @Override public void onChannelEvent(ChannelEventParcelable event) { }
        @Override public void onEntityUpdate(AmsEntityUpdateParcelable event) { }
        @Override public void onRequestReceived(MessageEventParcelable event, IRpcResponseCallback callback) { }
    }

    private static final class BlockingListener extends RecordingListener {
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        volatile Thread thread;
        @Override public void onConnectedCapabilityChanged(CapabilityInfoParcelable info) {
            thread = Thread.currentThread();
            entered.countDown();
            try { release.await(); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
            super.onConnectedCapabilityChanged(info);
        }
    }

    private static final class SilentConnection extends WearableConnection {
        SilentConnection() { super(null); }
        @Override protected void writeMessagePiece(MessagePiece piece) { }
        @Override protected MessagePiece readMessagePiece() { throw new AssertionError("No transport reads"); }
        @Override public void close() { }
    }

    /** A package replacement fixture changes only PackageManager's certificate observation. */
    private static final class SignatureContext extends ContextWrapper {
        volatile Signature replacementSignature;
        boolean deferBinding;
        String resolvedPackageOverride;
        volatile int unbound;
        final List<ServiceConnection> pendingBindings = new CopyOnWriteArrayList<>();
        final BlockingQueue<ServiceConnection> bindingEvents = new LinkedBlockingQueue<>();
        private int observedBindings;
        private final PackageManager packages = new MockPackageManager() {
            @Override public PackageInfo getPackageInfo(String name, int flags) throws NameNotFoundException {
                if (getPackageName().equals(name) && replacementSignature != null) {
                    PackageInfo info = new PackageInfo();
                    info.signatures = new Signature[]{replacementSignature};
                    return info;
                }
                return getBaseContext().getPackageManager().getPackageInfo(name, flags);
            }
            @Override public ResolveInfo resolveService(Intent intent, int flags) {
                ResolveInfo resolved = getBaseContext().getPackageManager().resolveService(intent, flags);
                if (resolved != null && resolvedPackageOverride != null) {
                    resolved = new ResolveInfo(resolved);
                    resolved.serviceInfo = new ServiceInfo(resolved.serviceInfo);
                    resolved.serviceInfo.packageName = resolvedPackageOverride;
                }
                return resolved;
            }
            @Override public Resources getResourcesForApplication(String name) throws NameNotFoundException {
                return getBaseContext().getPackageManager().getResourcesForApplication(name);
            }
        };
        SignatureContext(Context context) { super(context); }
        @Override public PackageManager getPackageManager() { return packages; }
        @Override public boolean bindService(Intent intent, ServiceConnection connection, int flags) {
            if (!deferBinding) return super.bindService(intent, connection, flags);
            pendingBindings.add(connection);
            bindingEvents.add(connection);
            return true;
        }
        @Override public void unbindService(ServiceConnection connection) {
            if (!pendingBindings.contains(connection)) super.unbindService(connection);
            else unbound++;
        }
        void deliver(int index, RecordingListener target) {
            pendingBindings.get(index).onServiceConnected(
                    new ComponentName(getPackageName(), ManifestService.class.getName()), target.asBinder());
        }
        void awaitBindings(int count) throws Exception {
            while (observedBindings < count) {
                assertNotNull("Manifest listener was not bound", bindingEvents.poll(3, TimeUnit.SECONDS));
                observedBindings++;
            }
        }
    }
}
