AndroidBleLib
=============

This is a work-in-progress Bluetooth LE / 4.0 library. Currently works with Heart Rate Monitors. I'll be using this for a production project, so the structure and API might change over time.

This depends on the [Witness](https://github.com/emil10001/Witness) library.

### Features

* Simple to pull in and get a BLE heart rate monitor working
* Leverages an event emitter, Witness
* Remember last connected device

### Usage

The first thing that we need to know is whether or not we've ever connected to a BLE device. This library assumes that there will be a single device that you will use.

Test to see if you have a stored device:

    BlePrefs prefs = new BlePrefs((Context)this);
    if (null == prefs.getConnectedDevice())

If you do have a stored device, you're in luck. All you need to do is start the `BleHeartService`, and it will automatically get you connected to the last known device.

    startService(new Intent(this, BleHeartService.class));

If you do not have a stored device, it's a good idea to find and connect to one. Here's a look at our `PairingActivity`, which is responsible for displaying all of the devices that we've found:

    public class PairingActivity extends Activity implements Reporter {
        private Handler handler = new Handler();

        // ...

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            Witness.register(DeviceFound.class, this);
            startService(new Intent(this, BleHeartService.class));

            // probably create some adapter to add devices to
            // ...

        }

        @Override
        public void onDestroy() {
            Witness.remove(DeviceFound.class, this);
            super.onDestroy();
        }

        private void addDeviceToAdapter(final BluetoothDevice device) {
            // add the device to the adapter
        }

        @Override
        public void notifyEvent(final Object o) {
            handler.post(new Runnable() {
                @Override
                public void run() {

                    if (o instanceof DeviceFound) {
                        // we found a device!
                        addDeviceToAdapter(((DeviceFound) o).device);
                    }

                }
            });
        }
    }

We now have a list of devices that we can connect to. Here's an example of how to connect to an item in an adapter:

    // whenever we click on an item, we want to make sure to store it,
    // start the service, and notify anything that needs to know
    mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            // obviously, change this per your specific implementation
            String address = mAdapter.getItem(position).getAddressString();
            StoredBluetoothDevice device = BleScanner.getDevices().get(address);

            // notify anyone listening
            Witness.notify(new BleConnect(device.getDevice()));

            // we need to set this here to avoid a race condition
            prefs.setConnectedDevice(device.getDevice());

            // I'm using startService because this is Glassware
            // this kicks off the thing that is going to display the heart rate
            startService(new Intent(PairingActivity.this, HeartyLiveCardService.class));

            // get rid of the PairingActivity
            finish();
        }
    });

We've connected, and now are kicking off our display:

    public class HeartyLiveCardService extends Service implements Reporter {
        private final Handler handler = new Handler();

        @Override
        public void onCreate() {
            super.onCreate();
            Witness.register(HeartRate.class, this);
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            startService(new Intent(this, BleHeartService.class));

            // build LiveCard for Glass
            // ...

            return START_STICKY;
        }

        @Override
        public void onDestroy() {
            Witness.remove(HeartRate.class, this);
            super.onDestroy();
        }

        private void notifyHeartRate(HeartRate heartRate) {
            // Set up initial RemoteViews values
            mLiveCardView.setTextViewText(R.id.heart_rate,
                String.valueOf(heartRate.getHeartRate()));
            mLiveCard.setViews(mLiveCardView);
        }

        @Override
        public void notifyEvent(final Object o) {
            handler.post(new Runnable() {
                @Override
                public void run() {

                    if (o instanceof HeartRate) {
                        notifyHeartRate(((HeartRate) o));
                    }

                }
            });
        }
    }

That's pretty much it! At this point, you've successfully connected your device, and are displaying data.
