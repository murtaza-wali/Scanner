package com.ontrac.warehouse.Utilities;


import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

public class BuildInfo {

    public static void Get(Application application){
        String result = "";

        PackageInfo pInfo = null;

        try {
            pInfo = application.getPackageManager().getPackageInfo(application.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        if (pInfo != null) {

        }
        String version = pInfo.versionName;
    }

    public class BuildInfoResult {
        //public String Verson
    }
}
