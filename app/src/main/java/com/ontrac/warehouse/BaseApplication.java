package com.ontrac.warehouse;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.Handler;
import android.util.Base64;
import com.facebook.stetho.Stetho;
import com.google.gson.Gson;
import com.ontrac.warehouse.OnTrac.APIs;
import com.ontrac.warehouse.Entities.Config;
import com.ontrac.warehouse.Entities.User;
import com.ontrac.warehouse.OnTrac.OnTracDeviceId;
import com.ontrac.warehouse.OnTrac.SyncScannedRunnable;
import com.ontrac.warehouse.Utilities.FileSystem;
import com.ontrac.warehouse.Utilities.UX.Orientation;
import com.ontrac.warehouse.Utilities.Zebra.Scanner;
import com.symbol.emdk.barcode.ScannerInfo;
import com.uphyca.stetho_realm.RealmInspectorModulesProvider;

import java.io.File;
import java.security.SecureRandom;
import java.util.Arrays;
import io.realm.Realm;
import io.realm.RealmConfiguration;

public class BaseApplication extends Application implements SyncScannedRunnable.Command, Scanner.IScanListener {
    public Config config = null;
    public User authUser = null;
    private static Context context;

    public String EmdkProfile = "OnTrac Warehouse";
    public String DeviceModel = "";
//    public String DeviceSerial = "";
    public String LandscapeModels[] = new String[]{"wt6000", "wt6300"};
    public ScannerInfo ScanDevice = null;

    //region Shared Scanner
    public Scanner SharedScanner = null;

    public OnTracDeviceId onTracDeviceId = null;

    @Override
    public void Restart() { }
    @Override
    public void Scanned(String data) {}
    //endregion

    @Override
    public void onCreate() {
        super.onCreate();

        //TODO: Take out after upgrading the TC75's OS
        //MultiDex.install(this);

        BaseApplication.context = getApplicationContext();

        DeviceModel = android.os.Build.MODEL;
        //DeviceSerial = android.os.Build.SERIAL;
        //DeviceSerial = "UNKNOWN";
        onTracDeviceId = new OnTracDeviceId();
        initializeConfig();

        byte[] dbKey = null;

        SharedPreferences settings = getSharedPreferences("PersistentStore", MODE_PRIVATE);

        if (settings.contains("RealmEncryptionKey"))
        {
            String keyAsString = settings.getString("RealmEncryptionKey", null);
            dbKey = Base64.decode(keyAsString, Base64.DEFAULT);
        }
        else
        {
            dbKey = new byte[64];
            new SecureRandom().nextBytes(dbKey);
            String keyAsString = Base64.encodeToString(dbKey, Base64.DEFAULT);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("RealmEncryptionKey", keyAsString);
            editor.commit();
        }
        Realm.init(this);
        RealmConfiguration config = new RealmConfiguration.Builder().encryptionKey(dbKey).deleteRealmIfMigrationNeeded().build();
        //RealmConfiguration config = new RealmConfiguration.Builder().deleteRealmIfMigrationNeeded().build();
        Realm.setDefaultConfiguration(config);
        if (BuildConfig.DEBUG) {
            Stetho.initialize(
                    Stetho.newInitializerBuilder(this)
                            .enableDumpapp(Stetho.defaultDumperPluginsProvider(this))
                            .enableWebKitInspector(RealmInspectorModulesProvider.builder(this).build())
                            .build());
        }

    }

    public static Context getAppContext() {
        return BaseApplication.context;
    }

    public boolean forceLandscape(){
        return Arrays.asList(LandscapeModels).contains(DeviceModel.toLowerCase());
    }

    public boolean setOrientation(Activity activity) {
        boolean result = false;
        if(forceLandscape()) {
            result = Orientation.lockOrientationLandscape(activity);
        } else {
            result = Orientation.lockOrientationPortrait(activity);
        }
        return result;
    }

    private void initializeConfig() {
        String filename = "config.json";
        File storage = Environment.getExternalStorageDirectory();
        File file = new File(storage.getAbsolutePath(), filename);
        String json = "{}";
        if (file.exists()){
            json = FileSystem.readFile(file);
            if (json != null)
                config = new Gson().fromJson(json, Config.class);
            else
                config = new Config();
        } else {

            config = new Config();
        }
        try {
            json = new Gson().toJson(config);
            FileSystem.writeFile(file, json);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /*
    private void initializeIdentity() {
        String filename = "identity.json";

        File storage = Environment.getExternalStorageDirectory();
        //File file = new File(storage.getAbsolutePath(), "dir1"); // if we want to add a subdirectory at some time
        File file = new File(storage.getAbsolutePath(), filename);

        if (file.exists()){
            String json = FileSystem.readFile(file);

            identity = new Gson().fromJson(json, Identity.class) ;
        } else {
            identity = new Identity();
            identity.Asset = 56956;

            String json = new Gson().toJson(identity);

            try {
                FileSystem.writeFile(file, json);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }
    */

    ///////////////////////////////////////////////////////////////////////////////
    // Sync Scan Data
    ///////////////////////////////////////////////////////////////////////////////

    public Handler syncScannedHandler = new Handler();
    protected SyncScannedRunnable syncScannedRunner = null;

    public void CacheSyncStart() {
        if (syncScannedRunner == null) {
            int scanSyncInterval = config.ScanSyncInterval * 1000;
            String endpoint = config.ApiScan;
            String assetTag = onTracDeviceId.GetAssetTag();
            APIs apis = new APIs(config, authUser.Id, authUser.FacilityCode, assetTag);
            syncScannedRunner = new SyncScannedRunnable(syncScannedHandler, scanSyncInterval, this, apis, endpoint);
            syncScannedHandler.postDelayed(syncScannedRunner, 1);
        }
    }

    public void CacheSyncStop() {
        syncScannedHandler.removeCallbacks(syncScannedRunner);
    }

    @Override
    public void execute(Object data) {
        //SyncScannedRunnable callback
    }

    ///////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////

    /*
    private void initializeStetho(final Context context) {

        Stetho.initialize(Stetho.newInitializerBuilder(context)
                .enableDumpapp(Stetho.defaultDumperPluginsProvider(context))
                .enableWebKitInspector(RealmInspectorModulesProvider.builder(context)
                        .withDescendingOrder()
                        .withLimit(1000)
                        .databaseNamePattern(Pattern.compile(".+\\.realm"))
                        .build())
                .build());
    }
    */

    public enum Preference
    {
        SyncZipSchemeDate,
        SyncUserDate,
        AssetTag
    }
}
