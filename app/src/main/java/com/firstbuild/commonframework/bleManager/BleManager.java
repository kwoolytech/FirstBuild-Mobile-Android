/**
 * @file BleManager.java
 * @brief BleManager handles all kind of interface with ble devices
 * @author Ryan Lee - 320006284
 * @date May/22/2015
 * Copyright (c) 2014 General Electric Corporation - Confidential - All rights reserved.
 */

package com.firstbuild.commonframework.bleManager;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.firstbuild.commonframework.deviceManager.Device;
import com.firstbuild.commonframework.deviceManager.DeviceManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BleManager {

    private final String TAG = getClass().getSimpleName();

    // Set enable bluetooth feature
    private int connectionState = BleValues.STATE_DISCONNECTED;

    private HashMap<String, BluetoothDevice> scannedDevices = new HashMap<String, BluetoothDevice>();

    // Blue tooth Gatt handler
    private BluetoothGatt bluetoothGatt;

    // Bluetooth adapter handler
    private BluetoothAdapter bluetoothAdapter = null;
    private BluetoothManager bluetoothManager = null;

    // Flag for checking device scanning state
    public boolean isScanning = false;

    // Post Delayed handler
    private Handler handler = new Handler();
    private Runnable stopScanRunnable = null;
    private String deviceAddress = "";
    private Context context = null;
    private HashMap<String, BleListener> callbacks = null;

    public static BleManager instance = new BleManager();
    public static BleManager getInstance(){
        return instance;
    }

    public BleManager(){
        // Default constructor
    }

    public void initBleManager(Context context){
        Log.d(TAG, "initBleManager IN");

        this.context = context;
        bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
    }

    public void addListener(BleListener listener){
        if (callbacks == null) {
            callbacks = new HashMap<String, BleListener>();
        }
        else{
            // Do nothing
        }

        // Key is listener is casted string
        // Value is listener itself.
        callbacks.put(listener.toString(), listener);
    }

    public void removeListener(BleListener listener){
        if (callbacks != null && !callbacks.isEmpty()) {
            callbacks.remove(listener.toString());
            Log.d(TAG, "Remove Listener: " + listener);
        }
        else{
            // Do nothing
        }
    }

    /**
     * Send update to subscribers
     * @param listener subscriber
     * @param args corresponding arguments
     */
    public void sendUpdate(String listener, Object... args){
        Log.d(TAG, "sendUpdate: " + "IN");

        // Clone hashmap to avoid java.util.ConcurrentModificationException
        HashMap<String, BleListener> callbackClone = (HashMap) callbacks.clone();
        for (Map.Entry<String, BleListener> entry : callbackClone.entrySet()) {
            BleListener callback = entry.getValue();

            if (callback != null && listener.equals("onScanStateChanged")) {
                callback.onScanStateChanged((int) args[0]);
            }
            else if (callback != null && listener.equals("onScanDevices")) {
                callback.onScanDevices((HashMap<String, BluetoothDevice>) args[0]);
            }
            else if (callback != null && listener.equals("onConnectionStateChanged")) {
                callback.onConnectionStateChanged((String) args[0], (int) args[1]);
            }
            else if (callback != null && listener.equals("onServicesDiscovered")) {
                callback.onServicesDiscovered((String) args[0], (List<BluetoothGattService>) args[1]);
            }
            else if (callback != null && listener.equals("onCharacteristicChanged")){
                callback.onCharacteristicChanged((String) args[0], (String) args[1], (String) args[2]);
            }
            else{
                // Do nothing
            }
        }
    }

    // Code to manage Service lifecycle.
    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.d(TAG, "onServiceConnected");

            // Automatically connects to the device upon successful start-up initialization.
            if (!deviceAddress.equals("")) {

                Log.d(TAG, "Address: " + deviceAddress);
                connect(deviceAddress);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
//            bluetoothLeService = null;
            // TODO: Remove item from device manager
        }
    };

    public boolean connect(final String address) {
        Log.d(TAG, "connect IN");

        if (bluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (deviceAddress != null && address.equals(deviceAddress) && bluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing BluetoothGatt for connection.");
            if (bluetoothGatt.connect()) {
                connectionState = BleValues.STATE_CONNECTING;
                return true;
            }
            else {
                return false;
            }
        }

        final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);

        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }

        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        bluetoothGatt = device.connectGatt(context, false, bluetoothGattCallback);

        Log.d(TAG, "Trying to create a new connection.");
        deviceAddress = address;
        connectionState = BleValues.STATE_CONNECTING;
        return true;
    }

    public void disconnect(){
        Log.d(TAG, "disconnect IN");

        // Disconnect bluetooth connection
        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect();

            bluetoothGatt = null;
            bluetoothAdapter = null;
        }
        else{
            // Do nothing
        }
    }

    /**
     * Check bluetooth feature in the phone is enabled
      * @return enabled or disabled
     */
    public boolean isBluetoothEnabled(){
        Log.d(TAG, "isBluetoothEnabled IN");

        return bluetoothAdapter.isEnabled();
    }

    /**
     * Check device is already bonded one.
     * @param address BLE device's address.
     * @return true or false
     */
    private boolean isDevicePaired(final String address) {
        boolean result = false;

        if(address != null) {

            // Retrieves paired device list
            Set<BluetoothDevice> pairedDevice = bluetoothAdapter.getBondedDevices();
            if (pairedDevice != null && pairedDevice.size() > 0) {

                // Iterate all the device in the list
                for (BluetoothDevice device : pairedDevice) {
                    Log.d(TAG, device.getName() + "\n" + device.getAddress());

                    if (device.getAddress().equals(address)) {

                        // Device found
                        deviceAddress = device.getAddress();
                        result = true;
                    }
                }
            }
        }

        return result;
    }


    /**
     * start device scan for duration
     * @param duration 1 to 120 sec.
     */
    public boolean startScan(final int duration) {
        Log.d(TAG, "startScan IN");

        boolean result = false;

        if(duration > 0 && duration <= 120) {
            // Check duration
            isScanning = true;
            bluetoothAdapter.startLeScan(leScanCallback);

            // Stops scanning after a pre-defined scan period.
            handler.postDelayed(setStopScanRunnable(), duration);

            result = true;
            sendUpdate("onScanStateChanged", new Object[]{BleValues.START_SCAN});
        }
        else{
            Log.d(TAG, "duration is out of range(1 - 120 sec): " + duration);
            result = false;
        }

        return result;
    }

    /**
     * Start device scan for 10 sec
     */
    public void startScan() {
        Log.d(TAG, "startScan IN");

        isScanning = true;
        bluetoothAdapter.startLeScan(leScanCallback);

        // Stops scanning after a pre-defined scan period.
        handler.postDelayed(setStopScanRunnable(), BleValues.SCAN_PERIOD);

        sendUpdate("onScanStateChanged", new Object[]{BleValues.START_SCAN});
    }

    /**
     * Stop scanning BLE devices
     */
    public void stopScan() {
        Log.d(TAG, "stopScan IN");

        isScanning = false;
        bluetoothAdapter.stopLeScan(leScanCallback);

        if(stopScanRunnable != null){
            Log.d(TAG, "Remove delayed stopScan task");
            handler.removeCallbacks(stopScanRunnable);
        }

        sendUpdate("onScanStateChanged", new Object[]{BleValues.STOP_SCAN});
    }

    private Runnable setStopScanRunnable(){
        stopScanRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Scan ble device time out!");
                stopScan();
            }
        };

        return stopScanRunnable;
    }

    /**
     * Device scan callback
     */
    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            try {
                if (device != null && device.getName() != null){
                    scannedDevices.put(device.getAddress(), device);

                    // Notify updates to other modules
                    sendUpdate("onScanDevices", new Object[]{scannedDevices});
                }
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
    };


    /**
     * Implements callback methods for GATT events that the app cares about.
     * For example, connection change and services discovered.
     */
    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.d(TAG, "onConnectionStateChange IN");

            String address = gatt.getDevice().getAddress();

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Connected to GATT server.");

                connectionState = BleValues.STATE_CONNECTED;

                // Add ble device in the device manager
                DeviceManager.getInstance().add(new Device(address));

                sendUpdate("onConnectionStateChanged", new Object[]{address, connectionState});

                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery: " + bluetoothGatt.discoverServices());
            }
            else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from GATT server.");

                connectionState = BleValues.STATE_DISCONNECTED;

                // Remove ble device from the device manager
                DeviceManager.getInstance().remove(address);

                sendUpdate("onConnectionStateChanged", new Object[]{address, connectionState});
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG, "onServicesDiscovered");

            String address = gatt.getDevice().getAddress();

            if (status == BluetoothGatt.GATT_SUCCESS) {
                List<BluetoothGattService> bleGattServices = gatt.getServices();
                DeviceManager.getInstance().setServices(address, bleGattServices);

                // Show all the supported services and characteristics on the user interface.
                displayGattServices(address);

                // Stop Scan
                stopScan();

                sendUpdate("onServicesDiscovered", new Object[]{address, bleGattServices});
            }
            else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            Log.d(TAG, "onCharacteristicRead IN");

            // Retrieves address
            String address = gatt.getDevice().getAddress();

            if (status == BluetoothGatt.GATT_SUCCESS) {

                // Retrieves uuid and value
                String uuid = characteristic.getUuid().toString();
                byte[] value = characteristic.getValue();

                Log.d(TAG, "Received Characteristic: " + uuid + ", Data: " + value.toString());
                sendUpdate("onCharacteristicChanged", new Object[]{address, uuid, value});
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "onCharacteristicChanged");

            // Retrieves address
            String address = gatt.getDevice().getAddress();

            // Retrieves uuid and value
            String uuid = characteristic.getUuid().toString();
            byte[] value = characteristic.getValue();

            Log.d(TAG, "Received Characteristic: " + uuid + ", Data: " + value.toString());
            sendUpdate("onCharacteristicChanged", new Object[]{address, uuid, value});
        }
    };

    /**
     * Print GATT services and characteristics
     * @param address
     */
    public void displayGattServices(String address) {
        Log.d(TAG, "displayGattServices IN");

        List<BluetoothGattService> bleGattServices = DeviceManager.getInstance().getServices(deviceAddress);

        if (bleGattServices != null) {

            // Loops through available GATT Services.
            for (BluetoothGattService gattService : bleGattServices) {
                String serviceUuid = gattService.getUuid().toString();

                Log.d(TAG, "Service UUID: " + serviceUuid);

                List<BluetoothGattCharacteristic> gattCharacteristics =
                        gattService.getCharacteristics();

                // Loops through available Characteristics.
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    String CharacteristicUuid = gattCharacteristic.getUuid().toString();
                    Log.d(TAG, "Characteristic UUID: " + CharacteristicUuid);
                }

                Log.d(TAG, "=====================================");
            }
        }
    }

    /**
     * Read a characteristic value from the gatt server
     * @param bluetoothGattCharacteristic bluetooth Characteristic
     */
    public boolean readCharacteristics(BluetoothGattCharacteristic bluetoothGattCharacteristic){
        Log.d(TAG, "readCharacteristics IN");

        boolean result = false;

        if (bluetoothAdapter == null || bluetoothGatt == null) {
            //Check BluetoothGatt is available
            Log.w(TAG, "BluetoothAdapter not initialized");
            result = false;
        }
        else{
            // Read a value from the characteristic
            result = bluetoothGatt.readCharacteristic(bluetoothGattCharacteristic);
        }

        return result;
    }

    /**
     * Write a value to a given characteristic in the gatt server.
     * @param bluetoothGattCharacteristic Characteristic to act on.
     * @param value value to be written.
     */
    public boolean writeCharateristics(BluetoothGattCharacteristic bluetoothGattCharacteristic, byte[] value){
        Log.d(TAG, "writeCharateristics IN");

        boolean result = false;

        if (bluetoothAdapter == null || bluetoothGatt == null) {
            // Check BluetoothGatt is available
            Log.w(TAG, "BluetoothAdapter not initialized");
            result = false;
        }
        else{
            // Write a value in the characteristic
            bluetoothGattCharacteristic.setValue(value);
            result = bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic);
        }

        return result;
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public boolean setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        Log.d(TAG, "setCharacteristicNotification IN");

        boolean result = false;

        if (bluetoothAdapter == null || bluetoothGatt == null) {
            // Check BluetoothGatt is available
            Log.w(TAG, "BluetoothAdapter not initialized");
            result = false;
        }
        else {
            // Set notification
            result = bluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        }

        return result;
    }
}



