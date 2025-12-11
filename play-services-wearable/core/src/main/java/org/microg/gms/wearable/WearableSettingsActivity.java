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
    private WearableDeviceAdapter deviceAdapter;

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
        ArrayList<BluetoothDevice> deviceList = new ArrayList<>();
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        
        if (adapter == null || !adapter.isEnabled()) {
            emptyView.setText("Bluetooth is disabled");
            return;
        }

        Set<BluetoothDevice> bondedDevices = adapter.getBondedDevices();
        if (bondedDevices != null) {
            deviceList.addAll(bondedDevices);
        }

        Set<String> connectedNodes = null;
        WearableImpl service = WearableService.impl;
        if (service != null) {
            connectedNodes = service.getConnectedNodes();
        }

        deviceAdapter = new WearableDeviceAdapter(this, deviceList, connectedNodes);
        listView.setAdapter(deviceAdapter);
        
        listView.setOnItemClickListener((parent, view, position, id) -> {
            BluetoothDevice device = deviceAdapter.getItem(position);
            boolean isConnected = false;
            WearableImpl service = WearableService.impl;
            if (service != null) {
                Set<String> nodes = service.getConnectedNodes();
                if (nodes != null) {
                    isConnected = nodes.contains(device.getAddress()); // Simplified check
                }
            }

            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle(device.getName());
            
            if (isConnected) {
                builder.setMessage("This device is currently connected via MicroG.");
                builder.setPositiveButton("Disconnect", (dialog, which) -> {
                     if (WearableService.impl != null) {
                         WearableService.impl.closeConnection(device.getAddress());
                         Toast.makeText(this, "Disconnected", Toast.LENGTH_SHORT).show();
                         refreshList();
                     }
                });
            } else {
                builder.setMessage("This device is acting as a WearOS peer.");
                builder.setPositiveButton("Connect", (dialog, which) -> {
                     // Connection is automatic, but we can trigger a scan or hint
                     Toast.makeText(this, "MicroG is managing connections automatically.", Toast.LENGTH_SHORT).show();
                });
            }
            builder.setNeutralButton("Bluetooth Settings", (dialog, which) -> {
                try {
                    startActivity(new android.content.Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS));
                } catch (Exception e) {}
            });
            builder.setNegativeButton("Cancel", null);
            builder.show();
        });
        });
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        menu.add(0, 1, 0, "Scan for Devices")
            .setIcon(android.R.drawable.ic_menu_search)
            .setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == 1) {
            try {
                startActivity(new android.content.Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS));
            } catch (Exception e) {
                Toast.makeText(this, "Cannot open Bluetooth settings", Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static class WearableDeviceAdapter extends ArrayAdapter<BluetoothDevice> {
        private final Set<String> connectedNodes;

        public WearableDeviceAdapter(android.content.Context context, ArrayList<BluetoothDevice> devices, Set<String> connectedNodes) {
            super(context, 0, devices);
            this.connectedNodes = connectedNodes;
        }

        @Override
        public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
            if (convertView == null) {
                convertView = android.view.LayoutInflater.from(getContext()).inflate(R.layout.wearable_device_item, parent, false);
            }

            BluetoothDevice device = getItem(position);
            TextView nameView = convertView.findViewById(R.id.device_name);
            TextView addressView = convertView.findViewById(R.id.device_address);
            TextView statusView = convertView.findViewById(R.id.device_status);
            android.widget.ImageView iconView = convertView.findViewById(R.id.device_icon);

            nameView.setText(device.getName());
            addressView.setText(device.getAddress());

            // Simple status logic
            boolean isConnected = false;
            // Ideally we'd match Node ID to Address, but for now we rely on the service to potentially use address as ID
            // or we just show "Bonded" until we have better mapping.
            if (connectedNodes != null && connectedNodes.contains(device.getAddress())) {
                isConnected = true;
            }

            if (isConnected) {
                statusView.setText("Connected");
                statusView.setTextColor(getContext().getResources().getColor(android.R.color.holo_green_dark));
                iconView.setAlpha(1.0f);
            } else {
                statusView.setText("Bonded");
                statusView.setTextColor(getContext().getResources().getColor(android.R.color.darker_gray));
                iconView.setAlpha(0.6f);
            }

            return convertView;
        }
    }
}
