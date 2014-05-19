package com.feigdev.ble.lib.data;

import android.bluetooth.BluetoothDevice;

/**
 * Created by ejf3 on 5/16/14.
 */
public class DeviceFound {
    public final BluetoothDevice device;

    public DeviceFound(BluetoothDevice device) {
        this.device = device;
    }
}
