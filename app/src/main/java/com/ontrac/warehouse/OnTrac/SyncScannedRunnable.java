package com.ontrac.warehouse.OnTrac;


import android.os.Handler;

import com.ontrac.warehouse.BaseApplication;
import com.ontrac.warehouse.Entities.ScanDataCache;
import com.ontrac.warehouse.Utilities.Networks;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;
import okhttp3.Request;

import static com.ontrac.warehouse.OnTrac.Utilities.OriginalGroupSeparator;

public class SyncScannedRunnable implements Runnable, APIsEventListener {
    private int _delay;
    private Handler _handler;
    private Command _callback;
    private APIs _apis;
    private String _apiEndpoint;
    private boolean _isBusy;
    private BaseApplication baseApp = null;

    public SyncScannedRunnable(Handler handler, int delay, Command callback, APIs apis, String apiEndpoint) {
        this._delay = delay;
        this._handler = handler;
        this._callback = callback;
        this._apis = apis;
        this._apis.addListener(this);
        this._apiEndpoint = apiEndpoint;
        this._isBusy = false;

        baseApp = ((BaseApplication)BaseApplication.getAppContext().getApplicationContext());
    }

    @Override
    public void run() {
        //Log.wtf("SyncScannedRunnable", "run() @ " + new java.util.Date());

        boolean hasDataConnection = Networks.hasWiFiConnection(BaseApplication.getAppContext()) | Networks.hasMobileConnection(BaseApplication.getAppContext());

        if (hasDataConnection) {
            Realm realm = Realm.getDefaultInstance();

            RealmResults<ScanDataCache> items = realm.where(ScanDataCache.class).findAll();

            if (!realm.isClosed()) {
                realm.close();
            }

            ArrayList<APIs.ScanObject> scanObjects = new ArrayList<>();

            for (ScanDataCache item : items) {
                APIs.ScanObject scan = new APIs.ScanObject();
                scan.ScanId = item.ScanId;
                scan.StatusCode = item.StatusCode;
                scan.Prompt1 = OriginalGroupSeparator(item.Prompt1);
                scan.Prompt2 = OriginalGroupSeparator(item.Prompt2);
                scan.Scandatetime = item.ScanDate;
                scan.OnTrac2DBarcode = item.OnTrac2DBarcode;
//                scan.TimeZoneOffset = item.TimeZoneOffset;

                scanObjects.add(scan);
            }

            if (scanObjects.size() > 0)
            {
                this._isBusy = true;
                Request request = this._apis.BuildPost(this._apiEndpoint)   ;
                this._apis.CallForScanObjects(APIs.Type.Scan, request, scanObjects);
            }

            _callback.execute(null);
        }

        if (!this._isBusy){
            _handler.postDelayed(this, _delay);
        }
    }

    @Override
    public void RequestCompleted(int type, int responseCode, String responseBody, Object data) throws Exception {
        if (type == APIs.Type.Scan) {
            //Log.wtf("SyncScannedRunnable", "RequestCompleted() @ " + new java.util.Date());

            String scanId = (String)data;
            ScanDataCache.Delete(scanId, baseApp.config.LogSynced, String.valueOf(responseCode), responseBody);
            /*
            if (baseApp.config.LogSynced){
                ScanDataCache item = ScanDataCache.Select(scanId);
                LogSyncedScanData.Save(item, responseCode, responseBody);
            }
            ScanDataCache.Delete(scanId);
            */
        } else {
            throw new Exception("Unhandled API call!");
        }
    }

    @Override
    public void CallCompleted(int type, Object data, List<ClientConnectionError> errors) {
        this._isBusy = false;
        _handler.postDelayed(this, _delay);

        //TODO Show toasts if errors
    }

    public interface Command
    {
        public void execute(Object data);
    }
}


