package com.ontrac.warehouse.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import static com.ontrac.warehouse.OnTrac.Utilities.LogoutAndGotoLogin;

public class PowerControlReceiver extends BroadcastReceiver {
    public void onReceive(Context context , Intent intent) {
        String action = intent.getAction();

        if(action.equals(Intent.ACTION_POWER_CONNECTED)) {
            LogoutAndGotoLogin(context);
        } else if(action.equals(Intent.ACTION_POWER_DISCONNECTED)) {

        }
    }
}