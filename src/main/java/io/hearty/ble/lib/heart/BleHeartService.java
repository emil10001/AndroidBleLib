package io.hearty.ble.lib.heart;

import android.app.Service;
import android.bluetooth.*;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import io.hearty.ble.lib.BleScanner;
import io.hearty.ble.lib.data.BleConnect;
import io.hearty.ble.lib.data.HeartRate;
import io.hearty.ble.lib.utils.BlePrefs;
import io.hearty.ble.lib.utils.SampleGattAttributes;
import io.hearty.witness.Reporter;
import io.hearty.witness.Witness;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class BleHeartService extends Service implements Reporter {
    // A service that interacts with the BLE device via the Android BLE API.
    private final static String TAG = "BleHeartService";
    private Handler handler = new Handler();

    private BluetoothGatt mBluetoothGatt;

    public final static String ACTION_GATT_CONNECTED =
            "io.hearty.ble.lib.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "io.hearty.ble.lib.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "io.hearty.ble.lib.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "io.hearty.ble.lib.ACTION_DATA_AVAILABLE";

    public static String UUID_HEART_RATE_MEASUREMENT =
            "00002a37-0000-1000-8000-00805f9b34fb";

    private BleScanner scanner;
    private BlePrefs prefs;

    public static boolean isRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = true;

        scanner = new BleScanner(getApplicationContext());
        prefs = new BlePrefs(this);

        Witness.register(BleConnect.class, this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        isRunning = true;

        if (!reconnectLeDevice())
            scanner.scanLeDevice(true);
        else
            scanner.scanLeDevice(false);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        isRunning = false;

        Witness.remove(BleConnect.class, this);

        if (null != scanner)
            scanner.scanLeDevice(false);

        if (null != mBluetoothGatt) {
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
        }

        super.onDestroy();
    }

    private void connectLeDevice(BluetoothDevice device) {
        mBluetoothGatt = device.connectGatt(BleHeartService.this, true, mGattCallback);
        Log.d(TAG, "connecting to " + device.getAddress());

        // remember the device you're connecting to
        prefs.setConnectedDevice(device);

        // stop scanning
        scanner.scanLeDevice(false);
    }

    private boolean reconnectLeDevice() {
        BluetoothDevice device = prefs.getConnectedDevice();
        if (null != device) {
            connectLeDevice(device);
            return true;
        }
        return false;
    }

    // Various callback methods defined by the BLE API.
    private final BluetoothGattCallback mGattCallback =
            new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                                    int newState) {
                    String intentAction;
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        intentAction = ACTION_GATT_CONNECTED;
                        broadcastUpdate(intentAction);
                        Log.i(TAG, "Connected to GATT server.");
                        Log.i(TAG, "Attempting to start service discovery:" +
                                mBluetoothGatt.discoverServices());
                        broadcastUpdate(intentAction);


                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        intentAction = ACTION_GATT_DISCONNECTED;
                        Log.i(TAG, "Disconnected from GATT server.");
                        broadcastUpdate(intentAction);
                    }
                }

                @Override
                // New services discovered
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                    } else {
                        Log.w(TAG, "onServicesDiscovered received: " + status);
                    }
                }

                @Override
                // Result of a characteristic read operation
                public void onCharacteristicRead(BluetoothGatt gatt,
                                                 BluetoothGattCharacteristic characteristic,
                                                 int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                    }
                }

                @Override
                // Characteristic notification
                public void onCharacteristicChanged(BluetoothGatt gatt,
                                                    BluetoothGattCharacteristic characteristic) {
                    broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                }

            };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void broadcastUpdate(final String action) {
        broadcastUpdate(action, null);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {

        Log.d(TAG, "broadcastUpdate");

        if (ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
            // Show all the supported services and characteristics on the
            // user interface.
            registerHeartRate(mBluetoothGatt.getServices());
        }

        if (null == characteristic)
            return;

        // This is special handling for the Heart Rate Measurement profile. Data
        // parsing is carried out as per profile specifications.
        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid().toString())) {
            int flag = characteristic.getProperties();
            int format;
            if ((flag & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
                Log.d(TAG, "Heart rate format UINT16.");
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
                Log.d(TAG, "Heart rate format UINT8.");
            }
            final HeartRate heartRate = new HeartRate(characteristic.getIntValue(format, 1));
            Log.d(TAG, String.format("Received heart rate: %d", heartRate.getHeartRate()));
            Witness.notify(heartRate);
        } else {
            // For all other profiles, writes the data formatted in HEX.
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for (byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));
                Log.d(TAG, "new value " + stringBuilder.toString());
            }
        }
    }

    // Demonstrates how to iterate through the supported GATT
    // Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the
    // ExpandableListView on the UI.
    private void registerHeartRate(List<BluetoothGattService> gattServices) {
        if (gattServices == null)
            return;

        Log.d(TAG, "registerHeartRate list size: " + gattServices.size());

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            String uuid = gattService.getUuid().toString();
            String service = SampleGattAttributes.lookup(uuid, "unknown service");

            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                String cUuid = gattCharacteristic.getUuid().toString();

                if (UUID_HEART_RATE_MEASUREMENT.equals(cUuid)) {
                    Log.d(TAG, String.format("charactersitics for %s: %s", cUuid, SampleGattAttributes.lookup(cUuid, "unknown characteristic")));
                    mBluetoothGatt.setCharacteristicNotification(gattCharacteristic, true);
                    BluetoothGattDescriptor descriptor = gattCharacteristic.getDescriptor(
                            UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    mBluetoothGatt.writeDescriptor(descriptor);
                    return;
                }
            }
        }

    }

    @Override
    public void notifyEvent(final Object o) {
        handler.post(new Runnable() {
            @Override
            public void run() {

                if (o instanceof BleConnect) {
                    connectLeDevice(((BleConnect) o).device);
                }

            }
        });
    }
}