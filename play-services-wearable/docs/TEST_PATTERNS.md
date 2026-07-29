# Wear OS Test Patterns & Mock Strategies

## Robolectric Test Patterns

### Testing Bluetooth-Dependent Code
```java
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 30)
public class BluetoothTest {
    
    @Before
    public void setUp() {
        // Robolectric provides ShadowBluetoothAdapter
        ShadowBluetoothAdapter shadowAdapter = 
            Shadows.shadowOf(BluetoothAdapter.getDefaultAdapter());
        shadowAdapter.setEnabled(true);
        shadowAdapter.setBleSupported(true);
    }
    
    @Test
    public void testBluetoothScanning_startAndStop() {
        // Test scan lifecycle without real Bluetooth hardware
        manager.startScanning();
        assertTrue(manager.isScanning());
        manager.stopScanning();
        assertFalse(manager.isScanning());
    }
}
```

### Testing with Mockito
```java
@RunWith(RobolectricTestRunner.class)
public class ListenerTest {
    
    @Mock
    private CompanionPairingManager.PairingListener mockListener;
    
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        manager.addListener(mockListener);
    }
    
    @Test
    public void testListener_calledOnDeviceFound() {
        CompanionDevice device = new CompanionDevice(
            "AA:BB:CC:DD:EE:FF", "Test", "node1", 1, 0L);
        
        manager.simulateDeviceFound(device);
        
        verify(mockListener, times(1)).onDeviceFound(device);
    }
}
```

### Testing Binder IPC
```java
@RunWith(RobolectricTestRunner.class)
public class BinderTest {
    
    @Test
    public void testOpenChannel_viaBinder() throws RemoteException {
        IWearableCallbacks.Stub callbacks = new IWearableCallbacks.Stub() {
            @Override
            public void onOpenChannelResponse(OpenChannelResponse response) {
                assertEquals(0, response.statusCode);
            }
        };
        
        service.openChannel(callbacks, "testNode", "/test/path");
    }
}
```

## Custom Robolectric Shadows

### ShadowWearableImpl
```java
@Implements(WearableImpl.class)
public class ShadowWearableImpl {
    
    private final List<MessageSent> sentMessages = new ArrayList<>();
    
    @Implementation
    public int sendMessage(String packageName, String nodeId, 
                           String path, byte[] data) {
        sentMessages.add(new MessageSent(nodeId, path, data));
        return 0; // SUCCESS
    }
    
    public List<MessageSent> getSentMessages() {
        return Collections.unmodifiableList(sentMessages);
    }
    
    @Implementation
    public List<NodeParcelable> getConnectedNodes() {
        return Collections.emptyList();
    }
}
```

### ShadowBluetoothAdapter (Extended)
```java
@Implements(BluetoothAdapter.class)
public class ExtendedShadowBluetoothAdapter {
    
    private static boolean bleScanningSupported = true;
    private static boolean bluetoothEnabled = true;
    private static final Set<String> bondedDevices = new HashSet<>();
    
    @Implementation
    public static BluetoothAdapter getDefaultAdapter() {
        return ReflectionHelpers.callConstructor(BluetoothAdapter.class);
    }
    
    @Implementation
    public boolean isEnabled() {
        return bluetoothEnabled;
    }
    
    @Implementation
    public boolean isBleScanningSupported() {
        return bleScanningSupported;
    }
    
    @Implementation
    public Set<BluetoothDevice> getBondedDevices() {
        return bondedDevices.stream()
            .map(addr -> createDevice(addr))
            .collect(Collectors.toSet());
    }
    
    public static void setBluetoothEnabled(boolean enabled) {
        bluetoothEnabled = enabled;
    }
    
    public static void addBondedDevice(String address) {
        bondedDevices.add(address);
    }
    
    public static void reset() {
        bluetoothEnabled = true;
        bleScanningSupported = true;
        bondedDevices.clear();
    }
}
```

## Test Data Factories

### CompanionDeviceFactory
```java
public class CompanionDeviceFactory {
    
    public static CompanionDevice createSamsungWatch() {
        return new CompanionDevice(
            "AA:BB:CC:DD:EE:01",
            "Galaxy Watch 6",
            "node_samsung_001",
            1, // TYPE_WATCH
            System.currentTimeMillis()
        );
    }
    
    public static CompanionDevice createPixelWatch() {
        return new CompanionDevice(
            "AA:BB:CC:DD:EE:02",
            "Pixel Watch 2",
            "node_pixel_001",
            1,
            System.currentTimeMillis()
        );
    }
    
    public static CompanionDevice createTicWatch() {
        return new CompanionDevice(
            "AA:BB:CC:DD:EE:03",
            "TicWatch Pro 5",
            "node_ticwatch_001",
            1,
            System.currentTimeMillis()
        );
    }
    
    public static CompanionDevice createFossilWatch() {
        return new CompanionDevice(
            "AA:BB:CC:DD:EE:04",
            "Fossil Gen 6",
            "node_fossil_001",
            1,
            System.currentTimeMillis()
        );
    }
    
    public static List<CompanionDevice> createAllWatches() {
        return Arrays.asList(
            createSamsungWatch(),
            createPixelWatch(),
            createTicWatch(),
            createFossilWatch()
        );
    }
    
    public static CompanionDevice createOfflineWatch() {
        CompanionDevice device = createSamsungWatch();
        device.isConnected = false;
        return device;
    }
}
```

