package com.feigdev.ble.lib.utils;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;

/**
 * Created by ejf3 on 4/15/14.
 */
public class BleUtils {

    public static boolean hasBle(Context context) {
        // Use this check to determine whether BLE is supported on the device. Then
        // you can selectively disable BLE-related features.
        return (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE));
    }

    public static BluetoothAdapter getAdapter(Context context) {
        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        return bluetoothManager.getAdapter();
    }

    public static String printDevice(BluetoothDevice device){
        return String.format("%s; %s; %s; %s", device.getAddress(), device.getName(), device.getType(), device.getBluetoothClass());
    }

}
