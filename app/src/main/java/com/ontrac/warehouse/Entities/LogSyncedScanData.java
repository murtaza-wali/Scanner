package com.ontrac.warehouse.Entities;


import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.util.Date;

import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.RealmResults;

public class LogSyncedScanData extends RealmObject {
    public String ScanId;
    public String StatusCode;
    public String Prompt1;
    public String Prompt2;
    public String ScanDate;
    public int UserNumber;
    public String FacilityCode;
    public String DeviceId;

    public int ApiResponseCode;
    public String ApiResponseBody;
    public String CreatedOn;

    public static Thread Save(ScanDataCache cache, int apiResponseCode, String apiResponseBody) {
        DateTime nowUTC = new DateTime(DateTimeZone.UTC);
        DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
        String now = nowUTC.toString(fmt);

        final LogSyncedScanData o = new LogSyncedScanData();
        o.ScanId = cache.ScanId;
        o.StatusCode = cache.StatusCode;
        o.Prompt1 = cache.Prompt1;
        o.Prompt2 = cache.Prompt2;
        o.ScanDate = cache.ScanDate;
        o.UserNumber = cache.UserNumber;
        o.FacilityCode = cache.FacilityCode;
        o.DeviceId = cache.DeviceId;

        o.ApiResponseCode = apiResponseCode;
        o.ApiResponseBody = apiResponseBody;
        o.CreatedOn = now;

        Thread t = new Thread(new Runnable() {
            public void run() {
                Realm realm = Realm.getDefaultInstance();

                realm.beginTransaction();
                realm.copyToRealm(o);
                realm.commitTransaction();

                if (!realm.isClosed()) {
                    realm.close();
                }
            }
        });
        t.start();

        return t;
    }

    public static void DeleteAll(){
        Realm realm = Realm.getDefaultInstance();

        RealmResults<LogSyncedScanData> results = realm.where(LogSyncedScanData.class).findAll();
        realm.beginTransaction();
        results.deleteAllFromRealm();
        realm.commitTransaction();

        if (!realm.isClosed()) {
            realm.close();
        }
    }
}
