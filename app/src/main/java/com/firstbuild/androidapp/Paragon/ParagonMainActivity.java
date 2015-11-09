package com.firstbuild.androidapp.paragon;

import android.app.Fragment;
import android.app.FragmentManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.firstbuild.androidapp.paragon.dataModel.RecipeManager;
import com.firstbuild.androidapp.paragon.navigation.NavigationDrawerFragment;
import com.firstbuild.androidapp.paragon.settings.SettingsActivity;
import com.firstbuild.androidapp.ParagonValues;
import com.firstbuild.androidapp.R;
import com.firstbuild.commonframework.bleManager.BleListener;
import com.firstbuild.commonframework.bleManager.BleManager;
import com.firstbuild.commonframework.bleManager.BleValues;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class ParagonMainActivity extends ActionBarActivity {
    private String TAG = ParagonMainActivity.class.getSimpleName();

    static final int REQUEST_TAKE_PHOTO = 1;

    // Bluetooth adapter handler
    private BluetoothAdapter bluetoothAdapter = null;
    private ParagonSteps currentStep = ParagonSteps.STEP_NONE;
//    private float targetTemp;
    private float currentTemp;
//    private int targetTime = ParagonValues.MAX_COOK_TIME;
    private byte batteryLevel;

    Toolbar toolbar;

    private Queue requestQueue = new LinkedList();
    private String REQUEST_METHOD_READ = "READ";
    private String REQUEST_METHOD_NOTIFICATION = "NOTIFICATION";
    private boolean isCheckingCurrentStatus = false;
    private BleListener bleListener = new BleListener() {
        @Override
        public void onScanDevices(HashMap<String, BluetoothDevice> bluetoothDevices) {
            super.onScanDevices(bluetoothDevices);

            Log.d(TAG, "onScanDevices IN");

            Log.d(TAG, "bluetoothDevices size: " + bluetoothDevices.size());
            for (Map.Entry<String, BluetoothDevice> entry : bluetoothDevices.entrySet()) {

                // Retrieves address and name
                BluetoothDevice device = entry.getValue();
                String address = device.getAddress();
                String name = device.getName();

                Log.d(TAG, "------------------------------------");
                Log.d(TAG, "Device address: " + address);
                Log.d(TAG, "Device name: " + name);

                if (ParagonValues.TARGET_DEVICE_NAME.equals(name)) {
                    Log.d(TAG, "device found: " + device.getName());

                    // Connect to device
                    BleManager.getInstance().connect(address);

                    // Stop ble device scanning
                    BleManager.getInstance().stopScan();

//                    nextStep(ParagonSteps.STEP_COOKING_MODE);
                    nextStep(ParagonSteps.STEP_CHECK_CURRENT_STATUS);

                    break;
                }
            }
            Log.d(TAG, "====================================");
        }

        @Override
        public void onScanStateChanged(int status) {
            super.onScanStateChanged(status);

            Log.d(TAG, "[onScanStateChanged] status: " + status);

            if (status == BleValues.START_SCAN) {
                Log.d(TAG, "Scanning BLE devices");
            }
            else {
                Log.d(TAG, "Stop scanning BLE devices");
            }
        }

        @Override
        public void onConnectionStateChanged(final String address, final int status) {
            super.onConnectionStateChanged(address, status);

            Log.d(TAG, "[onConnectionStateChanged] address: " + address + ", status: " + status);
        }

        @Override
        public void onServicesDiscovered(String address, List<BluetoothGattService> bleGattServices) {
            super.onServicesDiscovered(address, bleGattServices);

            Log.d(TAG, "[onServicesDiscovered] address: " + address);

            BleManager.getInstance().displayGattServices(address);

            // Get Initial values.
            BleManager.getInstance().readCharacteristics(ParagonValues.CHARACTERISTIC_COOK_CONFIGURATION);
            BleManager.getInstance().readCharacteristics(ParagonValues.CHARACTERISTIC_BATTERY_LEVEL);
            BleManager.getInstance().readCharacteristics(ParagonValues.CHARACTERISTIC_BURNER_STATUS);
            BleManager.getInstance().readCharacteristics(ParagonValues.CHARACTERISTIC_REMAINING_TIME);

            BleManager.getInstance().setCharacteristicNotification(ParagonValues.CHARACTERISTIC_COOK_CONFIGURATION, true);
            BleManager.getInstance().setCharacteristicNotification(ParagonValues.CHARACTERISTIC_BATTERY_LEVEL, true);
            BleManager.getInstance().setCharacteristicNotification(ParagonValues.CHARACTERISTIC_CURRENT_TEMPERATURE, true);
            BleManager.getInstance().setCharacteristicNotification(ParagonValues.CHARACTERISTIC_REMAINING_TIME, true);
            BleManager.getInstance().setCharacteristicNotification(ParagonValues.CHARACTERISTIC_BURNER_STATUS, true);
            BleManager.getInstance().setCharacteristicNotification(ParagonValues.CHARACTERISTIC_CURRENT_COOK_STATE, true);

        }

        @Override
        public void onCharacteristicRead(String address, String uuid, byte[] value) {
            super.onCharacteristicRead(address, uuid, value);

            Log.d(TAG, "[onCharacteristicRead] address: " + address + ", uuid: " + uuid);

            onReceivedData(uuid, value);

//            nextCharacteristicRead();
        }

        @Override
        public void onCharacteristicWrite(String address, String uuid, byte[] value) {
            super.onCharacteristicWrite(address, uuid, value);

            Log.d(TAG, "[onCharacteristicWrite] address: " + address + ", uuid: " + uuid);
        }

        @Override
        public void onCharacteristicChanged(String address, String uuid, byte[] value) {
            super.onCharacteristicChanged(address, uuid, value);

            Log.d(TAG, "[onCharacteristicChanged] address: " + address + ", uuid: " + uuid);

            onReceivedData(uuid, value);

            nextCharacteristicRead();
        }

        @Override
        public void onDescriptorWrite(String address, String uuid, byte[] value) {
            super.onDescriptorWrite(address, uuid, value);

            Log.d(TAG, "[onDescriptorWrite] address: " + address + ", uuid: " + uuid);
        }
    };
    private int MAX_BURNER = 5;

    // Navigation drawer.
    private NavigationDrawerFragment drawerFragment;
    private TextView toolbarText;
    private ImageView toolbarImage;
    private String currentPhotoPath;

    private void nextCharacteristicRead() {

        String nextRequest = (String) requestQueue.poll();

        if (nextRequest != null) {
            String method = nextRequest.split("/")[0];
            String characteristic = nextRequest.split("/")[1];

            if (method.equals(REQUEST_METHOD_READ)) {
                BleManager.getInstance().readCharacteristics(characteristic);
            }
            else if (method.equals(REQUEST_METHOD_NOTIFICATION)) {
                BleManager.getInstance().setCharacteristicNotification(characteristic, true);
            }
            else {

            }

        }
    }

    public int getTargetTime() {
        return RecipeManager.getInstance().getCurrentStage().getTime();

//        return targetTime;
    }

    public void setTargetTime(int targetTime, int setTargetTimeMax) {
        RecipeManager.getInstance().getCurrentStage().setTime(targetTime);
        RecipeManager.getInstance().getCurrentStage().setMaxTime(targetTime);
//        this.targetTime = targetTime;
    }

    public float getTargetTemp() {
        return RecipeManager.getInstance().getCurrentStage().getTemp();
//        return targetTemp;
    }

    public void setTargetTemp(float targetTemp) {
        RecipeManager.getInstance().getCurrentStage().setTemp((int)targetTemp);
//        this.targetTemp = targetTemp;
    }

    public float getCurrentTemp() {
        return currentTemp;
    }

    private void onReceivedData(String uuid, byte[] value) {

        Log.d(TAG, "onReceivedData :" + uuid);

        ByteBuffer byteBuffer = ByteBuffer.wrap(value);


        switch (uuid.toUpperCase()) {
            case ParagonValues.CHARACTERISTIC_CURRENT_TEMPERATURE:
                currentTemp = (byteBuffer.getShort() / 100.0f);
                Log.d(TAG, "CHARACTERISTIC_CURRENT_TEMPERATURE :" + currentTemp);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Fragment fragment = getFragmentManager().findFragmentById(R.id.frame_content);

                                if (fragment instanceof SousvideStatusFragment) {
                                    ((SousvideStatusFragment) fragment).updateUiCurrentTemp();

//                                    if(currentTemp >= targetTemp){
//                                        nextStep(ParagonSteps.STEP_SOUSVIDE_READY_COOK);
//                                    }
//                                    else{
//                                        //do nothing
//                                    }
                                }
                                else {
                                    //do nothing
                                }

                            }
                        });
                    }
                }).start();
                break;

