package com.firstbuild.androidapp;

import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by hans on 16. 5. 31..
 */
public class OpalValues {

    // BLE Service UUID
    public static final String OPAL_BLE_SERVICE_UUID = "3E6763C5-9429-40CC-909E-BEBF8C7487BE";
    public static final String OPAL_OP_STATE_UUID = "097A2751-CA0D-432F-87B5-7D2F31E45551";
    public static final String OPAL_OP_MODE_UUID = "79994230-4B04-40CD-85C9-02DD1A8D4DD0";
    public static final String OPAL_LIGHT_UUID = "37988F00-EA39-4A2D-9983-AFAD6535C02E";
    public static final String OPAL_CLEAN_CYCLE_UUID = "EFE4BD77-0600-47D7-B3F6-DC81AF0D9AAF";
    public static final String OPAL_TIME_SYNC_UUID = "ED9E0784-FBF1-47F4-AFE2-D439A6C207FC";
    public static final String OPAL_SET_SCHEDULE_UUID = "9E1AE873-CB5E-4485-9884-5C5A3AD60E47";
    public static final String OPAL_ENABLE_DISABLE_SCHEDULE_UUID = "B45163B3-1092-4725-95DC-1A43AC4A9B88";
    public static final String OPAL_IMG_TYPE_UUID = "5EA370C7-2059-41DB-9999-36527B43A4B4";
    public static final String OPAL_VERSION_CHAR_UUID = "CF88A5B6-6687-4F14-8E21-BB9E78A40ECC";
    public static final String OPAL_UPDATE_PROGRESS_UUID = "14FF6DFB-36FA-4456-927D-759E1A9A8446";
    public static final String OPAL_ERROR_CHAR_UUID = "5BCBF6B1-DE80-94B6-0F4B-99FB984707B6";
    public static final String OPAL_TEMPERATURE_CHAR_UUID = "BD205030-B5CE-4847-B78D-83BFF1450A6B";
    public static final String OPAL_FILTER_INSTALL_CHAR_UUID = "9BFDF2DD-E92C-468C-996B-F2EB95AA9325";
    public static final String OPAL_PUMP_CYCLE_CHAR_UUID = "6AF78DF8-629C-490E-9D42-100E84915753";


    // OTA Upgrade Service UUID
    public static final String OPAL_OTA_UPGRADE_SERVICE_UUID = "E936877A-8DD0-FAA7-B648-F46ACDA1F27B";
    public static final String OPAL_CONTROL_COMMAND_CHAR_UUID = "4FB34AB1-6207-E5A0-484F-E24A7F638FFF";
    public static final String OPAL_IMAGE_DATA_CHAR_UUID = "78282AE5-3060-C3B6-7D49-EC74702414E5";
    public static final String OPAL_OTA_APP_INFO_CHAR_UUID = "318DB1F5-67F1-119B-6A41-1EECA0C744CE";

    // OP State Char , Permission > Read only , Property > Read/Notify
    public static final byte OPAL_STATE_IDLE = 0x00;
    public static final byte OPAL_STATE_ICE_MAKING = 0x01;
    public static final byte OPAL_STATE_ADD_WATER = 0x02;
    public static final byte OPAL_STATE_ICE_FULL = 0x03;
    public static final byte OPAL_STATE_CLEANING = 0x04;

    // OP Mode Char , Permission > Read/Write , Property > Read/Write/Notify
    // Clean mode cannot set by app
    public static final byte OPAL_MODE_OFF = 0x00;
    public static final byte OPAL_MODE_ICE_MAKING = 0x01;
    public static final byte OPAL_MODE_CLEAN = 0x02;

    // Light Char , Permission > Read/Write , Property > Read/Write/Notify
    // Available during Opal is on
    public static final byte OPAL_DAY_TIME_LIGHT = 0x00;
    public static final byte OPAL_NIGHT_TIME_LIGHT = 0x01;

    //Clean cycle Char, Permission > Read only , Property > Read/Notify
    public static final byte OPAL_CYCLE_ONE = 0x00;
    public static final byte OPAL_CYCLE_TWO = 0x01;
    public static final byte OPAL_CYCLE_THREE = 0x02;
    public static final byte OPAL_CYCLE_FOUR = 0x03;
    public static final byte OPAL_CYCLE_FIVE = 0x04;

    // Enable/Disable schedule , Permission > Read/Write , Property > Read/Write
    public static final byte OPAL_DISABLE_SCHEDULE = 0x00;
    public static final byte OPAL_ENABLE_SCHEDULE = 0x01;

    // Image Type char , Permission > Read/Write , Property > Read/Write
    public static final byte OPAL_BLE_IMAGE_TYPE = 0x00;
    public static final byte OPAL_OPAL_IMAGE_TYPE = 0x01;


    // BLE return code
    public static final String OPAL_BLE_RET_SUCCESS = "0x00";
    public static final String OPAL_BLE_RET_OUT_OF_RANGE = "0x80";
    public static final String OPAL_BLE_RET_WRITE_NOT_ALLOWED = "0x90";
    public static final String OPAL_BLE_RET_ILLEGAL_OTA_STATE_TRANSITION = "0xE0";

    // Target device Opal's name
    public static final String TARGET_DEVICE_NAME = "OPAL Bluetooth";

    public static void convertEndian() {

        ArrayList<String> uuidList = new ArrayList<>();
        uuidList.add(OPAL_BLE_SERVICE_UUID);
        uuidList.add(OPAL_OP_STATE_UUID);
        uuidList.add(OPAL_OP_MODE_UUID);
        uuidList.add(OPAL_LIGHT_UUID);
        uuidList.add(OPAL_CLEAN_CYCLE_UUID);
        uuidList.add(OPAL_TIME_SYNC_UUID);
        uuidList.add(OPAL_SET_SCHEDULE_UUID);
        uuidList.add(OPAL_ENABLE_DISABLE_SCHEDULE_UUID);
        uuidList.add(OPAL_IMG_TYPE_UUID);
        uuidList.add(OPAL_VERSION_CHAR_UUID);
        uuidList.add(OPAL_UPDATE_PROGRESS_UUID);
        uuidList.add(OPAL_ERROR_CHAR_UUID);
        uuidList.add(OPAL_TEMPERATURE_CHAR_UUID);
        uuidList.add(OPAL_FILTER_INSTALL_CHAR_UUID);
        uuidList.add(OPAL_PUMP_CYCLE_CHAR_UUID);
        uuidList.add(OPAL_OTA_UPGRADE_SERVICE_UUID);
        uuidList.add(OPAL_CONTROL_COMMAND_CHAR_UUID);
        uuidList.add(OPAL_IMAGE_DATA_CHAR_UUID);
        uuidList.add(OPAL_OTA_APP_INFO_CHAR_UUID);

        StringBuilder builder = new StringBuilder();
        int start;
        int end;
        int delimiterLocation = 0;

        for(String s : uuidList) {

            s = s.replaceAll("-", "");


            for(int i = s.length() -1 ; i >= 0 ; i = i - 2) {
                start = i-1;
                end = i+1;
                if(start < 0) {
                    start = 0;
                }
                builder.append(s.substring(start, end));
                delimiterLocation++;
                if(delimiterLocation == 4 || delimiterLocation == 6
                        || delimiterLocation == 8 || delimiterLocation == 10 ) {
                    builder.append("-");

                }

            }

            Log.d("HANS", "reversed String  : " + builder.toString());
            builder.setLength(0);
            delimiterLocation = 0;

        }







    }



}
