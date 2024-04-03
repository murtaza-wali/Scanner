package com.ontrac.warehouse.Entities;

import com.google.gson.Gson;

public class OnTracAssetTag {
    public String AssetTag;
    public String toJson()
    {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