/////////////////////////////////////
///////////// TRASH CAN /////////////
/////////////////////////////////////

//    /**
//     * Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
//     * fire an intent to display a dialog asking the user to grant permission to enable it.
//     */
//    public void enableBluetoothAndStartScan(){
//        Log.d(TAG, "enableBluetoothAndStartScan IN");
//
//        if (!bluetoothAdapter.isEnabled()) {
//            Log.d(TAG, "Bluetooth adapter is disabled. Enable bluetooth adapter.");
//            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            ((Activity) context).startActivityForResult(enableBtIntent, BleValues.REQUEST_ENABLE_BT);
//        }
//        else {
//            Log.d(TAG, "Bluetooth adapter is enabled!");
//            startScan();
//
//            if(isDevicePaired()){
//                final boolean result = connect(deviceAddress);
//                Log.d(TAG, "Connect request result: " + result);
//            }
//            else{
//                startScan();
//            }
//        }
//    }



//    private static IntentFilter makeGattUpdateIntentFilter() {
//        final IntentFilter intentFilter = new IntentFilter();
//        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
//        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
//        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
//        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
//        return intentFilter;
//    }



//    public void startGattServer() throws InterruptedException {
//        Log.d(TAG, "startGattServer IN");
//        BluetoothGattService service;
//
//        bluetoothGattServer = bluetoothManager.openGattServer(context, gattServerCallback);
//        Thread.sleep(10);
//        service = new BluetoothGattService(UUID.fromString(GATT_SERVER_SERVICE_UUID), BluetoothGattService.SERVICE_TYPE_PRIMARY);
//        bluetoothGattServer.addService(service);
//        Thread.sleep(100);
//        Log.d(TAG, "startGattServer OUT");
//    }
//    private final BluetoothGattServerCallback gattServerCallback = new BluetoothGattServerCallback() {
//        @Override
//        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
//            Log.d(TAG, "Our gatt server connection state changed, new state: " + Integer.toString(newState));
//            Log.d(TAG, "Device:" + device.getAddress());
//
//            super.onConnectionStateChange(device, status, newState);
//        }
//
//        @Override
//        public void onServiceAdded(int status, BluetoothGattService service) {
//            Log.d(TAG, "Our gatt server service was added.");
//            super.onServiceAdded(status, service);
//        }
//
//        @Override
//        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
//            Log.d(TAG, "Our gatt characteristic was read.");
//            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
//        }
//
//        @Override
//        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
//            Log.d(TAG, "We have received a write request for one of our hosted characteristics");
//
//            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
//        }
//
//        @Override
//        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
//            Log.d(TAG, "Our gatt server descriptor was read.");
//            super.onDescriptorReadRequest(device, requestId, offset, descriptor);
//        }
//
//        @Override
//        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
//            Log.d(TAG, "Our gatt server descriptor was written.");
//            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
//        }
//
//        @Override
//        public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
//            Log.d(TAG, "Our gatt server on execute write.");
//            super.onExecuteWrite(device, requestId, execute);
//        }
//    };



