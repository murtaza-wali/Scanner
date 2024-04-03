package com.ontrac.warehouse.Utilities.Zebra;


import android.media.AudioManager;
import android.media.ToneGenerator;

import com.symbol.emdk.notification.Notification;
import com.symbol.emdk.notification.NotificationDevice;

public class Notifications {

    public static void ScanError(NotificationDevice notificationDevice){
        ToneGenerator scaninvalidps = new ToneGenerator(AudioManager.STREAM_DTMF, 100);

        if (scaninvalidps != null) {
            scaninvalidps.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD);
        }

        Notification notification = new Notification();

        notification.led.color = 0xFF0000;
        notification.led.onTime = 250;
        notification.led.offTime = 100;
        notification.led.repeatCount = 3;

        notification.beep.pattern = new Notification.Beep[4];
        notification.beep.pattern[0] = new Notification.Beep();
        notification.beep.pattern[0].frequency = 1000;
        notification.beep.pattern[0].time = 325;
        notification.beep.pattern[1] = new Notification.Beep();
        notification.beep.pattern[1].frequency = 2000;
        notification.beep.pattern[1].time = 325;
        notification.beep.pattern[2] = new Notification.Beep();
        notification.beep.pattern[2].frequency = 3000;
        notification.beep.pattern[2].time = 325;
        notification.beep.pattern[3] = new Notification.Beep();
        notification.beep.pattern[3].frequency = 4000;
        notification.beep.pattern[3].time = 325;

        notification.vibrate.pattern = new long[]{0,250,100,250,100,250,100,250};

        try {
            if (notificationDevice != null)
            {
                notificationDevice.notify(notification);
            }
        } catch (Exception e) {
            //Log.wtf(TAG, "ScanError()");
            //e.printStackTrace();
        }
    }
}
