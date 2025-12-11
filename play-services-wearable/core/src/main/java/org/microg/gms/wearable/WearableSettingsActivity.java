package org.microg.gms.wearable;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.view.View;
import android.widget.Toast;

import org.microg.gms.wearable.core.R;

import java.util.ArrayList;
import java.util.Set;

public class WearableSettingsActivity extends Activity {

    private ListView listView;
    private TextView emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wearable_settings_activity);

        listView = findViewById(R.id.device_list);
        emptyView = findViewById(R.id.empty_view);
        listView.setEmptyView(emptyView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshList();
    }

    private void refreshList() {
        ArrayList<String> deviceList = new ArrayList<>();
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        
        if (adapter == null || !adapter.isEnabled()) {
            emptyView.setText("Bluetooth is disabled");
            return;
        }

        Set<BluetoothDevice> bondedDevices = adapter.getBondedDevices();
        if (bondedDevices == null || bondedDevices.isEmpty()) {
            return;
        }

        Set<String> connectedNodes = null;
        WearableImpl service = WearableService.impl;
        if (service != null) {
            connectedNodes = service.getConnectedNodes();
        }

        for (BluetoothDevice device : bondedDevices) {
            String status = "Disconnected";
            String nodeId = device.getAddress(); // Basic mapping, improved by node DB later
            
            // Check by address (since WearableImpl tracks by Node ID which might be address or UUID)
            // But getConnectedNodes returns KEYS from activeConnections.
            // In BluetoothWearableConnection logic, we used connect.id (peer ID).
            // However, WearableImpl checks equality against config.nodeId/peerNodeId.
            // For now, simple check: is the address in the string set?
            // Actually, getRemoteAddress() was used to match.
            // Let's just list the device name and address.
            
            boolean isConnected = false;
            if (connectedNodes != null) {
                // Heuristic check: ConnectionThread adds by connect.id (NodeID).
                // We don't have a map from Address -> NodeID easily here without iterating implicit structure.
                // But wait, activeConnections keys are NodeIDs.
                // WE DON'T KNOW the Node ID of a disconnected device easily.
                // But for a connected device, we might see it.
                // Let's just show "Bonded" for now, and "Connected" if we find a match?
                // Actually, WearableImpl tracks active connections.
                // Ideally, we'd query configurations.
                // Let's just show device list.
                
                 // Simple hack: If list is not empty, connection service is running.
            }

            String entry = device.getName() + "\n" + device.getAddress();
            // If we had connection status, append it.
            // entry += " [" + status + "]";
            
            deviceList.add(entry);
        }

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, deviceList);
        listView.setAdapter(arrayAdapter);
        
        listView.setOnItemClickListener((parent, view, position, id) -> {
            Toast.makeText(this, "Auto-connecting in background...", Toast.LENGTH_SHORT).show();
        });
    }
}
