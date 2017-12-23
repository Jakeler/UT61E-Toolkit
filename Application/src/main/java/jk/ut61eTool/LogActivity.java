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

import android.Manifest;
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
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.jake.UT61e_decoder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Map;
import java.util.UUID;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class LogActivity extends Activity implements SharedPreferences.OnSharedPreferenceChangeListener {
    private final static String TAG = LogActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private TextView mConnectionState, mDataField;
    private String mDeviceName, mDeviceAddress;
    private BluetoothLeService mBluetoothLeService;
    private boolean mConnected = false;
    private UUID uuid;

    private FileWriter fWriter;
    private File logFile;
    private EditText filename;
    private TextView fileInfo;
    private ProgressBar logRunning;
    private int lineCount = 0;
    private Toast startLogToast;

    NotificationManager mNotifyMgr;
    private boolean auto_reconnect;

    GraphUI ui;
    Alarms alarm;

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
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                mDataField.setText(R.string.no_data);
                reconnect();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
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
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                byte[] extra = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                decodeData(extra);
            }

        }
    };

    public void reconnect() {
        if (auto_reconnect) {
            mBluetoothLeService.connect(mDeviceAddress);
            Log.i(TAG, "reconnect: " + auto_reconnect);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.log_activity);
        findViews();

        ui = new GraphUI(this);
        alarm = new Alarms(this);

        PreferenceManager.setDefaultValues(this, R.xml.prefs, false);
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
        loadSettings();

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    private void findViews() {
        mConnectionState = (TextView) findViewById(R.id.connection_state);
        mDataField = (TextView) findViewById(R.id.data_value);

        logRunning = (ProgressBar) findViewById(R.id.logRunning);
        filename = (EditText) findViewById(R.id.filename);
        fileInfo = (TextView) findViewById(R.id.fileInfo);
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
        if (fWriter == null && !alarm.enabled) {
            unregisterReceiver(mGattUpdateReceiver);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
        if (fWriter != null) {
            stopLog();
        }
        mNotifyMgr.cancelAll();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
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
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.settings:
                startActivity(new Intent(this, SettingsActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    private void decodeData(byte[] data) {
        UT61e_decoder ut61e = new UT61e_decoder();
        if (ut61e.parse(data)) {

            ui.displayData(ut61e);

            logData(ut61e.toCSVString());

            if (alarm.isAlarm(ut61e.getValue())) {
                alarm.alarm(ut61e.toString());
            }
        }
    }

    private void logData(String data) {
        if (fWriter != null) {
            try {
                fWriter.write(data + "\n");
                fWriter.flush();
                lineCount++;
            } catch (IOException e) {
                e.printStackTrace();
            }
            fileInfo.setText("Path: " + logFile.getPath() + "\n" +
                    "Size: " + String.format("%.2f", logFile.length() / 1000.0) + " KB  (" + lineCount + " Data points)");
            putNotify(lineCount);
        }
    }


    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }

    private void putNotify(int points) {
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

    public void onSwitchClick(View v) {
        Switch sw = (Switch) v;
        if (sw.isChecked()) {
            startLog();
        } else {
            stopLog();
        }
    }


    private void startLog() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 7);
        }

        createFolder();
        logFile = new File(Environment.getExternalStorageDirectory() + File.separator + getString(R.string.log_folder) + File.separator + filename.getText());
        Log.d("TAG", logFile.getPath()); //<-- check the log to make sure the path is correct.
        Map<String, ?> prefs = PreferenceManager.getDefaultSharedPreferences(this).getAll();
        Log.d(TAG, "PREFS: " + prefs.get("viewport"));

        Switch sw = (Switch) findViewById(R.id.switch1);
        try {
            fWriter = new FileWriter(logFile, true);
            fWriter.write("### " + Calendar.getInstance().getTime().toString() + " ###\n");
            fWriter.write(UT61e_decoder.csvHeader + "\n");
            fWriter.flush();
            filename.setEnabled(false);
            logRunning.setIndeterminate(true);
            lineCount = 0;
            sw.setChecked(true);
        } catch (IOException e) {
            startLogToast = Toast.makeText(this, getString(R.string.storage_exp) + e.getMessage(), Toast.LENGTH_LONG);
            startLogToast.show();
            sw.setChecked(false);
        }
    }

    private boolean createFolder() {
        File folder = new File(Environment.getExternalStorageDirectory() + File.separator + getString(R.string.log_folder));
        if (!folder.exists()) {
            return folder.mkdirs();
        }
        return false;
    }

    private void stopLog() {
        try {
            fWriter.flush();
            fWriter.close();
            fWriter = null;
            filename.setEnabled(true);
            logRunning.setIndeterminate(false);
            mNotifyMgr.cancel(1);
        } catch (IOException | NullPointerException e) {
            Toast.makeText(this, getString(R.string.storage_exp) + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }


    private void loadSettings() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        ui.viewSize = Integer.valueOf(prefs.getString("viewport", "60"));
        uuid = UUID.fromString(prefs.getString("uuid", ""));
        auto_reconnect = prefs.getBoolean("reconnect", false);
        alarm.enabled = prefs.getBoolean("alarm_enabled", false);
        alarm.condition = prefs.getString("alarm_condition", "0");
        alarm.samples = Integer.valueOf(prefs.getString("samples", "3"));
        alarm.low_limit = Double.valueOf(prefs.getString("low_limit", "0"));
        alarm.high_limit = Double.valueOf(prefs.getString("high_limit", "0"));
        alarm.vibration = prefs.getBoolean("vibra", true);
        alarm.sound = prefs.getString("sound", "");
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        loadSettings();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (startLogToast != null) {
                startLogToast.cancel();
            }
            startLog();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


}
