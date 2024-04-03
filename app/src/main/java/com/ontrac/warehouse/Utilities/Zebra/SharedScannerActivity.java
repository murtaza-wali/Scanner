package com.ontrac.warehouse.Utilities.Zebra;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.ontrac.warehouse.BaseApplication;
import com.ontrac.warehouse.Utilities.Zebra.Scanner;

public class SharedScannerActivity extends AppCompatActivity implements Scanner.IScanListener {
    BaseApplication baseApp = null;

    public boolean CanScan = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        baseApp = ((BaseApplication)getApplicationContext());
    }

    @Override
    public void onResume() {
        super.onResume();
        //Log.wtf("SharedScannerActivity", "onResume()");

        if (baseApp.SharedScanner == null || (baseApp.SharedScanner != null && !baseApp.SharedScanner.isScannerConnected())){
            Restart();
        }

        if (baseApp.SharedScanner != null)
        {
            baseApp.SharedScanner.CanScan = this.CanScan;
            baseApp.SharedScanner.removeAllListeners();
            baseApp.SharedScanner.addListener(this);
            baseApp.SharedScanner.resume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        //Log.wtf("SharedScannerActivity", "onPause()");

        if (baseApp.SharedScanner != null) {
            baseApp.SharedScanner.CanScan = this.CanScan;
            baseApp.SharedScanner.removeListener(this);
            baseApp.SharedScanner.pause();
        }
    }

    @Override
    public void Restart() {
        //Log.wtf("SharedScannerActivity", "Restart()");
        if (baseApp.SharedScanner != null)
        {
            boolean destroyed = baseApp.SharedScanner.destroy();
        }
        baseApp.SharedScanner = null;
        baseApp.SharedScanner = new com.ontrac.warehouse.Utilities.Zebra.Scanner(this, this, baseApp.EmdkProfile);
    }

    @Override
    public void Scanned(String data) {

    }
}
