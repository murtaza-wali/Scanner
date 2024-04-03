package com.ontrac.warehouse.Utilities.Zebra;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.ontrac.warehouse.BaseApplication;
import com.symbol.emdk.EMDKManager;
import com.symbol.emdk.EMDKResults;
import com.symbol.emdk.ProfileManager;
import com.symbol.emdk.barcode.BarcodeManager;
import com.symbol.emdk.barcode.ScanDataCollection;
import com.symbol.emdk.barcode.ScannerException;
import com.symbol.emdk.barcode.ScannerInfo;
import com.symbol.emdk.barcode.ScannerResults;
import com.symbol.emdk.barcode.StatusData;
import com.symbol.emdk.notification.NotificationDevice;
import com.symbol.emdk.notification.NotificationException;
import com.symbol.emdk.notification.NotificationManager;

import java.util.ArrayList;
import java.util.List;

public class Scanner implements EMDKManager.EMDKListener, BarcodeManager.ScannerConnectionListener, com.symbol.emdk.barcode.Scanner.DataListener, com.symbol.emdk.barcode.Scanner.StatusListener {
    public interface IScanListener {
        void Restart();
        void Scanned(String data);
    }

    private static final String TAG = Scanner.class.getSimpleName();

    private List<IScanListener> _listeners = new ArrayList<IScanListener>();

    private Context _context = null;
    private EMDKManager _emdkManager = null;
    private String _emdkProfileName = ""; // profile name used in EMDKConfig.xml
    public BarcodeManager _barcodeManager = null;
    public com.symbol.emdk.barcode.Scanner _scanner = null;
    private NotificationManager _notificationManager = null;
    private NotificationDevice _notificationDevice = null;
    private int _lastScanTimeHash;

    BaseApplication baseApp = null;

    public boolean CanScan = true;

    public Scanner(Activity activity, IScanListener listener, String profile){
        this._context = activity.getApplicationContext();
        this._emdkProfileName = profile;
        this.addListener(listener);
        baseApp = ((BaseApplication) activity.getApplicationContext());
        this.initialize();
    }

    public NotificationDevice getNotificationDevice() {
        return _notificationDevice;
    }

    public void addListener(IScanListener listener) {
        _listeners.add(listener);
    }

    public void removeListener(IScanListener listener) {
        _listeners.remove(listener);
    }

    public void removeAllListeners() {
        _listeners.clear();
    }

    @Override
    public void onOpened(EMDKManager emdkManager) {
        //Log.i(TAG, "onOpened()");
        _emdkManager = emdkManager;

        //Get the ProfileManager object to process the profiles
        ProfileManager profileManager = (ProfileManager) emdkManager.getInstance(EMDKManager.FEATURE_TYPE.PROFILE);

        if(profileManager != null)
        {
            try{
                String[] modifyData = null;//new String[1];

                EMDKResults results = profileManager.processProfile(_emdkProfileName, ProfileManager.PROFILE_FLAG.SET, modifyData);

                //Log.i(TAG, "EMDKResults.STATUS_CODE = " + results.statusCode);

                if(results.statusCode == EMDKResults.STATUS_CODE.FAILURE)
                {
                    //Log.i(TAG, "Failed to set EMDK profile");
                }

                if (results.statusCode == EMDKResults.STATUS_CODE.SUCCESS)
                {
                    initScanner();
                }
            }catch (Exception e){
                Log.e(TAG, "EXCEPTION Process Profile");
                //e.printStackTrace();
            }
        }
    }

    @Override
    public void onClosed() {

    }

    @Override
    public void onConnectionChange(ScannerInfo scannerInfo, BarcodeManager.ConnectionState connectionState) {
        //Log.i(TAG, "onConnectionChange() State: " + connectionState);

        if (connectionState == BarcodeManager.ConnectionState.DISCONNECTED) {
            for (IScanListener listener : _listeners) {
                listener.Restart();
            }

        } else if (connectionState == BarcodeManager.ConnectionState.CONNECTED) {
            for (IScanListener listener : _listeners) {
                listener.Restart();
            }
        }
    }

    public void pause() {
        if (_scanner != null) {
            try {
                _scanner.disable();
            } catch (ScannerException e) {
               // e.printStackTrace();
            }
        }
    }

    public void resume() {
        if (_scanner != null){
            _scanner.removeDataListener(this);
            _scanner.removeStatusListener(this);
        }

        if (_scanner != null && CanScan) {
            _scanner.addDataListener(this);
            _scanner.addStatusListener(this);

            try {
                _scanner.enable();
            } catch (ScannerException e) {
                //Log.wtf(TAG, "Failed to scanner.enable()");
                //e.printStackTrace();
            }

            if (_scanner.isEnabled()) {
                try {
                    if (!_scanner.isReadPending()) {
                        _scanner.read();
                    }
                } catch (ScannerException e) {
                    //Log.wtf(TAG, "resume(): Failed to scanner.read().");
                    //e.printStackTrace();
                }
            }
        }
    }

    public boolean isScannerConnected()
    {
        boolean result = false;

        if (_barcodeManager != null && _scanner != null)
        {
            result = _scanner.getScannerInfo().isConnected();
        }

        return result;
    }