//            case ParagonValues.CHARACTERISTIC_TARGET_TEMPERATURE:
//                targetTemp = (float) byteBuffer.getShort() / 100.0f;
//
//                Log.d(TAG, "CHARACTERISTIC_TARGET_TEMPERATURE :" + targetTemp);
//                break;

            case ParagonValues.CHARACTERISTIC_BATTERY_LEVEL:
                batteryLevel = byteBuffer.get();

                Log.d(TAG, "CHARACTERISTIC_BATTERY_LEVEL :" + batteryLevel);


                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
//                                ((TextView) toolbar.findViewById(R.id.text_battery_level)).setText(batteryLevel + "%");
                            }
                        });
                    }
                }).start();
                break;

            case ParagonValues.CHARACTERISTIC_REMAINING_TIME:
                Log.d(TAG, "CHARACTERISTIC_REMAINING_TIME :" + byteBuffer.getShort());
                String buffer = String.format("%02x%02x", value[0], value[1]);
                int remainingTime = Integer.parseInt(buffer, 16);
                onElapsedTime(remainingTime);

                break;


//            case ParagonValues.CHARACTERISTIC_COOK_TIME:
//                Log.d(TAG, "CHARACTERISTIC_COOK_TIME :" + byteBuffer.getShort());
//                break;

            case ParagonValues.CHARACTERISTIC_COOK_CONFIGURATION:
                Log.d(TAG, "CHARACTERISTIC_COOK_CONFIGURATION :");

                for(int i = 0; i < 32; i++){
                    Log.d(TAG, "CHARACTERISTIC_COOK_CONFIGURATION :" + String.format("%02x", value[i]));
                }
                break;


            case ParagonValues.CHARACTERISTIC_BURNER_STATUS:
                Log.d(TAG, "CHARACTERISTIC_BURNER_STATUS :" + String.format("%02x", value[0]));

