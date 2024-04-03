package com.ontrac.warehouse.Utilities;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.ontrac.warehouse.BaseApplication;

public class Networks {

    public static boolean hasWiFiConnection(Context context) {
        boolean result = false;

        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni != null && ni.getType() == ConnectivityManager.TYPE_WIFI) {
            result = true;
        }

        return result;
    }

    public static boolean hasMobileConnection(Context context) {
        boolean result = false;

        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni != null && ni.getType() == ConnectivityManager.TYPE_MOBILE) {
            result = true;
        }

        return result;
    }

    public static boolean hasDataConnection(Context context) {
        return Networks.hasWiFiConnection(BaseApplication.getAppContext()) | Networks.hasMobileConnection(BaseApplication.getAppContext());
    }
}
