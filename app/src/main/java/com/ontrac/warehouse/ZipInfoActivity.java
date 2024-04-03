package com.ontrac.warehouse;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.TextView;

import com.ontrac.warehouse.OnTrac.APIs;
import com.ontrac.warehouse.OnTrac.APIsEventListener;
import com.ontrac.warehouse.OnTrac.Utilities;
import com.ontrac.warehouse.Utilities.Strings;
import com.ontrac.warehouse.Utilities.UX.General;
import com.ontrac.warehouse.Utilities.UX.HideKeyboard;
import com.ontrac.warehouse.Utilities.Zebra.Notifications;
import com.ontrac.warehouse.Utilities.Zebra.SharedScannerActivity;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.Callable;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnEditorAction;
import butterknife.OnFocusChange;
import butterknife.OnLongClick;
import okhttp3.Request;

import static com.ontrac.warehouse.OnTrac.Utilities.ValidateUser;

public class ZipInfoActivity extends SharedScannerActivity implements APIsEventListener {
    private final static String TAG = ZipInfoActivity.class.getName();
    private static final String Title =  " {1} - {0}";
    BaseApplication baseApp = null;

    private APIs.CallResult _lookupCallResult;
    private ProgressDialog _lookupDialog = null;
    private ProgressDialog _alertDialog = null;

    private APIs _apis;
    private HideKeyboard _hidekey;

    @BindView(R.id.uxCode)
    EditText uxCode;

