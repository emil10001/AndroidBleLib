package com.feigdev.ble.lib;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.feigdev.ble.lib.data.DeviceFound;
import com.feigdev.ble.lib.data.StoredBluetoothDevice;
import com.feigdev.ble.lib.utils.BleUtils;
import com.feigdev.witness.Witness;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ejf3 on 5/18/14.
 */
public class BleScanner {
    private final static String TAG = "BleScanner";
    private static final ConcurrentHashMap<String, StoredBluetoothDevice> devices
            = new ConcurrentHashMap<String, StoredBluetoothDevice>();
    private static final long SCAN_PERIOD = 45000;
    private static final long SCAN_RESET = 5 * 60 * 1000; // flush the device list after 5 minutes

    private final BluetoothAdapter bleAdapter;
    private BluetoothAdapter.LeScanCallback mLeScanCallback;
    private Handler mHandler = new Handler(Looper.myLooper());
    private boolean mScanning = false;

    public BleScanner(Context context) {
        Log.d(TAG, "initBle");
        bleAdapter = BleUtils.getAdapter(context);
        mLeScanCallback =
                new BluetoothAdapter.LeScanCallback() {

                    @Override
                    public void onLeScan(final BluetoothDevice device, int rssi,
                                         byte[] scanRecord) {
                        if (BluetoothDevice.DEVICE_TYPE_UNKNOWN != device.getType()
                                && BluetoothDevice.DEVICE_TYPE_LE != device.getType()
                                && BluetoothDevice.DEVICE_TYPE_DUAL != device.getType()) {
                            Log.d(TAG, "failed, not LE " + device.getType());
                            return;
                        }

                        // we only want to take action on new devices found
                        if (!devices.containsKey(device.getAddress())) {
                            Log.d(TAG, "found " + BleUtils.printDevice(device));
                            devices.put(device.getAddress(), new StoredBluetoothDevice(device));
                            Witness.notify(new DeviceFound(device));
                        }
                    }
                };
    }


    public void scanLeDevice(final boolean enable) {
        Log.d(TAG, "scanLeDevice");
        if (mScanning && enable)
            return;

        if (enable) {
            mScanning = true;

            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "postDelayed ...");
                    mScanning = false;
                    bleAdapter.stopLeScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!mScanning)
                        devices.clear();
                }
            }, SCAN_RESET);

            bleAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            bleAdapter.stopLeScan(mLeScanCallback);
        }
    }

    public static ConcurrentHashMap<String, StoredBluetoothDevice> getDevices(){
        return devices;
    }

}
