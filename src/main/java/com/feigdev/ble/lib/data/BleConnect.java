package com.feigdev.ble.lib.data;

import android.bluetooth.BluetoothDevice;

/**
 * Created by ejf3 on 4/15/14.
 */
public class BleConnect {
    public final BluetoothDevice device;

    public BleConnect(BluetoothDevice device) {
        this.device = device;
    }
}
