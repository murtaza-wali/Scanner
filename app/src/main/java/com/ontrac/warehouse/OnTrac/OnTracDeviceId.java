package com.ontrac.warehouse.OnTrac;

import android.os.Environment;

import com.google.gson.Gson;
import com.ontrac.warehouse.Entities.OnTracAssetTag;
import com.ontrac.warehouse.Utilities.FileSystem;

import java.io.File;

public class OnTracDeviceId {
    private static OnTracAssetTag assetTag = new OnTracAssetTag();

    private static String Filename = "OnTracDeviceId.json";

    public OnTracDeviceId() {
        assetTag.AssetTag = "Unknown";
        ReadFile();
    }

    public void SetAssetTag(String AssetTag){
        assetTag.AssetTag = AssetTag;
        File storage = Environment.getExternalStorageDirectory();
        File file = new File(storage.getAbsolutePath(), Filename);
        String json = "{}";
        try {
            String test = assetTag.AssetTag;
            json = assetTag.toJson();
            FileSystem.writeFile(file, json);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public String GetAssetTag(){
        return assetTag.AssetTag;
    }

    public void ReadFile(){
        File storage = Environment.getExternalStorageDirectory();
        File file = new File(storage.getAbsolutePath(), Filename);
        String json = "{}";
        if (file.exists()){
            json = FileSystem.readFile(file);
            if (json != null)
                assetTag = new Gson().fromJson(json,OnTracAssetTag.class);
        }
    }
}