//                Log.d(TAG, "CHARACTERISTIC_BURNER_STATUS :" +
//                        String.format("%02x", value[0]) + ", " +
//                        String.format("%02x", value[1]) + ", " +
//                        String.format("%02x", value[2]) + ", " +
//                        String.format("%02x", value[3]) + ", " +
//                        String.format("%02x", value[4]));
//
//                boolean isSousVide = false;
//                boolean isPreheat = false;
//
//                //There are 5 burner statuses and and 5 bytes. Each byte is a status
//                //the statuses are:
//                //
//                //Bit 7: 0 - Off, 1 - On
//                //Bit 6: Normal / Sous Vide
//                //Bit 5: 0 - Cook, 1 - Preheat
//                //Bits 4-0: Burner PwrLevel
//
//                for (int i = 0; i < MAX_BURNER; i++) {
//                    if (getBit(value[i], 6)) {
//                        isSousVide = true;
//                        isPreheat = getBit(value[i], 5);
//
//                        break;
//                    }
//                }
//
//                onBurnerStatus(isSousVide, isPreheat);
                onBurnerStatus(false, false);

                break;


            case ParagonValues.CHARACTERISTIC_CURRENT_COOK_STATE:
                Log.d(TAG, "CHARACTERISTIC_CURRENT_COOK_STATE :" + String.format("%02x", value[0]));
                onCookState(value[0]);

                break;
        }
    }

    private void onCookState(final byte state) {
        Fragment fragment = getFragmentManager().findFragmentById(R.id.frame_content);

        if (fragment instanceof GetReadyFragment) {
            nextStep(ParagonSteps.STEP_SOUSVIDE_CIRCLE);
        }
        else if (fragment instanceof SousvideStatusFragment) {

            new Thread(new Runnable() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Fragment fragment = getFragmentManager().findFragmentById(R.id.frame_content);

                            ((SousvideStatusFragment) fragment).updateCookStatus(state);

                        }
                    });
                }
            }).start();
        }
        else{
            // nothing.
        }

    }

    private void onElapsedTime(int elapsedTime) {
        final int elapsedTimeValue = elapsedTime;
        Fragment fragment = getFragmentManager().findFragmentById(R.id.frame_content);

        if (fragment instanceof SousvideStatusFragment) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Fragment fragment = getFragmentManager().findFragmentById(R.id.frame_content);

                            ((SousvideStatusFragment) fragment).updateUiElapsedTime(elapsedTimeValue);

                        }
                    });
                }
            }).start();

        }
        else {

        }

    }

    private void onBurnerStatus(boolean isSousVide, boolean isPreheat) {
        final boolean isPreheatValue = isPreheat;

        Log.d(TAG, "onBurnerStatus : " + "isSousvide " + isSousVide + ", isPreheat " + isPreheatValue);

        // Checking current step, if very first step..
        if (currentStep == ParagonSteps.STEP_CHECK_CURRENT_STATUS) {

            // And Cook Top already start sousvide mode.
            if (isSousVide) {
                // Then go to the Status Screen.
                nextStep(ParagonSteps.STEP_SOUSVIDE_CIRCLE);

//                new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                Fragment fragment = getFragmentManager().findFragmentById(R.id.frame_content);
//
//                                ((SousvideStatusFragment) fragment).updateCookStatus(isPreheatValue);
//
//                            }
//                        });
//                    }
//                }).start();

            }
            else {
                // Or go to the Cooking Method Screen.
                nextStep(ParagonSteps.STEP_COOKING_MODE);
            }
        }
        else {

            if (isSousVide) {
//
//                Fragment fragment = getFragmentManager().findFragmentById(R.id.frame_content);
//
//                if (fragment instanceof GetReadyFragment) {
//
//                    nextStep(ParagonSteps.STEP_SOUSVIDE_CIRCLE);
//
//                }
//                else if (fragment instanceof SousvideStatusFragment) {
//
//                    new Thread(new Runnable() {
//                        @Override
//                        public void run() {
//                            runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    Fragment fragment = getFragmentManager().findFragmentById(R.id.frame_content);
//
//                                    ((SousvideStatusFragment) fragment).updateCookStatus(isPreheatValue);
//
//                                }
//                            });
//                        }
//                    }).start();
//
//                }
//                else {
//                    // do nothing
//                }
            }
            else {
                //do nothging.
            }
        }

    }

    boolean getBit(int value, int bit) {
        return (value & (1 << bit)) != 0;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paragon_main);

        toolbar = (Toolbar) findViewById(R.id.app_bar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);

