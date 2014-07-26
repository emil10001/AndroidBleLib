package io.hearty.ble.lib.data;

import android.bluetooth.BluetoothDevice;

import java.util.Comparator;

/**
 * Created by ejf3 on 5/16/14.
 *
 * BluetoothDevice is final, so couldn't simply extend it
 */
public class StoredBluetoothDevice implements Comparator<StoredBluetoothDevice> {
    private final BluetoothDevice device;

    public StoredBluetoothDevice(BluetoothDevice device) {
        this.device = device;
    }

    @Override
    public int compare(StoredBluetoothDevice x, StoredBluetoothDevice y) {
        return x.getAddress().compareTo(y.getAddress());
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    public String getAddress() {
        return device.getAddress();
    }

    public String getName() {
        return device.getName();
    }
}
