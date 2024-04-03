package com.ontrac.warehouse.Utilities.UX;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.view.View;
import android.webkit.WebView;
import android.widget.EditText;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.Callable;

public class General {

    public static String ViewName(View view){
        String viewName = view.toString();
        viewName = viewName.substring(viewName.lastIndexOf("/") + 1);
        viewName = viewName.substring(0, viewName.length() - 1);
        return viewName;
    }

    public static void ClearOnLongClick(View view){
        view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (v instanceof EditText) {
                    ((EditText)v).setText("");
                }
                return true;
            }
        });
    }

    @Nullable
    public static Activity getActivity() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        Class activityThreadClass = Class.forName("android.app.ActivityThread");
        Object activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
        Field activitiesField = activityThreadClass.getDeclaredField("mActivities");
        activitiesField.setAccessible(true);

        Map<Object, Object> activities = (Map<Object, Object>) activitiesField.get(activityThread);
        if(activities == null) {
            return null;
        }

        for (Object activityRecord : activities.values()) {
            Class activityRecordClass = activityRecord.getClass();
            Field pausedField = activityRecordClass.getDeclaredField("paused");
            pausedField.setAccessible(true);
            if (!pausedField.getBoolean(activityRecord)) {
                Field activityField = activityRecordClass.getDeclaredField("activity");
                activityField.setAccessible(true);
                Activity activity = (Activity) activityField.get(activityRecord);
                return activity;
            }
        }

        return null;
    }

    public static void ClearTextInputLayoutError(Activity activity, int id)
    {
        SetTextInputLayoutError(activity, id, null);
    }
    public static void SetTextInputLayoutError(Activity activity, int id, String error)
    {
        TextInputLayout til = (TextInputLayout)activity.findViewById(id);

        til.setError(null);
        til.setErrorEnabled(false);

        if (error != null)
        {
            til.setErrorEnabled(true);
            til.setError(error);
        }
    }

    public static AlertDialog Alert(Activity activity, String title, String message)
    {
        return Alert(activity, title, message, false);
    }

    public static AlertDialog Alert(Activity activity, String title, String message, final Callable func)
    {
        return AlertStandard(activity, title, message, func);
    }

    public static AlertDialog Alert(Activity activity, String title, String message, boolean html)
    {
        AlertDialog result = null;

        if (html){
            AlertHtml(activity, title, message);
        } else {
            result = AlertStandard(activity, title, message);
        }

        return result;
    }

    private static AlertDialog AlertStandard(Activity activity, String title, String message)
    {
        return AlertStandard(activity, title, message, null);
    }

    public static class AlertBox implements Runnable
    {
        public AlertDialog Box;
        public Context Container;
        public String Title;
        public String Message;
        public Callable Function;

        public AlertBox(Activity container, String title, String message, Callable function) {
            this.Container = container;
            this.Title = title;
            this.Message = message;
            this.Function = function;
        }

        public void run()
        {
            Box = new AlertDialog.Builder(Container).create();
            Box.setTitle(Title);
            Box.setMessage(Message);
            Box.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            if (Function != null){
                                try {
                                    Function.call();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                            dialog.dismiss();
                        }
                    });
            Box.show();
        }
    }

    private static AlertDialog AlertStandard(final Activity activity, final String title, final String message, final Callable func)
    {
        final AlertBox ab = new AlertBox(activity, title, message, func);

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ab.run();
            }
        });

        return ab.Box;
    }

    private static void AlertHtml(final Activity activity, final String title, final String html) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog ad = new AlertDialog.Builder(activity).create();
                ad.setTitle(title);
                ad.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });

                WebView view = new WebView(ad.getContext());
                //view.getSettings().setJavaScriptEnabled(true);
                //view.getSettings().setDomStorageEnabled(true);
                view.loadData(html, "text/html; charset=utf-8", "UTF-8");

                ad.setView(view);
                ad.show();

                /*
                ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams)view.getLayoutParams();
                p.leftMargin = 20;
                p.rightMargin = 20;
                view.setLayoutParams(p);
                */

                /*
                alertDialog.getWindow().setLayout(600, 400); //Controlling width and height
                OR
                WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
                lp.copyFrom(ad.getWindow().getAttributes());
                lp.width = 150;
                lp.height = 500;
                lp.x=-170;
                lp.y=100;
                ad.getWindow().setAttributes(lp);
                */
            }
        });


    }

}
