package com.ontrac.warehouse.Utilities;


import android.content.Context;
import android.os.Vibrator;
import android.widget.Toast;

import com.ontrac.warehouse.BaseApplication;

public class Notifications {

    public static void Vibrate(){
        Vibrate(100);
    }

    public static void Vibrate(int length){
        Vibrator v = (Vibrator)BaseApplication.getAppContext().getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(length);
    }

    public static Toast Toast(String message, int duration){
        Toast result = Toast.makeText(BaseApplication.getAppContext(), message, duration);
        result.show();
        return result;
    }

    public static Toast ToastShort(String message){
        return Toast(message, Toast.LENGTH_SHORT);
    }

    public static Toast ToastLong(String message){
        return Toast(message, Toast.LENGTH_LONG);
    }
}