    @BindView(R.id.uxWebView)
    WebView uxWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zip_info);
        baseApp = ((BaseApplication)getApplicationContext());

        boolean changingOrientation = baseApp.setOrientation(this);

        if (!changingOrientation) {
            ValidateUser(this, baseApp.authUser);

            getSupportActionBar().setTitle(MessageFormat.format(getString(R.string.packageZipLookup)+ Title, baseApp.authUser.FriendlyName(), baseApp.authUser.FacilityCode));

            ButterKnife.bind(this);

            CanScan = true;

            _hidekey = new HideKeyboard(this);

            uxWebView.setBackgroundColor(getWindow().getDecorView().getSolidColor());
        }
    }

    // called when scanner returns scan data
    @Override
    public void Scanned(final String data) {
        this.runOnUiThread(new Runnable() {
            String newData = data;
            @Override
            public void run() {
                SetStatus("");
                if (Utilities.isValidOnTrac2DBarcode(newData)) {
                    String splitArray[] = newData.split("\035");
                    newData = splitArray[4];
                }
                if (Utilities.isValidLaserShip2DBarcode(data))
                {
                    newData = data.substring(0,data.indexOf("|"));
                }
                uxCode.setText(newData);
                LookupCode(newData);
            }
        });
    }

    @OnFocusChange({R.id.uxCode})
    public void OnFocusChange(View view, boolean hasFocus) {
        _hidekey.OnFocusChange(view, hasFocus);
    }

    @OnLongClick({R.id.uxCode})
    public boolean OnLongClick(View view) {
        _hidekey.OnLongClick(view);
        return true; // hides the paste context popup
    }

    @OnEditorAction({R.id.uxCode})
    public boolean OnEditorAction(TextView view, int action) {
        LookupCode(uxCode.getText().toString());

        _hidekey.OnEditorAction(view, action);
        return false;
    }


    protected void SetStatus(final String html){

        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                uxWebView.loadData(html, "text/html; charset=utf-8", "UTF-8");
            }
        });
    }

    protected void SetStatus(APIs.TrackingObject item){
        String html = "";

        if (!item.TrackingNumberFound)
        {
            html = MessageFormat.format("<span style=\"color:red;\">{0}</span>", "Not Found.");
        } else {
            if (!Strings.IsNullOrWhiteSpace(item.RoutingInfo))
                html += MessageFormat.format("<span style=\"color:red;\"><h1><b>{0}</b></h1></span>", item.RoutingInfo);
            html += MessageFormat.format("<p>Delivery Route: {0}</p><p>Delivery Zip: {1}</p>", item.DeliveryRoute, item.DeliveryZip);
            if (!Strings.IsNullOrWhiteSpace(item.SaturdayDelivery))
                html += MessageFormat.format("<span style=\"color:red;\"><h><b>{0}</b></h></span>", item.SaturdayDelivery);
        }

        SetStatus(html);
    }

    protected void LookupCode(String data) {
        CancelLookup();

        if (Utilities.ValidateOnTracCode(data)){

            if (Utilities.VerifyTrackingUsps(data))
            {
                data = Utilities.GetTrackingFromUspsTrackingBarcode(data);
            }

            startLookupDialog(getString(R.string.searching), getString(R.string.performingZipLookup));
            String assetTag = baseApp.onTracDeviceId.GetAssetTag();
            _apis = new APIs(baseApp.config, baseApp.authUser.Id, baseApp.authUser.FacilityCode, assetTag);
            _apis.addListener(this);
            String endPoint = null;

            try {
                endPoint = MessageFormat.format(baseApp.config.ApiZipInfo, URLEncoder.encode(data,"UTF-8"));
//                endPoint = MessageFormat.format(baseApp.config.ApiZipInfo, URLEncoder.encode(data,"UTF-8"), baseApp.authUser.Id);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            Request request = _apis.BuildGet(endPoint);
            _lookupCallResult = _apis.Call(APIs.Type.Tracking, request, null);
        } else {
            // RJD - 3/2/21 - ZeroInteractionBadScans
            Notifications.ScanError(baseApp.SharedScanner.getNotificationDevice());
            String html = MessageFormat.format("<span style=\"color:red;\">{0}</span>", "Invalid barcode.");
            SetStatus(html);
        }
    }

    protected void startLookupDialog(String title, String message) {
        baseApp.SharedScanner.pause();

        if (_lookupDialog == null || !_lookupDialog.isShowing()) {
            _lookupDialog = new ProgressDialog(this);
            _lookupDialog.setTitle(title);
            _lookupDialog.setMessage(message);
            _lookupDialog.setCancelable(false);
            _lookupDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    CancelLookup();
                }
            });
            _lookupDialog.show();
        }
    }

    private void CancelLookup(){
        if (_lookupCallResult != null) {
            if (_lookupCallResult.Call != null) {
                _lookupCallResult.Call.cancel();
            }

            if (_lookupCallResult.Thread != null) {
                _lookupCallResult.Thread.interrupt();
                _lookupCallResult.Thread = null;
            }
        }

        if (_lookupDialog != null) {
            //_syncDialog.isShowing()
            _lookupDialog.dismiss();
            _lookupDialog = null;
        }

        baseApp.SharedScanner.resume();
    }

    // called when an API request completes
    @Override
    public void RequestCompleted(int type, int responseCode, String responseBody, Object data) throws Exception {
        baseApp.SharedScanner.resume();

        switch (type) {
            case APIs.Type.Tracking:
                HandleZipInfo(responseCode, responseBody);
                break;
            default:
                throw new Exception(getString(R.string.unhandledApiCall));
        }
    }

    @Override
    public void CallCompleted(int type, Object data, List<ClientConnectionError> errors) {
        if (errors != null && errors.size() > 0) {
            baseApp.SharedScanner.pause();

            if (_lookupDialog != null && _lookupDialog.isShowing()) {
                _lookupDialog.cancel();
            }

            Callable doAfterErrorAlert = new Callable() {
                public Object call() {
                    AfterErrorAlert();
                    return null;
                }
            };

            ClientConnectionError error = errors.get(0);

            switch (error.Type){
                case UnknownHost:
                    General.Alert(this, getString(R.string.unknownHostError), getString(R.string.checkInternetConnection), doAfterErrorAlert);
                    break;
                case SocketTimeout:
                    General.Alert(this, getString(R.string.socketTimeoutError), getString(R.string.checkInternetConnection), doAfterErrorAlert);
                    break;
                default:
                    General.Alert(this, getString(R.string.connectionError), getString(R.string.checkInternetConnectivity), doAfterErrorAlert);
                    break;
            }
        }
    }

    private void AfterErrorAlert(){
        baseApp.SharedScanner.resume();
    }

    public void HandleZipInfo(final int responseCode, final String responseBody){
        final Activity activity = this;

        _lookupDialog.dismiss();
        _lookupDialog = null;

        if (responseCode == 200) {
            APIs.TrackingObject item = APIs.TrackingObject.fromJson(responseBody);

            SetStatus(item);
        }
        else {
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    General.Alert(activity, getString(R.string.apiFailure), responseCode + "\r" + responseBody);
                }
            });
        }
    }

    private Menu uxMenu;
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.zipinfo, menu);
        uxMenu = menu;

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.uxRefreshScanner:
//                if (!baseApp.SharedScanner.isScannerConnected()) {
                    Restart();
//                }
                return true;
            case R.id.uxClear:
                uxCode.setText("");
                SetStatus("");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}