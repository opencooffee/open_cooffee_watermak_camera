package com.opencooffeewatermarkcamera.sharedPreferences;

import android.content.Context;

import com.opencooffeewatermarkcamera.CommonValues;

public class SharedPreferencesManager {

	private static final String IS_CAMERA_FLASH_MODE = "is_camera_flash_mode";
    private static final String IS_CAMERA_RECTANGLE = "is_camera_rectangle";
	private static final String IS_CAMERA_FRONT_CAMERA = "is_camera_front_camera";
	private static final String DEVICE_SCREEN_WIDTH = "device_screen_width";
	private static final String DEVICE_SCREEN_HEIGHT = "device_screen_height";
	private static final String DEVICE_STATUS_BAR_HEIGHT = "device_status_bar_height";
	private static final String DEVICE_NAVIGATION_BAR_HEIGHT = "device_navigation_bar_height";
    private static final String DEVICE_MODEL = "device_model";

	private ObscuredSharedPreferences obscuredSharedPreferences;

	public SharedPreferencesManager(Context context) {
		obscuredSharedPreferences = ObscuredSharedPreferences.getPrefs(context, CommonValues.APP_NAME, Context.MODE_PRIVATE);
	}

    public void createIsCameraFlashMode(boolean isFlashMode) {
    	obscuredSharedPreferences.edit().putBoolean(IS_CAMERA_FLASH_MODE, isFlashMode).commit();
    }	

    public boolean getStoredIsCameraFlashMode() {
        return obscuredSharedPreferences.getBoolean(IS_CAMERA_FLASH_MODE, false);
    }

    public void createIsCameraRectangle(boolean isCameraRectangle) {
        obscuredSharedPreferences.edit().putBoolean(IS_CAMERA_RECTANGLE, isCameraRectangle).commit();
    }

    public boolean getStoredIsCameraRectangle() {
        return obscuredSharedPreferences.getBoolean(IS_CAMERA_RECTANGLE, false);
    }

    public void createIsCameraFrontCamera(boolean isCameraFrontCamera) {
    	obscuredSharedPreferences.edit().putBoolean(IS_CAMERA_FRONT_CAMERA, isCameraFrontCamera).commit();
    }	

    public boolean getStoredIsCameraFrontCamera() {
        return obscuredSharedPreferences.getBoolean(IS_CAMERA_FRONT_CAMERA, false);
    }
    
    public void createDeviceScreenWidth(int deviceScreenWidth) {
    	obscuredSharedPreferences.edit().putInt(DEVICE_SCREEN_WIDTH, deviceScreenWidth).commit();
    }
    
    public int getStoredDeviceScreenWidth() {
    	return obscuredSharedPreferences.getInt(DEVICE_SCREEN_WIDTH, 0);
    }
    
    public void createDeviceScreenHeight(int deviceScreenHeight) {
    	obscuredSharedPreferences.edit().putInt(DEVICE_SCREEN_HEIGHT, deviceScreenHeight).commit();
    }
    
    public int getStoredDeviceScreenHeight() {
    	return obscuredSharedPreferences.getInt(DEVICE_SCREEN_HEIGHT, 0);
    }

    public void createDeviceStatusBarHeight(int deviceStatusBarHeight) {
        obscuredSharedPreferences.edit().putInt(DEVICE_STATUS_BAR_HEIGHT, deviceStatusBarHeight).commit();
    }

    public int getStoredDeviceStatusBarHeight() {
        return obscuredSharedPreferences.getInt(DEVICE_STATUS_BAR_HEIGHT, 0);
    }

    public void createDeviceNavigationBarHeight(int deviceNavigationBarHeight) {
        obscuredSharedPreferences.edit().putInt(DEVICE_NAVIGATION_BAR_HEIGHT, deviceNavigationBarHeight).commit();
    }

    public int getStoredDeviceNavigationBarHeight() {
        return obscuredSharedPreferences.getInt(DEVICE_NAVIGATION_BAR_HEIGHT, 0);
    }

    public void createDeviceModel(String deviceModel) {
        obscuredSharedPreferences.edit().putString(DEVICE_MODEL, deviceModel).commit();
    }

    public String getStoredDeviceModel() {
        return obscuredSharedPreferences.getString(DEVICE_MODEL, "");
    }

}