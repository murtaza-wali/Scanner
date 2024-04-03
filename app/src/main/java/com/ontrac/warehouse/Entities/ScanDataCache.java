package com.ontrac.warehouse.Entities;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.RealmResults;
import io.realm.annotations.PrimaryKey;

import static com.ontrac.warehouse.OnTrac.Utilities.SubstituteGroupSeparator;

public class ScanDataCache extends RealmObject {
    @PrimaryKey
    public String ScanId;
    public String StatusCode;
    public String Prompt1;
    public String Prompt2;
    public String ScanDate;
    public int UserNumber;
    public String FacilityCode;
    public String DeviceId;
    public String OnTrac2DBarcode;
//    public String TimeZoneOffset;

    public static Thread Save(String statusCode, String prompt1, String prompt2, int userNumber, String facilityCode, String deviceId, String onTrac2DBarcode){
        DateTime nowUTC = new DateTime(DateTimeZone.UTC);
        DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
        String now = nowUTC.toString(fmt);

        final ScanDataCache sd = new ScanDataCache();
        sd.ScanId = java.util.UUID.randomUUID().toString();
        sd.StatusCode = statusCode;
        sd.Prompt1 = SubstituteGroupSeparator(prompt1);
        sd.Prompt2 = SubstituteGroupSeparator(prompt2);
        sd.ScanDate = now;
        sd.UserNumber = userNumber;
        sd.FacilityCode = facilityCode;
        sd.DeviceId = deviceId;
        sd.OnTrac2DBarcode = onTrac2DBarcode ;
//        sd.TimeZoneOffset = timeZoneOffset;

        Thread t = new Thread(new Runnable() {
            public void run() {
                Realm realm = Realm.getDefaultInstance();

                realm.beginTransaction();
                realm.copyToRealm(sd);
                realm.commitTransaction();

                if (!realm.isClosed()) {
                    realm.close();
                }
            }
        });
        t.start();

        return t;
    }

    public static ScanDataCache Select(final String scanId){
        ScanDataCache result = null;

        Realm realm = Realm.getDefaultInstance();
        result = Clone(realm.where(ScanDataCache.class).equalTo("ScanId", scanId).findFirst());

        if (!realm.isClosed()) {
            realm.close();
        }
        return result;

    }

    public static ScanDataCache Clone(ScanDataCache self) {
        ScanDataCache result = new ScanDataCache();
        result.ScanId = self.ScanId;
        result.StatusCode = self.StatusCode;
        result.Prompt1 = self.Prompt1;
        result.Prompt2 = self.Prompt2;
        result.ScanDate = self.ScanDate;
        result.UserNumber = self.UserNumber;
        result.FacilityCode = self.FacilityCode;
        result.DeviceId = self.DeviceId;
        result.OnTrac2DBarcode = self.OnTrac2DBarcode;

        return result;
    }

    public static void Delete(final String scanId, boolean log, String... args){
        Realm realm = Realm.getDefaultInstance();

        realm.beginTransaction();

        ScanDataCache item = realm.where(ScanDataCache.class).equalTo("ScanId", scanId).findFirst();

        if (log){
            LogSyncedScanData.Save(ScanDataCache.Clone(item), Integer.parseInt(args[0]), args[1]);
        }

        item.deleteFromRealm();

        realm.commitTransaction();

        if (!realm.isClosed()) {
            realm.close();
        }
    }

    public static void Delete(final String scanId){
        Realm realm = Realm.getDefaultInstance();

        ScanDataCache item = realm.where(ScanDataCache.class).equalTo("ScanId", scanId).findFirst();

        realm.beginTransaction();
        item.deleteFromRealm();
        realm.commitTransaction();

        if (!realm.isClosed()) {
            realm.close();
        }
    }

    /*
    public static Thread Delete(final String scanId){
        Thread t = new Thread(new Runnable() {
            public void run() {
                Realm realm = Realm.getDefaultInstance();

                ScanDataCache item = realm.where(ScanDataCache.class).equalTo("ScanId", scanId).findFirst();

                realm.beginTransaction();
                item.deleteFromRealm();
                realm.commitTransaction();

                if (!realm.isClosed()) {
                    realm.close();
                }
            }
        });
        t.start();

        return t;
    }
    */

    public static long Count(){
        long result = 0;
        Realm realm = Realm.getDefaultInstance();
        result = realm.where(ScanDataCache.class).count();
        if (!realm.isClosed()) {
            realm.close();
        }
        return result;
    }

    public static void DeleteAll(){
        Realm realm = Realm.getDefaultInstance();

        RealmResults<ScanDataCache> results = realm.where(ScanDataCache.class).findAll();
        realm.beginTransaction();
        results.deleteAllFromRealm();
        realm.commitTransaction();

        if (!realm.isClosed()) {
            realm.close();
        }
    }
}
