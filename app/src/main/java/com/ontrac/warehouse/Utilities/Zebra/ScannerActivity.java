package com.ontrac.warehouse.Utilities.Zebra;

import android.support.v7.app.AppCompatActivity;

import com.ontrac.warehouse.BaseApplication;
import com.ontrac.warehouse.Utilities.Zebra.Scanner;

public class ScannerActivity extends AppCompatActivity implements Scanner.IScanListener {
    protected Scanner _scanner = null;

    @Override
    public void onResume() {
        super.onResume();
        //Log.wtf("ScannerActivity", "onResume()");

        if (_scanner == null || (_scanner != null && !_scanner.isScannerConnected())){
            Restart();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (_scanner != null)
        {
            boolean destroyed = _scanner.destroy();
        }
        _scanner = null;
    }

    @Override
    public void Restart() {
        //Log.wtf("ScannerActivity", "Restart()");

        BaseApplication baseApp = ((BaseApplication)getApplicationContext());

        if (_scanner != null)
        {
            boolean destroyed = _scanner.destroy();
        }
        _scanner = null;
        _scanner = new com.ontrac.warehouse.Utilities.Zebra.Scanner(this, this, baseApp.EmdkProfile);
    }

    @Override
    public void Scanned(String data) {

    }
}
