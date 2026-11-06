package org.microg.gms.wearable.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import androidx.annotation.RequiresPermission;

import java.util.Calendar;
import java.util.TimeZone;

public class BleServicesHandlerImpl implements BleServicesHandler {
    private static final String TAG = "BleServicesHandlerImpl";

    private final BluetoothGattHelperImpl gattHelper;

    BleServicesHandlerImpl(BluetoothGattHelperImpl gattHelper) {
        this.gattHelper = gattHelper;
    }

    @Override
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void updateCurrentTime() throws BleException {
        BluetoothGatt g = gattHelper.getGatt();
        if (g == null) throw new BleException("GATT not connected");

        BluetoothGattService cts = g.getService(BluetoothGattHelperImpl.UUID_CTS);
        if (cts == null) {
            throw new BleException("Current Time Service not found",
                    BleException.CODE_TIME_SERVICE_NOT_FOUND);
        }

        BluetoothGattCharacteristic ctChar =
                cts.getCharacteristic(BluetoothGattHelperImpl.UUID_CURRENT_TIME);
        if (ctChar == null) {
            throw new BleException("Current Time characteristic not found",
                    BleException.CODE_TIME_CHAR_INVALID);
        }

        Calendar now = Calendar.getInstance(TimeZone.getDefault());
        byte[] value = new byte[10];
        int year = now.get(Calendar.YEAR);
        value[0] = (byte) (year & 0xFF);
        value[1] = (byte) ((year >> 8) & 0xFF);
        value[2] = (byte) (now.get(Calendar.MONTH) + 1);
        value[3] = (byte) now.get(Calendar.DAY_OF_MONTH);
        value[4] = (byte) now.get(Calendar.HOUR_OF_DAY);
        value[5] = (byte) now.get(Calendar.MINUTE);
        value[6] = (byte) now.get(Calendar.SECOND);
        int dow = now.get(Calendar.DAY_OF_WEEK);
        value[7] = (byte) (dow == Calendar.SUNDAY ? 7 : dow - 1);
        value[8] = 0;
        value[9] = 0x02;

        ctChar.setValue(value);
        ctChar.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);

        if (!g.writeCharacteristic(ctChar)) {
            throw new BleException("writeCharacteristic(currentTime) failed");
        }
        Log.d(TAG, "updateCurrentTime written");
    }

    @Override
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void cleanup() {
        BluetoothGatt g = gattHelper.getGatt();
        if (g == null) return;
        try {
            BluetoothGattService cts = g.getService(BluetoothGattHelperImpl.UUID_CTS);
            if (cts == null) return;
            BluetoothGattCharacteristic c =
                    cts.getCharacteristic(BluetoothGattHelperImpl.UUID_CURRENT_TIME);
            if (c == null) return;
            g.setCharacteristicNotification(c, false);
            BluetoothGattDescriptor cccd = c.getDescriptor(BluetoothGattHelperImpl.UUID_CCCD);
            if (cccd != null) {
                cccd.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                g.writeDescriptor(cccd);
            }
            Log.d(TAG, "cleanup: notifications disabled");
        } catch (Exception e) {
            Log.w(TAG, "cleanup error: " + e);
        }
    }
}
