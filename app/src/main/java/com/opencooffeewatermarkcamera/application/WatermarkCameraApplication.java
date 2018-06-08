package com.opencooffeewatermarkcamera.application;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Typeface;

import com.opencooffeewatermarkcamera.sharedPreferences.SharedPreferencesManager;
import com.opencooffeewatermarkcamera.utils.Utils;

public class WatermarkCameraApplication extends Application {

    //private static final String LOG_TAG = WatermarkCameraApplication.class.getSimpleName();

    private static Context staticContext;

    private static SharedPreferencesManager sharedPreferencesManager;

    private static Typeface regularFont, boldFont;

    private static boolean isCameraRectangle, isCameraFlashMode, isCameraFrontCamera;

    private static int deviceScreenWidth, deviceScreenHeight, deviceStatusBarHeight, deviceNavigationBarHeight;

    private static String deviceModel;

    /**
     * Called when application is created.
     * Override this method to initialize your application singleton, create and initialize any application state variables or shared resources.
     */
    @Override
    public void onCreate() {
        super.onCreate();

        Context context = getApplicationContext();

        WatermarkCameraApplication.staticContext = context;

        sharedPreferencesManager = new SharedPreferencesManager(context);

        isCameraRectangle = sharedPreferencesManager.getStoredIsCameraRectangle();
        isCameraFlashMode = sharedPreferencesManager.getStoredIsCameraFlashMode();
        isCameraFrontCamera = sharedPreferencesManager.getStoredIsCameraFrontCamera();
        deviceScreenWidth = sharedPreferencesManager.getStoredDeviceScreenWidth();
        deviceScreenHeight = sharedPreferencesManager.getStoredDeviceScreenHeight();
        deviceStatusBarHeight = sharedPreferencesManager.getStoredDeviceStatusBarHeight();
        deviceNavigationBarHeight = sharedPreferencesManager.getStoredDeviceNavigationBarHeight();
        deviceModel = sharedPreferencesManager.getStoredDeviceModel();
    }

    /**
     * Generally only be called when background processes have already been terminated and the current foreground applications are still low on memory.
     * Override this handler to clear caches or release unnecessary resources.
     */
    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    /**
     * Application specific alternative to the onLowMemory handler.
     * Called when the run time determines that the current application should attempt to trim its memory overhead - typically when it moves to the background.
     * Includes a level parameter that provides the context around the request.
     *
     * @param level
     */
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
    }

    /**
     * If the application uses values dependent on specific configurations, override this handler to reload those values and otherwise handle configuration changes at an application level.
     *
     * @param newConfiguration
     */
    @Override
    public void onConfigurationChanged(Configuration newConfiguration) {
        super.onConfigurationChanged(newConfiguration);
    }

    public static boolean getIsCameraRectangle() {
        return isCameraRectangle;
    }

    public static void setIsCameraRectangle(boolean isCameraRectangle) {
        WatermarkCameraApplication.isCameraRectangle = isCameraRectangle;
        sharedPreferencesManager.createIsCameraRectangle(isCameraRectangle);
    }

    public static boolean getIsCameraFlashMode() {
        return isCameraFlashMode;
    }

    public static void setIsCameraFlashMode(boolean isCameraFlashMode) {
        WatermarkCameraApplication.isCameraFlashMode = isCameraFlashMode;
        sharedPreferencesManager.createIsCameraFlashMode(isCameraFlashMode);
    }

    public static boolean getIsCameraFrontCamera() {
    	return isCameraFrontCamera;
    }

    public static void setIsCameraFrontCamera(boolean isCameraFrontCamera) {
		WatermarkCameraApplication.isCameraFrontCamera = isCameraFrontCamera;
		sharedPreferencesManager.createIsCameraFrontCamera(isCameraFrontCamera);
    }

    public static int getDeviceScreenWidth() {

        if (deviceScreenWidth == 0) {

            deviceScreenWidth = Utils.getDeviceScreeWidth(staticContext);

            setDeviceScreenWidth(deviceScreenWidth);
        }

        return deviceScreenWidth;
    }

    public static void setDeviceScreenWidth(int deviceScreenWidth) {
        WatermarkCameraApplication.deviceScreenWidth = deviceScreenWidth;
        sharedPreferencesManager.createDeviceScreenWidth(deviceScreenWidth);
    }

    public static int getDeviceScreenHeight() {

        if (deviceScreenHeight == 0) {

            deviceScreenHeight = Utils.getDeviceScreenHeight(staticContext);

            setDeviceScreenHeight(deviceScreenHeight);
        }

        return deviceScreenHeight;
    }

    public static void setDeviceScreenHeight(int deviceScreenHeight) {
        WatermarkCameraApplication.deviceScreenHeight = deviceScreenHeight;
        sharedPreferencesManager.createDeviceScreenHeight(deviceScreenHeight);
    }

    public static int getDeviceStatusBarHeight() {

        if (deviceStatusBarHeight == 0) {

            deviceStatusBarHeight = Utils.getDeviceStatusBarHeight(staticContext);

            setDeviceStatusBarHeight(deviceStatusBarHeight);
        }

        return deviceStatusBarHeight;
    }

    public static void setDeviceStatusBarHeight(int deviceStatusBarHeight) {
        WatermarkCameraApplication.deviceStatusBarHeight = deviceStatusBarHeight;
        sharedPreferencesManager.createDeviceStatusBarHeight(deviceStatusBarHeight);
    }

    public static int getDeviceNavigationBarHeight() {

        if (deviceNavigationBarHeight == 0) {

            deviceNavigationBarHeight = Utils.getDeviceNavigationBarHeight(staticContext);

            setDeviceNavigationBarHeight(deviceNavigationBarHeight);
        }

        return deviceNavigationBarHeight;
    }

    public static void setDeviceNavigationBarHeight(int deviceNavigationBarHeight) {
        WatermarkCameraApplication.deviceNavigationBarHeight = deviceNavigationBarHeight;
        sharedPreferencesManager.createDeviceNavigationBarHeight(deviceNavigationBarHeight);
    }

    public static String getDeviceModel() {

        if (deviceModel.equals("")) {

            deviceModel = Utils.getDeviceModel();

            if (!deviceModel.equals("")) {
                setDeviceModel(deviceModel);
            }

        }

        return deviceModel;
    }

    public static void setDeviceModel(String deviceModel) {
        WatermarkCameraApplication.deviceModel = deviceModel;
        sharedPreferencesManager.createDeviceModel(deviceModel);
    }

    public static Typeface getRegularFont() {

        if (regularFont == null) {
            regularFont = Typeface.createFromAsset(staticContext.getAssets(), "fonts/montserrat_regular.ttf");
        }

        return regularFont;
    }

    public static Typeface getBoldFont() {

        if (boldFont == null) {
            boldFont = Typeface.createFromAsset(staticContext.getAssets(), "fonts/montserrat_bold.ttf");
        }

        return boldFont;
    }

}