/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jk.ut61eTool;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import com.jake.UT61e_decoder;

import java.util.UUID;

/**
 * For a given BLE device, this Activity provides the user interface to display data,
 * from the serial GATT characteristic of the multimeter.
 * The Activity communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class LogActivity extends Activity implements SharedPreferences.OnSharedPreferenceChangeListener {
    private final static String TAG = LogActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private TextView mConnectionState, mDataField;
    private String mDeviceAddress;
    private BluetoothLeService mBluetoothLeService;
    private boolean mConnected = false;
    private UUID uuid;

    NotificationManager mNotifyMgr;
    private boolean connection_wanted;

    GraphUI graphUI;
    UI ui;
    Alarms alarm;
    DataLogger logger;
    private boolean ignore_ol, shunt_mode;
    private double shunt_value;

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
            connection_wanted = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch (action) {
                case BluetoothLeService.ACTION_GATT_CONNECTED:
                    mConnected = true;
                    updateConnectionState();
                    invalidateOptionsMenu();
                    break;
                case BluetoothLeService.ACTION_GATT_DISCONNECTED:
                    mConnected = false;
                    updateConnectionState();
                    invalidateOptionsMenu();
                    mDataField.setText(R.string.no_data);
                    break;
                case BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED:
                    boolean foundUUID = false;
                    for (BluetoothGattService gattService : mBluetoothLeService.getSupportedGattServices()) {
                        BluetoothGattCharacteristic characteristic = gattService.getCharacteristic(uuid);
                        if (characteristic != null) {
                            mBluetoothLeService.setCharacteristicNotification(characteristic, true);
                            foundUUID = true;
                            break;
                        }
                    }
                    if (!foundUUID) {
                        Log.e(TAG, "No suitable Characteristic found");
                    }
                    break;
                case BluetoothLeService.ACTION_DATA_AVAILABLE:
                    updateConnectionState();
                    if (!connection_wanted) return;
                    byte[] extra = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                    decodeData(extra);
                    break;
            }

        }
    };


    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.log_activity);
        findViews();

        graphUI = new GraphUI(this, findViewById(R.id.graph), findViewById(R.id.dataInfo), R.color.blePrimary);
        ui = new UI(this);

        alarm = new Alarms(this);

        logger = new DataLogger(this);

        PreferenceManager.setDefaultValues(this, R.xml.prefs, false);
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
        loadSettings();

        final Intent intent = getIntent();
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        getActionBar().setTitle(intent.getStringExtra(EXTRAS_DEVICE_NAME));
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    private void findViews() {
        mConnectionState = findViewById(R.id.connection_state);
        mDataField = findViewById(R.id.data_value);


    }


    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (logger.fWriter == null && !alarm.enabled) {
            unregisterReceiver(mGattUpdateReceiver);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mGattUpdateReceiver);
        unbindService(mServiceConnection);
        mBluetoothLeService = null;

        logger.stopLog();
        mNotifyMgr.cancelAll();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (connection_wanted) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_connect:
                connection_wanted = true;
                mBluetoothLeService.connect(mDeviceAddress);
                updateConnectionState();
                invalidateOptionsMenu();
                return true;
            case R.id.menu_disconnect:
                connection_wanted = false;
                mBluetoothLeService.disconnect();
                updateConnectionState();
                invalidateOptionsMenu();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.settings:
                startActivity(new Intent(this, SettingsActivity.class));
        }
        updateConnectionState();
        return super.onOptionsItemSelected(item);
    }

    private void decodeData(byte[] data) {
        UT61e_decoder ut61e = new UT61e_decoder();
        if (ut61e.parse(data)) {

            if (shunt_mode && shunt_value != 0.0 && ut61e.getMode() == ut61e.MODE_VOLTAGE) {
                ut61e.value = ut61e.value/shunt_value; //I = U/R
                if (ut61e.unit_str.equals("mV")) ut61e.value /= 1000.0;
                ut61e.unit_str = "A (EXT. SHUNT)";
            }

            ui.update(ut61e);

            if (alarm.isAlarm(ut61e.getValue())) {
                alarm.alarm(ut61e.toString());
            }
            if (ignore_ol && (ut61e.isOL() || ut61e.isUL())) {
                //Log.i("GRAPH/LOG", "Skipped ol/ul value");
                return;
            }

            graphUI.displayData(ut61e);
            graphUI.updateDataInfo();

            logger.logData(ut61e.toCSVString());
            putLogNotify(logger.lineCount);
        }
    }

    private void updateConnectionState() {
        runOnUiThread(() -> {
            if (mConnected && connection_wanted) {
                mConnectionState.setText(R.string.connected);
            } else if (!mConnected && !connection_wanted){
                mConnectionState.setText(R.string.disconnected);
            } else {
                mConnectionState.setText(R.string.working);
            }
        });
    }

    private void putLogNotify(int points) {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.tile)
                        .setContentTitle("Logging Running")
                        .setContentText(points + " Data points");
        mBuilder.setOngoing(true);

        Intent resultIntent = new Intent(this, LogActivity.class);
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        resultIntent.setAction("android.intent.action.MAIN");
        resultIntent.addCategory("android.intent.category.LAUNCHER");
        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);

        mNotifyMgr.notify(1, mBuilder.build());

    }

    @SuppressWarnings("unused")
    public void onSwitchClick(View v) {
        Switch sw = (Switch) v;
        if (sw.isChecked()) {
            logger.startLog();
        } else {
            logger.stopLog();
        }
    }


    private void loadSettings() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        graphUI.viewSize = Integer.valueOf(prefs.getString("viewport", "60"));
        uuid = UUID.fromString(prefs.getString("uuid", ""));
        alarm.enabled = prefs.getBoolean("alarm_enabled", false);
        alarm.condition = prefs.getString("alarm_condition", "0");
        alarm.samples = Integer.valueOf(prefs.getString("samples", "3"));
        alarm.low_limit = Double.valueOf(prefs.getString("low_limit", "0"));
        alarm.high_limit = Double.valueOf(prefs.getString("high_limit", "0"));
        alarm.vibration = prefs.getBoolean("vibration", true);
        alarm.sound = prefs.getString("sound", "");
        logger.log_dir = prefs.getString("log_folder", getString(R.string.log_folder));
        ignore_ol = prefs.getBoolean("ignore_ol", false);
        shunt_mode = prefs.getBoolean("shunt_mode", false);
        shunt_value = Double.valueOf(prefs.getString("shunt_ohm", "1.0"));
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        loadSettings();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            logger.startLog();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


}