    public void initialize() {
        //Log.i(TAG, "initializeEmdk()");

        terminateScanner();

        terminateBarcodeManager();

        EMDKResults results = EMDKManager.getEMDKManager(_context, this); // The EMDKManager object will be created and returned in the callback.

        if (results.statusCode == EMDKResults.STATUS_CODE.FAILURE) {
            //Log.e(TAG, "Failed to create EMDKManager object");
        }
    }

    public void terminate() {
        //Log.i(TAG, "terminateEmdk()");

        terminateScanner();

        terminateBarcodeManager();

        if (_emdkManager != null) {
            //_emdkManager.release();
            //_emdkManager = null;
        }
    }

    public boolean destroy()
    {
        boolean result = false;

        if (_emdkManager != null) {
            _emdkManager.release();
            _emdkManager = null;

            result = true;
        }

        return result;
    }

    @Override
    public void onData(ScanDataCollection scanDataCollection) {
        //Log.i(TAG, "onData()");

        if ((scanDataCollection != null) && (scanDataCollection.getResult() == ScannerResults.SUCCESS)) {
            ArrayList<ScanDataCollection.ScanData> scanData = scanDataCollection.getScanData();


            for (ScanDataCollection.ScanData data : scanData) {
                if (_lastScanTimeHash != data.getTimeStamp().hashCode()){
                    _lastScanTimeHash = data.getTimeStamp().hashCode();

                    final String dataString = data.getData();

                    // Raise Events
                    for (IScanListener listener : _listeners) {
                        listener.Scanned(dataString);
                    }
                }
            }
        }
    }

    @Override
    public void onStatus(StatusData statusData) {
        //Log.i(TAG, "onStatus() State: " + statusData.getState());

        if (statusData != null)
        {
            if (statusData.getState() == StatusData.ScannerStates.IDLE){
                try {
                    //_scanner.addDataListener(this);
                    if (!_scanner.isReadPending()) {
                        _scanner.read();
                    }
                } catch (ScannerException e) {
                    //Log.d(TAG, "onStatus(): ### scanner.read() exception ###");
                    //e.printStackTrace();
                }
            }
        }
    }

    protected void terminateScanner() {
        //Log.i(TAG, "terminateScanner()");

        if (_scanner != null) {

            try {
                _scanner.cancelRead();
            } catch (ScannerException e) {
                //Log.i(TAG, "EXCEPTION scanner.cancelRead()");
                //e.printStackTrace();
            }

            _scanner.removeDataListener(this);
            _scanner.removeStatusListener(this);

            try {
                _scanner.disable();
            } catch (ScannerException e) {
                //Log.i(TAG, "EXCEPTION scanner.disable()");
                //e.printStackTrace();
            }

            try {
                if (_scanner.getScannerInfo().isConnected()) {
                    _scanner.release();
                }
            } catch (ScannerException e) {
                //Log.i(TAG, "EXCEPTION scanner.release()");
                //e.printStackTrace();
            }
            _scanner = null;
        }
    }

    protected void terminateBarcodeManager() {
        //Log.i(TAG, "terminateBarcodeManager()");

        if (_barcodeManager != null) {

            try {
                _barcodeManager.removeConnectionListener(this);
            } catch (Exception e) {
                //Log.d(TAG, "EXCEPTION barcodeManager.removeConnectionListener()");
                //e.printStackTrace();
            }

            _barcodeManager = null;
        }
    }

    protected void initializeNotification() {
        if (_emdkManager != null) {
            _notificationManager = (NotificationManager) _emdkManager.getInstance(EMDKManager.FEATURE_TYPE.NOTIFICATION);

            try {
                if (_notificationManager != null){
                    _notificationDevice = _notificationManager.getDevice(NotificationManager.DeviceIdentifier.DEFAULT);
                    if (_notificationDevice != null) {
                        _notificationDevice.enable();
                    }
                }
            } catch (NotificationException e) {
                //e.printStackTrace();
            }
        }
    }



    protected void initScanner() {
        //Log.i(TAG, "initScanner()");

        if(_emdkManager != null){
            //Log.wtf(TAG, "Creating Barcode Manager");

            _barcodeManager = (BarcodeManager)_emdkManager.getInstance(EMDKManager.FEATURE_TYPE.BARCODE);

            if (_barcodeManager != null) {
                //Log.wtf(TAG, "Creating Scanner");

                _barcodeManager.addConnectionListener(this);

                if (baseApp.ScanDevice == null)
                    _scanner = _barcodeManager.getDevice(BarcodeManager.DeviceIdentifier.DEFAULT);
                else
                    _scanner = _barcodeManager.getDevice(baseApp.ScanDevice);

                if (_scanner == null) {
                    _scanner = _barcodeManager.getDevice(BarcodeManager.DeviceIdentifier.PLUGGABLE_LASER1);
                }

                if (_scanner == null) {
                    _scanner = _barcodeManager.getDevice(BarcodeManager.DeviceIdentifier.BLUETOOTH_IMAGER_RS6000);
                }
            }
        }

        if (_scanner != null) {
            //Log.wtf(TAG, "Adding scanner listeners");

            _scanner.triggerType = com.symbol.emdk.barcode.Scanner.TriggerType.HARD;

            resume();

            initializeNotification();
        }

        /*
        for (IScanListener listener : _listeners) {
            listener.OnInitializationEnd();
        }
        */
    }

}

