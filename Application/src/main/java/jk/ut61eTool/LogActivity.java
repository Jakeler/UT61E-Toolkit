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
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.databinding.DataBindingUtil;

import com.jake.UT61e_decoder;

import java.util.UUID;

import jk.ut61eTool.databinding.LogActivityBinding;


/**
 * For a given BLE device, this Activity provides the user interface to display data,
 * from the serial GATT characteristic of the multimeter.
 * The Activity communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class LogActivity extends Activity implements SharedPreferences.OnSharedPreferenceChangeListener {
    private final static String TAG = LogActivity.class.getSimpleName();

    public LogActivityBinding binding;

    private String mDeviceAddress;
    private BluetoothLeService mBluetoothLeService;
    private boolean mConnected = false;
    private boolean connection_wanted;
    private UUID uuid;

    NotificationManager mNotifyMgr;

    GraphUI graphUI;
    UI ui;
    Alarms alarm;
    DataLogger logger;
    Converter conv;

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
                    binding.dataValue.setText(R.string.no_data);
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
        binding = DataBindingUtil.setContentView(this, R.layout.log_activity);

        graphUI = new GraphUI(this, binding.graph, binding.dataInfo, R.color.blePrimary);
        ui = new UI(binding);

        mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        alarm = new Alarms(this);
        logger = new DataLogger(this);
        ui.setLogger(logger);

        conv = new Converter();

        PreferenceManager.setDefaultValues(this, R.xml.prefs, false);
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
        loadSettings();

        final Intent intent = getIntent();
        mDeviceAddress = intent.getStringExtra(DeviceScanActivity.EXTRAS_DEVICE_ADDRESS);
        if (mDeviceAddress == null) {
            finish();
            Log.e("LogActivity", "no device address");
            startActivity(new Intent(this, StartActivity.class));
        }
        getActionBar().setTitle(intent.getStringExtra(DeviceScanActivity.EXTRAS_DEVICE_NAME));
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
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
        if (!logger.isRunning() && !alarm.enabled) {
            unregisterReceiver(mGattUpdateReceiver);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {unregisterReceiver(mGattUpdateReceiver);}
        catch(IllegalArgumentException e){Log.w("GATT RECEIVER", "already unregistered");}
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

            conv.adjust(ut61e);

            ui.update(ut61e);

            if (alarm.isAlarm(ut61e.getValue())) {
                alarm.alarm(ut61e.toString());
            }
            if (conv.isIgnored(ut61e)) {
                return;
            }

            graphUI.displayData(ut61e);
            graphUI.updateDataInfo();

            logger.logData(ut61e.toCSVString());
        }
    }

    private void updateConnectionState() {
        runOnUiThread(() -> {
            if (mConnected && connection_wanted) {
                binding.connectionState.setText(R.string.connected);
            } else if (!mConnected && !connection_wanted){
                binding.connectionState.setText(R.string.disconnected);
            } else {
                binding.connectionState.setText(R.string.working);
            }
        });
    }

    private void loadSettings() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        graphUI.viewSize = Integer.parseInt(prefs.getString("viewport", "60"));
        uuid = UUID.fromString(prefs.getString("uuid", ""));
        alarm.enabled = prefs.getBoolean("alarm_enabled", false);
        alarm.condition = prefs.getString("alarm_condition", "0");
        alarm.samples = Integer.parseInt(prefs.getString("samples", "3"));
        alarm.low_limit = Double.parseDouble(prefs.getString("low_limit", "0"));
        alarm.high_limit = Double.parseDouble(prefs.getString("high_limit", "0"));
        alarm.vibration = prefs.getBoolean("vibration", true);
        logger.log_dir = prefs.getString("log_folder", "");
        logger.setReuseLogfile(!prefs.getBoolean("no_logfile_reuse", false));
        conv.ignore_ol = prefs.getBoolean("ignore_ol", false);
        conv.shunt_mode = prefs.getBoolean("shunt_mode", false);
        conv.shunt_value = Double.parseDouble(prefs.getString("shunt_ohm", "1.0"));
        conv.tc_mode = prefs.getBoolean("tc_mode", false);
        conv.tc_sens = Double.parseDouble(prefs.getString("tc_sens", "0.039"));
        conv.tc_ref_id = Integer.parseInt(prefs.getString("tc_reference", "-1"));
        conv.tc_ref_constant = Double.parseDouble(prefs.getString("tc_ref_constant", "20.0"));
        conv.tr_mode = prefs.getBoolean("tr_mode", false);
        conv.tr_res = Double.parseDouble(prefs.getString("tr_ohm", "100000"));
        conv.tr_beta = Double.parseDouble(prefs.getString("tr_beta", "4092"));
        conv.rtd_mode = prefs.getBoolean("rtd_mode", false);
        conv.rtd_res = Double.parseDouble(prefs.getString("rtd_ohm", "1000"));
        conv.rtd_alpha = Double.parseDouble(prefs.getString("rtd_alpha", "0.00385"));
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        loadSettings();
    }
}