//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//        getSupportActionBar().setHomeButtonEnabled(true);
        toolbarText = (TextView) toolbar.findViewById(R.id.toolbar_title_text);
        toolbarImage = (ImageView) toolbar.findViewById(R.id.toolbar_title_image);

//        // Setup drawer navigation.
//        drawerFragment = (NavigationDrawerFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_navigation_drawer);
//        drawerFragment.setUp(R.id.fragment_navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout), toolbar);

        // Use this check to determine whether BLE is supported on the device. Then
        // you can selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.d(TAG, "BLE is not supported - Stop activity!");

            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }
        else {
            // Initializes Bluetooth adapter.
            final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            bluetoothAdapter = bluetoothManager.getAdapter();

            // Checks if Bluetooth is supported on the device.
            if (bluetoothAdapter == null) {
                Log.d(TAG, "Bluetooth is not supported - Stop activity!");

                Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
                finish();
            }
            else {
                // Do nothing
            }

        }

        // Initialize ble manager
        BleManager.getInstance().initBleManager(this);

        // Add ble event listener
        BleManager.getInstance().addListener(bleListener);


        isCheckingCurrentStatus = false;


        //TODO: remove this code after test.
//        nextStep(ParagonSteps.STEP_COOKING_MODE);


    }

    @Override
    public void onBackPressed() {

        FragmentManager fm = getFragmentManager();

        if (fm.getBackStackEntryCount() > 1) {
            fm.popBackStack();
        }
        else {
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_paragon_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {

            Intent intent = new Intent(this, SettingsActivity.class);

            intent.putExtra("SelectedMenu", "MenuSettings");
            startActivity(intent);
            return true;
        }
        else if (id == R.id.action_about) {
            Intent intent = new Intent(this, SettingsActivity.class);

            intent.putExtra("SelectedMenu", "MenuAbout");
            startActivity(intent);
            return true;
        }
        else if (id == R.id.action_my_product) {
            finish();
            return true;
        }
        else if (id == R.id.action_help) {
            String url = getResources().getString(R.string.url_manual);
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            return true;
        }
        else if (id == R.id.action_feedback) {
            final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);

            emailIntent.setType("plain/text");
            emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"master@firstbuild.com"});
            emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Feedback Paragon] ");
            emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, "Text");

            startActivity(Intent.createChooser(emailIntent, "Send mail..."));
        }
        else if (id == android.R.id.home) {
            onBackPressed();

            return true;
        }
        else{
            // do nothing.
        }




        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Check bluetooth adapter. If the adapter is disabled, enable it
        boolean result = BleManager.getInstance().isBluetoothEnabled();

        if (!result) {
            Log.d(TAG, "Bluetooth adapter is disabled. Enable bluetooth adapter.");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, BleValues.REQUEST_ENABLE_BT);
        }
        else {
            Log.d(TAG, "Bluetooth adapter is already enabled. Start scanning.");
            BleManager.getInstance().startScan();
        }

    }

    /**
     * @param step
     */
    public void nextStep(ParagonSteps step) {
        Fragment fragment = null;

        currentStep = step;

        switch (currentStep) {
            case STEP_CHECK_CURRENT_STATUS:
                isCheckingCurrentStatus = true;
                return;

            case STEP_COOKING_MODE:
                fragment = new SelectModeFragment();
                break;

            case STEP_SOUSVIDE_SETTINGS:
                fragment = new SettingsFragment();
                break;

            case STEP_SOUSVIDE_GETREADY:
                fragment = new GetReadyFragment();
                break;

            case STEP_SOUSVIDE_CIRCLE:
                fragment = new SousvideStatusFragment();
                break;

            case STEP_SOUSVIDE_COMPLETE:
                fragment = new CompleteFragment();
                break;

            case STEP_QUICK_START:
                fragment = new QuickStartFragment();
                break;

            case STEP_MY_RECIPES:
                fragment = new MyRecipesFragment();
                break;

            case STEP_EDIT_RECIPES:
                fragment = new RecipeEditFragment();
                setTitle("Edit");
                break;

            case STEP_EDIT_STAGE:
                fragment = new StageEditFragment();
                int index = RecipeManager.getInstance().getCurrentStageIndex();

                if(index == RecipeManager.INVALID_INDEX){
                    setTitle("New Stage");
                }
                else{
                    setTitle("Stage " + (index + 1));
                }
                break;

            case STEP_VIEW_RECIPE:
                fragment = new RecipeViewFragment();
                break;

            case STEP_VIEW_STAGE:
                fragment = new StageViewFragment();
                break;

            case STEP_ADD_RECIPE_MUTISTAGE:
                fragment = new RecipeEditFragment();
                setTitle("Multi-Stage");
                break;

            case STEP_ADD_RECIPE_SOUSVIDE:
                fragment = new SousVideEditFragment();
                setTitle("Sous vide");
                break;

            case STEP_ADD_SOUSVIDE_SETTING:
                fragment = new QuickStartFragment();
                setTitle("Sous Vide Settings");
                break;


            default:
                break;

        }

        if (fragment != null) {
            getFragmentManager().
                    beginTransaction().
                    replace(R.id.frame_content, fragment).
                    addToBackStack(null).
                    commit();
        }
        else {

        }


    }

    public enum ParagonSteps {
        STEP_NONE,
        STEP_CHECK_CURRENT_STATUS,
        STEP_COOKING_MODE,
        STEP_SOUSVIDE_SETTINGS,
        STEP_SOUSVIDE_GETREADY,
        STEP_SOUSVIDE_CIRCLE,
        STEP_SOUSVIDE_COMPLETE,
        STEP_QUICK_START,
        STEP_MY_RECIPES,
        STEP_EDIT_RECIPES,
        STEP_EDIT_STAGE,
        STEP_VIEW_RECIPE,
        STEP_VIEW_STAGE,
        STEP_ADD_RECIPE_MUTISTAGE,
        STEP_ADD_RECIPE_SOUSVIDE,
        STEP_ADD_SOUSVIDE_SETTING
    }


    /**
     * Set title.
     * @param title String to be title
     */
    protected void setTitle(String title){
        if(title.equals("Paragon")){
            toolbarText.setVisibility(View.GONE);
            toolbarImage.setVisibility(View.VISIBLE);
        }
        else{
            toolbarText.setVisibility(View.VISIBLE);
            toolbarImage.setVisibility(View.GONE);

            toolbarText.setText(title);
        }

    }

    /**
     * Create image file for recipe.
     * @return String dir + file name of image file.
     * @throws IOException
     */
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    /**
     * Take picture and store in file.
     */
    protected void dispatchTakePictureIntent() {
        Log.d(TAG, "dispatchTakePictureIntent IN");

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.d(TAG, "dispatchTakePictureIntent error : "+ex);
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            Bitmap imageBitmap = BitmapFactory.decodeFile(currentPhotoPath, options);
            Fragment fragment = getFragmentManager().findFragmentById(R.id.frame_content);

            if (fragment instanceof RecipeEditFragment) {
                ((RecipeEditFragment) fragment).setRecipeImage(imageBitmap, currentPhotoPath);
            }
        }
    }


    /**
     * Get recipe's title image from file.
     *
     * @param imageFileName File name of external storage.
     */
    public void loadImageFromFile(String imageFileName) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        Bitmap imageBitmap;
        try {
            imageBitmap = BitmapFactory.decodeFile(imageFileName, options);

        }
        catch (Exception e) {
            imageBitmap = null;
        }

        Fragment fragment = getFragmentManager().findFragmentById(R.id.frame_content);

        if (fragment instanceof RecipeViewFragment) {
            ((RecipeViewFragment) fragment).setRecipeImage(imageBitmap);
        }
    }


}
