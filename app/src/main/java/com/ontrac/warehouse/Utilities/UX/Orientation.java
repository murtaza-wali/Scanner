package com.ontrac.warehouse.Utilities.UX;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

public class Orientation {

    //region "Enumerations"
    /*
    public static final int ORIENTATION_UNDEFINED = 0;
    public static final int ORIENTATION_PORTRAIT = 1;
    public static final int ORIENTATION_LANDSCAPE = 2;

    public static final int ROTATION_0 = 0;
    public static final int ROTATION_90 = 1;
    public static final int ROTATION_180 = 2;
    public static final int ROTATION_270 = 3;
    */
    //endregion

    public static boolean lockOrientationLandscape(Activity activity) {
        boolean result = false;
        int targetOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;

        //Display display = ((WindowManager)activity.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        //int rotation = display.getRotation();
        int currentOrientation = activity.getResources().getConfiguration().orientation;

        if (currentOrientation != Configuration.ORIENTATION_LANDSCAPE) {
            result = true;
        }

        activity.setRequestedOrientation(targetOrientation);

        return result;
    }

    public static boolean lockOrientationPortrait(Activity activity) {
        boolean result = false;
        int targetOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;

        //Display display = ((WindowManager)activity.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        //int rotation = display.getRotation();
        int currentOrientation = activity.getResources().getConfiguration().orientation;

        if (currentOrientation != Configuration.ORIENTATION_PORTRAIT ) {
            result = true;
        }

        activity.setRequestedOrientation(targetOrientation);

        return result;
    }

    public static boolean isLandscape(Activity activity) {
        boolean result = false;

        int currentOrientation = activity.getResources().getConfiguration().orientation;

        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            result = true;
        }

        return result;
    }

    public static boolean isPortrait(Activity activity) {
        boolean result = false;

        int currentOrientation = activity.getResources().getConfiguration().orientation;

        if (currentOrientation == Configuration.ORIENTATION_PORTRAIT ) {
            result = true;
        }

        return result;
    }

    // Locks the device window in actual screen mode
    public static void lockOrientation(Activity activity) {
        Display display = ((WindowManager) activity.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int rotation = display.getRotation();
        int tempOrientation = activity.getResources().getConfiguration().orientation;
        int orientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;

        switch (tempOrientation) {
            case Configuration.ORIENTATION_LANDSCAPE: {
                if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_90) {
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                } else {
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                }
                break;
            }
            case Configuration.ORIENTATION_PORTRAIT: {
                if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_270) {
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                } else {
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                }
                break;
            }
        }
        activity.setRequestedOrientation(orientation);
    }

    public static void unlockOrientation(Activity activity) {
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
    }

}
