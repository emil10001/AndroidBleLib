package com.feigdev.ble.lib.utils;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;

/**
 * Created by ejf3 on 5/18/14.
 */
public class BlePrefs {
    private static final String TAG = "BlePrefs";

    private static final String BLE_DEVICE = "ble_device";

    private SharedPreferences preferences;

    public BlePrefs(Context context) {
        preferences = context
                .getSharedPreferences("auth", Context.MODE_PRIVATE);
    }

    public void setConnectedDevice(BluetoothDevice device) {
        Gson gson = new Gson();
        String objStr = gson.toJson(device);

        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(BLE_DEVICE, objStr);
        editor.commit();
    }

    public BluetoothDevice getConnectedDevice() {
        String objStr = preferences.getString(BLE_DEVICE, null);
        if (null == objStr)
            return null;

        Gson gson = new Gson();
        return gson.fromJson(objStr, BluetoothDevice.class);
    }

}
