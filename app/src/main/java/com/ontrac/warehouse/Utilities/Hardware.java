package com.ontrac.warehouse.Utilities;


import android.content.Context;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.os.Build;

public class Hardware {

    public static boolean isEmulator() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || Build.PRODUCT.contains("google_sdk")
                || Build.PRODUCT.contains("sdk")
                ;
    }

    public void getDisplay(Context context){
        /*
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        //WindowManager windowManager = (WindowManager)this.getSystemService(Context.WINDOW_SERVICE);

        Display display = windowManager.getDefaultDisplay();
        String displayName = display.getName();  // minSdkVersion=17+
        Log.i(TAG, "displayName  = " + displayName);

        // display size in pixels
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        Log.i(TAG, "width        = " + width);
        Log.i(TAG, "height       = " + height);

        // pixels, dpi
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        int heightPixels = metrics.heightPixels;
        int widthPixels = metrics.widthPixels;
        int densityDpi = metrics.densityDpi;
        float xdpi = metrics.xdpi;
        float ydpi = metrics.ydpi;
        Log.i(TAG, "widthPixels  = " + widthPixels);
        Log.i(TAG, "heightPixels = " + heightPixels);
        Log.i(TAG, "densityDpi   = " + densityDpi);
        Log.i(TAG, "xdpi         = " + xdpi);
        Log.i(TAG, "ydpi         = " + ydpi);

        // deprecated
        int screenHeight = display.getHeight();
        int screenWidth = display.getWidth();
        Log.i(TAG, "screenHeight = " + screenHeight);
        Log.i(TAG, "screenWidth  = " + screenWidth);

        // orientation (either ORIENTATION_LANDSCAPE, ORIENTATION_PORTRAIT)
        int orientation = context.getResources().getConfiguration().orientation;
        Log.i(TAG, "orientation  = " + orientation);
        */
    }
}
