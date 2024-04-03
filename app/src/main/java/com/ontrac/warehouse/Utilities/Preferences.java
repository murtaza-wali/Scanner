package com.ontrac.warehouse.Utilities;

import android.content.Context;
import android.content.SharedPreferences;

import static android.content.Context.MODE_PRIVATE;

public class Preferences {

    public static boolean Save(Context context, Enum key, String value)
    {
        return Save(context, key.toString(), value);
    }

    public static boolean Save(Context context, String key, String value)
    {
        SharedPreferences settings = context.getSharedPreferences("PersistentStore", MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(key, value);
        return editor.commit();
    }

    public static String Get(Context context, Enum key)
    {
        return Get(context, key.toString(), null);
    }

    public static String Get(Context context, Enum key, String defaultValue)
    {
        return Get(context, key.toString(), defaultValue);
    }

    public static String Get(Context context, String key)
    {
        return Get(context, key, null);
    }

    public static String Get(Context context, String key, String defaultValue)
    {
        String result;

        SharedPreferences settings = context.getSharedPreferences("PersistentStore", MODE_PRIVATE);

        result = settings.getString(key, defaultValue);

        return result;
    }


}
