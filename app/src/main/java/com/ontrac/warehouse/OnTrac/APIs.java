package com.ontrac.warehouse.OnTrac;


import android.util.Log;

import com.ontrac.warehouse.BaseApplication;
import com.ontrac.warehouse.BuildConfig;
import com.ontrac.warehouse.Entities.Config;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class APIs {

    private String _onTracUserId;
    private String _onTracFacility;
    private String _onTracDeviceId;
    private String _ontracVersion = BuildConfig.VERSION_NAME;

    private BaseApplication baseApp = null;


    public APIs(String onTracUserId, String onTracFacility, String onTracDeviceId)
    {
        baseApp = ((BaseApplication)BaseApplication.getAppContext().getApplicationContext());

        _onTracUserId = onTracUserId;
        _onTracFacility = onTracFacility;
        _onTracDeviceId = onTracDeviceId;
    }

    public APIs(Config config, int onTracUserId, String onTracFacility, String deviceId) {
        baseApp = ((BaseApplication)BaseApplication.getAppContext().getApplicationContext());

        _onTracUserId = Integer.toString(onTracUserId);
        _onTracFacility = onTracFacility;
        _onTracDeviceId = deviceId;
    }

    private List<APIsEventListener> listeners = new ArrayList<APIsEventListener>();

    public void addListener(APIsEventListener toAdd) {
        listeners.add(toAdd);
    }

    public Request.Builder AddHeaders(Request.Builder builder)
    {
        return builder
                .header("xontrac_userid", _onTracUserId)
                .addHeader("xontrac_facility", _onTracFacility)
                .addHeader("xontrac_deviceid", _onTracDeviceId)
                .addHeader("xontrac_programversion", _ontracVersion);
    }

    public Request BuildGet(String endpoint)
    {
        Request.Builder builder = new Request.Builder();
        builder.url(endpoint);
        builder = AddHeaders(builder);

        return builder.build();
    }

    public Request BuildPost(String endpoint, PostType postType, String json)
    {
        MediaType contentType = null;

        switch (postType){
            case JSON:
                contentType = MediaType.parse("application/json; charset=utf-8");
                break;
        }

        RequestBody body = RequestBody.create(contentType, json);

        Request.Builder builder = new Request.Builder();
        builder.url(endpoint);
        builder.post(body);
        builder = AddHeaders(builder);

        return builder.build();
    }

    public Request BuildPost(String endpoint)
    {
        Request.Builder builder = new Request.Builder();
        builder.url(endpoint);

        return builder.build();
    }

    public enum PostType {
        JSON
    }

    public CallResult Call(final int type, final Request request, final Object data)
    {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(baseApp.config.ApiConnectTimeout, TimeUnit.SECONDS)
                .writeTimeout(baseApp.config.ApiWriteTimeout, TimeUnit.SECONDS)
                .readTimeout(baseApp.config.ApiReadTimeout, TimeUnit.SECONDS)
                .build();

        final Call call = client.newCall(request);

        Thread t = new Thread(new Runnable() {
            public void run()
            {
                List<APIsEventListener.ClientConnectionError> errors = null;

                try {
                    Response response = call.execute();

                    // raise RequestCompleted event
                    for (APIsEventListener listener : listeners) {
                        listener.RequestCompleted(type, response.code(), response.body().string(), data);
                    }

                    response.body().close();
                    response.close();

                } catch (UnknownHostException e) {
                    errors = new ArrayList<>();
                    errors.add(new APIsEventListener.ClientConnectionError(APIsEventListener.ClientConnectionError.ErrorType.UnknownHost, e.toString(), e.getStackTrace()));
                } catch (SocketTimeoutException e) {
                    errors = new ArrayList<>();
                    errors.add(new APIsEventListener.ClientConnectionError(APIsEventListener.ClientConnectionError.ErrorType.SocketTimeout, e.toString(), e.getStackTrace()));
                } catch (java.net.SocketException e) {
                    //Call was likely canceled so we don't really care about this exception.
                    //result.Errors.add(new WebServicesListener.ClientConnectionError(WebServicesListener.ClientConnectionError.ErrorType.SocketException, e.toString(), e.getStackTrace()));
                } catch (IOException e) {
                    errors = new ArrayList<>();
                    errors.add(new APIsEventListener.ClientConnectionError(APIsEventListener.ClientConnectionError.ErrorType.Unspecified, e.toString(), e.getStackTrace()));
                    Log.wtf("OkHttpClient", "IOException");
                    e.printStackTrace();
                }
                catch(InterruptedException e){
                    Log.wtf("OkHttpClient", "InterruptedException");
                    e.printStackTrace();
                }
                catch (Exception e) {
                    errors = new ArrayList<>();
                    errors.add(new APIsEventListener.ClientConnectionError(APIsEventListener.ClientConnectionError.ErrorType.Unspecified, e.toString(), e.getStackTrace()));
                    Log.wtf("OkHttpClient", "Exception");
                    e.printStackTrace();
                }

                // raise CallCompleted event
                for (APIsEventListener listener : listeners) {
                    listener.CallCompleted(type, data, errors);
                }
            }
        });
        t.start();

        return new CallResult(call, t);
    }

    public static class CallResult {
        public CallResult(okhttp3.Call call, Thread thread){
            this.Call = call;
            this.Thread = thread;
        }

        public Call Call;
        public Thread Thread;
    }

    public Thread CallForScanObjects(final int type, final Request request, final ArrayList<ScanObject> scanObjects)
    {
        /*
        if (!(scanObjects instanceof ArrayList)){
            return Call(type, request, (Object)scanObjects);
        }
        */

        Thread t = new Thread(new Runnable() {
            public void run()
            {
                List<APIsEventListener.ClientConnectionError> errors = null;

                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(baseApp.config.ApiConnectTimeout, TimeUnit.SECONDS)
                        .writeTimeout(baseApp.config.ApiWriteTimeout, TimeUnit.SECONDS)
                        .readTimeout(baseApp.config.ApiReadTimeout, TimeUnit.SECONDS)
                        .build();

                for (ScanObject scan : scanObjects) {
                    try {
                        Request itemRequest = BuildPost(request.url().toString(), PostType.JSON, scan.toJson());

                        Response response = client.newCall(itemRequest).execute();

                        // raise RequestCompleted event
                        for (APIsEventListener listener : listeners) {
                            listener.RequestCompleted(type, response.code(), response.body().string(), scan.ScanId);
                        }

                        response.body().close();
                        response.close();
                    } catch (UnknownHostException e) {
                        errors = new ArrayList<>();
                        errors.add(new APIsEventListener.ClientConnectionError(APIsEventListener.ClientConnectionError.ErrorType.UnknownHost, e.toString(), e.getStackTrace()));
                    } catch (SocketTimeoutException e) {
                        errors = new ArrayList<>();
                        errors.add(new APIsEventListener.ClientConnectionError(APIsEventListener.ClientConnectionError.ErrorType.SocketTimeout, e.toString(), e.getStackTrace()));
                    } catch (java.net.SocketException e) {
                        //Call was likely canceled so we don't really care about this exception.
                        //result.Errors.add(new WebServicesListener.ClientConnectionError(WebServicesListener.ClientConnectionError.ErrorType.SocketException, e.toString(), e.getStackTrace()));
                    } catch (IOException e) {
                        errors = new ArrayList<>();
                        errors.add(new APIsEventListener.ClientConnectionError(APIsEventListener.ClientConnectionError.ErrorType.Unspecified, e.toString(), e.getStackTrace()));
                        Log.wtf("OkHttpClient", "IOException");
                        //e.printStackTrace();
                    }
                    catch(InterruptedException e){
                        Log.wtf("OkHttpClient", "InterruptedException");
                        e.printStackTrace();
                    }
                    catch (Exception e) {
                        errors = new ArrayList<>();
                        errors.add(new APIsEventListener.ClientConnectionError(APIsEventListener.ClientConnectionError.ErrorType.Unspecified, e.toString(), e.getStackTrace()));
                        Log.wtf("OkHttpClient", "Exception");
                        e.printStackTrace();
                    }
                }

                // raise CallCompleted event
                for (APIsEventListener listener : listeners) {
                    listener.CallCompleted(type, null, errors);
                }
            }
        });
        t.start();

        return t;
    }

    public static abstract class Type {
        public static final int Tracking = 1;
        public static final int ZipScheme = 2;
        public static final int UserInfo = 3;
        public static final int Scan = 4;
        public static final int RouteLabel = 5;
        public static final int TrailerStatus = 6;
        public static final int TrailerProcess = 7;
    }

    public static class TrackingObject {
        TrackingObject() {}

        public String DeliveryZip;
        public String DeliveryRoute;
        public String RoutingInfo;
        public String SaturdayDelivery;
        public boolean TrackingNumberFound;

        public static TrackingObject fromJson(String Json)
        {
            Gson gson = new Gson();
            return gson.fromJson(Json, TrackingObject.class);
        }
    }

    public static class ScanObject {
        public ScanObject() {}

        public String ScanId;
        public String StatusCode;
        public String Prompt1;
        public String Prompt2;
        public String Scandatetime;
        public String OnTrac2DBarcode;
//        public String TimeZoneOffset;

        public String toJson()
        {
            Gson gson = new Gson();
            return gson.toJson(this);
        }
    }

    public static class ZipSchemeObject {
        ZipSchemeObject() {}

        public String packageZip;
        public String schemeZip;

        public static ZipSchemeObject[] fromJson(String Json)
        {
            Gson gson = new Gson();
            return gson.fromJson(Json, ZipSchemeObject[].class);
        }
    }

    public static class UserInfoObject {
        UserInfoObject() {}

        public int UserId;
        public String UserName;
        public String Password;
        public String FaciliyCode;

        public static UserInfoObject[] fromJson(String Json)
        {
            Gson gson = new Gson();
            return gson.fromJson(Json, UserInfoObject[].class);
        }
    }

    public static class SortLabelRequestObject {
        public SortLabelRequestObject() {}

        public String Tracking;
        public String DeliveryZip;
        public String OnTrac2DBarcode;

        public String toJson()
        {
            Gson gson = new Gson();
            return gson.toJson(this);
        }
    }

    public static class TrailerStatusRequestObject{
        public TrailerStatusRequestObject() {}
        public String Trailer;
        public String LoadDoor;
        public String ProcessType;
        public String toJson()
        {
            Gson gson = new Gson();
            return gson.toJson(this);
        }
    }


    public static class TrailerProcessRequestObject{
        public TrailerProcessRequestObject(){}
        public String Trailer;
        public String LoadDoor;
        public String TrailerSeal;
        public boolean IsEmpty;
        public String UnloadDoor;
        public String ProcessType;
        public String toJson()
        {
            Gson gson = new Gson();
            return gson.toJson(this);
        }
    }

}