### WearablePayloadFactory
```java
public class WearablePayloadFactory {
    
    public static byte[] createNotificationPayload(
            int id, String pkg, String title, String text) {
        return String.format(
            "{\"id\":%d,\"package\":\"%s\",\"title\":\"%s\",\"text\":\"%s\"}",
            id, pkg, title, text
        ).getBytes(StandardCharsets.UTF_8);
    }
    
    public static byte[] createCallStatePayload(
            String state, String number, String name) {
        return String.format(
            "{\"state\":\"%s\",\"number\":\"%s\",\"name\":\"%s\"}",
            state, number, name
        ).getBytes(StandardCharsets.UTF_8);
    }
    
    public static byte[] createMediaPayload(
            String title, String artist, String album, long duration) {
        return String.format(
            "{\"title\":\"%s\",\"artist\":\"%s\",\"album\":\"%s\",\"duration\":%d}",
            title, artist, album, duration
        ).getBytes(StandardCharsets.UTF_8);
    }
    
    public static byte[] createFilePayload(int sizeInBytes) {
        byte[] data = new byte[sizeInBytes];
        new Random(42).nextBytes(data);
        return data;
    }
}
```

## Integration Test Patterns

### End-to-End Pairing Test
```java
@RunWith(AndroidJUnit4.class)
public class PairingIntegrationTest {
    
    @Test
    public void testFullPairingLifecycle() {
        // 1. Start scanning
        manager.startScanning();
        
        // 2. Wait for device
        await().atMost(10, TimeUnit.SECONDS)
            .until(() -> !manager.getPairedDevices().isEmpty());
        
        // 3. Verify device
        List<CompanionDevice> devices = manager.getPairedDevices();
        assertEquals(1, devices.size());
        
        // 4. Check connection
        CompanionDevice device = devices.get(0);
        assertTrue(device.isConnected);
        assertNotNull(device.nodeId);
    }
}
```

### Notification Forwarding Test
```java
@RunWith(AndroidJUnit4.class)
public class NotificationIntegrationTest {
    
    @Test
    public void testNotificationForwarded() {
        // Send test notification
        Notification notification = new NotificationCompat.Builder(context, "test")
            .setContentTitle("Test Title")
            .setContentText("Test Body")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build();
        
        // Post and wait for forwarding
        notificationManager.notify(1, notification);
        
        await().atMost(5, TimeUnit.SECONDS)
            .until(() -> shadowWearable.getSentMessages().size() > 0);
        
        // Verify payload
        byte[] payload = shadowWearable.getSentMessages().get(0).getData();
        String json = new String(payload, StandardCharsets.UTF_8);
        assertTrue(json.contains("Test Title"));
        assertTrue(json.contains("Test Body"));
    }
}
```

## Performance Test Patterns

### Throughput Benchmark
```java
@RunWith(AndroidJUnit4.class)
public class ThroughputBenchmark {
    
    @Test(timeout = 5000)
    public void testMessageThroughput_1000Messages() {
        long start = System.nanoTime();
        
        for (int i = 0; i < 1000; i++) {
            byte[] data = ("message_" + i).getBytes();
            wearable.sendMessage("com.test", "node1", "/test", data);
        }
        
        long elapsed = System.nanoTime() - start;
        double msPerMessage = elapsed / 1_000_000.0 / 1000.0;
        
        // Should average under 1ms per message
        assertTrue("Too slow: " + msPerMessage + "ms/message", 
                   msPerMessage < 1.0);
    }
}
```

### Memory Leak Detection
```java
@RunWith(AndroidJUnit4.class)
public class MemoryTest {
    
    @Test
    public void testNoMemoryLeak_afterPairedDeviceListClear() {
        // Create many devices
        for (int i = 0; i < 1000; i++) {
            CompanionDevice device = new CompanionDevice(
                "AA:BB:CC:DD:EE:" + String.format("%02X", i % 256),
                "Watch_" + i, "node_" + i, 1, System.currentTimeMillis()
            );
            // Simulate pairing
        }
        
        // Trigger GC and check heap
        Runtime.getRuntime().gc();
        long heapAfter = Runtime.getRuntime().totalMemory() 
                       - Runtime.getRuntime().freeMemory();
        
        // Heap should be under 20MB for 1000 devices
        assertTrue("Heap too large: " + heapAfter / 1024 / 1024 + "MB",
                   heapAfter < 20 * 1024 * 1024);
    }
}
```
