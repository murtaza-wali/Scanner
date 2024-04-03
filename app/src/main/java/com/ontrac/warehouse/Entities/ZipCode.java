package com.ontrac.warehouse.Entities;

import com.ontrac.warehouse.OnTrac.APIs;

import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.annotations.PrimaryKey;

public class ZipCode extends RealmObject {
    @PrimaryKey
    public String PackageZip;
    public String SchemeZip;

    public ZipCode() {}

    public ZipCode(APIs.ZipSchemeObject item) {
        PackageZip = item.packageZip;
        SchemeZip = item.schemeZip;
    }

    public static void DeleteAll(){
        Realm realm = Realm.getDefaultInstance();

        RealmResults<ZipCode> results = realm.where(ZipCode.class).findAll();
        realm.beginTransaction();
        results.deleteAllFromRealm();
        realm.commitTransaction();

        if (!realm.isClosed()) {
            realm.close();
        }
    }

    public static long Count(){
        Realm realm = Realm.getDefaultInstance();
        return realm.where(ZipCode.class).count();
    }

    public static int Save(APIs.ZipSchemeObject[] items){
        int recordCount = 0;

        Realm realm = Realm.getDefaultInstance();

        // delete existing data
        /*
        RealmQuery<ZipCode> query = realm.where(ZipCode.class);
        final RealmResults<ZipCode> zips = query.findAll();
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                zips.deleteAllFromRealm();
            }
        });
        */

        realm.beginTransaction();

        for (APIs.ZipSchemeObject item: items) {
            realm.copyToRealm(new ZipCode(item));
            recordCount += 1;
        }

        realm.commitTransaction();

        if (!realm.isClosed()) {
            realm.close();
        }

        return recordCount;
    }
}