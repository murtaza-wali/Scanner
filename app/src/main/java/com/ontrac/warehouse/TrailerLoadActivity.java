package com.ontrac.warehouse;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.widget.EditText;
import android.widget.TextView;

import com.google.gson.Gson;
import com.ontrac.warehouse.Entities.Action;
import com.ontrac.warehouse.Entities.TrailerStatus;
import com.ontrac.warehouse.Entities.ScanDataCache;
import com.ontrac.warehouse.OnTrac.APIs;
import com.ontrac.warehouse.OnTrac.APIsEventListener;
import com.ontrac.warehouse.OnTrac.Utilities;
import com.ontrac.warehouse.Utilities.LabelPrinterHelper;
import com.ontrac.warehouse.Utilities.Strings;
import com.ontrac.warehouse.Utilities.UX.General;
import com.ontrac.warehouse.Utilities.UX.HideKeyboard;
import com.ontrac.warehouse.Utilities.Zebra.Notifications;
import com.ontrac.warehouse.Utilities.Zebra.SharedScannerActivity;
import com.ontrac.warehouse.Utilities.Zebra.UIHelper;

import org.joda.time.DateTime;
import org.joda.time.Period;

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
import static j2html.TagCreator.body;
import static j2html.TagCreator.div;
import static j2html.TagCreator.span;

public class TrailerLoadActivity extends SharedScannerActivity implements APIsEventListener {
    private static final String Title =  " {1} - {0}";

    private UIHelper helper = new UIHelper(this);
    private LabelPrinterHelper labelPrinterHelper = new LabelPrinterHelper(helper);

    private APIs.CallResult _lookupCallResult;
    private ProgressDialog _lookupDialog = null;
    private APIs _apis;

    BaseApplication baseApp = null;
    Action.ScanType scanType = null;

    private HideKeyboard _hidekey;

    @BindView(R.id.uxTrailer)
    EditText uxTrailer;

    @BindView(R.id.uxDoor)
    EditText uxDoor;

    @BindView(R.id.uxTracking)
    EditText uxTracking;

    @BindView(R.id.uxWebView)
    WebView uxWebView;

    String currentItem = "uxTrailer";
    String trailer = "";
    String door = "";
    String _tracking = "";
    DateTime lastScanDateTime = null;
    int _totalScanned = 0;
    int _trailerInstanceId = 0;
    final Handler _statusHandler = new Handler();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trailer_load);

        baseApp = ((BaseApplication)getApplicationContext());

        boolean changingOrientation = baseApp.setOrientation(this);

        if (!changingOrientation) {
            ValidateUser(this, baseApp.authUser);

            /* From Intent */
            Intent source = getIntent();
            scanType = (Action.ScanType) source.getSerializableExtra("ScanType");
            String buttonText = source.getStringExtra("ButtonText");
            int buttonForeColor = source.getIntExtra("ButtonForeColor", -1);
            int buttonBackgroundColor = source.getIntExtra("ButtonBackgroundColor", -1);

            ActionBar actionBar = getSupportActionBar();

            SpannableString barTitle = new SpannableString(MessageFormat.format(Title, baseApp.authUser.FriendlyName(), baseApp.authUser.FacilityCode));
            barTitle.setSpan(new ForegroundColorSpan(buttonForeColor), 0, barTitle.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            actionBar.setTitle(getString(R.string.loadTrailer) + barTitle);
            actionBar.setBackgroundDrawable(new ColorDrawable(buttonBackgroundColor));

            ButterKnife.bind(this);

            CanScan = true;

            _hidekey = new HideKeyboard(this);

            uxWebView.setBackgroundColor(getWindow().getDecorView().getSolidColor());

            uxTrailer.requestFocus();
            uxDoor.setEnabled(false);
            uxTracking.setEnabled(false);

            final int interval = 2000;
            _statusHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    UpdateAdditionalData();
                    _statusHandler.postDelayed(this, interval);
                }
            }, 0);
        }
    }

    @Override
    protected void onStart(){
        super.onStart();
    }

    @Override
    public void Scanned(final String data) {
        Process(data);
    }

    @OnFocusChange({R.id.uxTrailer, R.id.uxDoor, R.id.uxTracking})
    public void OnFocusChange(View view, boolean hasFocus) {
        _hidekey.OnFocusChange(view, hasFocus);
    }

    @OnLongClick({R.id.uxTrailer, R.id.uxDoor, R.id.uxTracking})
    public boolean OnLongClick(View view) {
        _hidekey.OnLongClick(view);
        return true; // hides the paste context popup
    }

    @OnEditorAction({R.id.uxTrailer, R.id.uxDoor, R.id.uxTracking})
    public boolean OnEditorAction(TextView view, int action) {
        switch(view.getId()) {
            case R.id.uxTrailer : {
                Process(uxTrailer.getText().toString());
                break;
            }
            case R.id.uxDoor : {
                Process(uxDoor.getText().toString());
                break;
            }
            case R.id.uxTracking : {
                Process(uxTracking.getText().toString());
                break;
            }
        }
        _hidekey.OnEditorAction(view, action);
        return false;
    }

    protected void Process(String data)
    {
        if (lastScanDateTime != null) {
            DateTime now = DateTime.now();
            Period period = new Period(lastScanDateTime, now);
            int test = period.getMinutes();
            if (period.getMinutes() > 15) {
                Callable doAfter = new Callable() {
                    public Object call() {
                        AfterErrorAlert();
                        return null;
                    }
                };
                baseApp.SharedScanner.pause();
                General.Alert(this, getString(R.string.trailerLoadError), getString(R.string.trailerLoadTimeout), doAfter);
            }
        }
        lastScanDateTime = DateTime.now();
        boolean doContinue = true;

/*
        Callable doAfter = new Callable() {
            public Object call() {
                AfterBadDataAlert();
                return null;
            }
        };
*/

        if (!Strings.IsNullOrWhiteSpace(data) && doContinue && (currentItem == "uxTrailer")){
            if (Utilities.isValidTrailerBarcode(data)) {
                GoodData(data, "");
            } else {
                BadData(data);
            }
        }
        else if (!Strings.IsNullOrWhiteSpace(data) && doContinue && (currentItem == "uxDoor")) {
            if (Utilities.isValidDoorBarcode(data)) {
                GoodData(data, "");
            } else {
                BadData(data);
            }
        }
        else if (!Strings.IsNullOrWhiteSpace(data) && doContinue && (currentItem == "uxTracking")) {
            String onTrac2DBarcode = "";
            if (!Strings.IsNullOrWhiteSpace(data)){
                if (Utilities.isValidOnTrac2DBarcode(data)) {
                    onTrac2DBarcode = data;
                    String splitArray[] = data.split("\035");
                    data = splitArray[4];
                }
                if (Utilities.isValidLaserShip2DBarcode(data))
                {
                    onTrac2DBarcode = data;
                    data = data.substring(0,data.indexOf("|"));
                }
                if (Utilities.ValidateOnTracCode(data)) {
                    GoodData(data, onTrac2DBarcode);
                } else {
                    BadData(data);
                }
            }
        }
    }
    protected void GoodData(final String data, String onTrac2DBarcode){

        switch(currentItem){
            case "uxTrailer" :{
                this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        uxTrailer.setText(data);
                        uxDoor.setText("");
                        uxTracking.setText("");
                        uxTrailer.setEnabled(false);
                        uxDoor.setEnabled(true);
                        uxTracking.setEnabled(false);
                        uxDoor.requestFocus();
                    }
                });
                currentItem = "uxDoor";
                trailer = data;
                break;
            }
            case "uxDoor" :{
                this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        uxDoor.setText(data);
                        uxTrailer.setEnabled(false);
                        uxDoor.setEnabled(false);
                        startLookupDialog(getString(R.string.searching), getString(R.string.checkingTraierStatus));
                    }
                });
                currentItem = "uxTracking";
                door = data;
                APIs.TrailerStatusRequestObject trailerStatusRequest = new APIs.TrailerStatusRequestObject();
                trailerStatusRequest.Trailer = trailer;
                trailerStatusRequest.LoadDoor = door;
                trailerStatusRequest.ProcessType = "LOAD";
                CheckIfTrailerIsOpen(trailerStatusRequest);
                break;
            }

            case "uxTracking" : {
                _totalScanned += 1;
                _tracking = data;
                this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        uxTracking.setText("");
                    }
                });
                String assetTag = baseApp.onTracDeviceId.GetAssetTag();
                ScanDataCache.Save("TL", data, trailer+','+door+','+_trailerInstanceId, baseApp.authUser.Id, baseApp.authUser.FacilityCode, assetTag, onTrac2DBarcode);
                UpdateAdditionalData();
                break;
            }
        }
    }

    protected void CheckIfTrailerIsOpen(APIs.TrailerStatusRequestObject trailerStatusRequest)
    {
        baseApp.SharedScanner.pause();

        Callable doAfter = new Callable() {
            public Object call() {
                AfterBadDataAlert();
                return null;
            }
        };
        String assetTag = baseApp.onTracDeviceId.GetAssetTag();
        _apis = new APIs(baseApp.config, baseApp.authUser.Id, baseApp.authUser.FacilityCode, assetTag);
        _apis.addListener(this);
        Request request = _apis.BuildPost(baseApp.config.ApiTrailerStatus, APIs.PostType.JSON, trailerStatusRequest.toJson());
        _lookupCallResult = _apis.Call(APIs.Type.TrailerStatus, request, trailerStatusRequest);
    }

/*
    protected void CloseTrailer(APIs.TrailerCloseRequestObject trailerCloseRequest)
    {
        baseApp.SharedScanner.pause();
        Callable doAfter = new Callable() {
            public Object call() {
                AfterBadDataAlert();
                return null;
            }
        };
        _apis = new APIs(baseApp.config, baseApp.authUser.Id, baseApp.authUser.FacilityCode, baseApp.DeviceSerial);
        _apis.addListener(this);
        Request request = _apis.BuildPost(baseApp.config.ApiTrailerClose, APIs.PostType.JSON, trailerCloseRequest.toJson());
        _lookupCallResult = _apis.Call(APIs.Type.TrailerClose, request, trailerCloseRequest);
    }
*/


    protected void BadData(final String data){
        baseApp.SharedScanner.pause();
        Callable doAfter = new Callable() {
            public Object call() {
                AfterBadDataAlert();
                return null;
            }
        };
        switch(currentItem){
            case "uxTrailer" : {
                this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        uxTrailer.setText(data);
                    }
                });
                General.Alert(this, getString(R.string.trailerBarcodeError), getString(R.string.notValidBarcode), doAfter);
                break;
            }
            case "uxDoor" : {
                this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        uxDoor.setText(data);
                    }
                });
                General.Alert(this, getString(R.string.doorBarcodeError), getString(R.string.notValidDoor), doAfter);
                break;
            }
            case "uxTracking" : {
                baseApp.SharedScanner.resume();
                UpdateAdditionalData();
                break;
            }
        }
        Notifications.ScanError(baseApp.SharedScanner.getNotificationDevice());
    }

    protected void AfterBadDataAlert()
    {
        baseApp.SharedScanner.resume();
    }

    @Override
    public void RequestCompleted(int type, final int responseCode, final String responseBody, Object data) throws Exception {
        baseApp.SharedScanner.resume();

        final Activity activity = this;

        switch (type) {
            case APIs.Type.TrailerStatus:
                if (responseCode == 200)
                {
                    TrailerStatus trailerStatus = new Gson().fromJson(responseBody, TrailerStatus.class) ;
                    if (trailerStatus.Success) {
                        _trailerInstanceId = trailerStatus.TrailerInstanceId;
                        this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                uxTracking.setEnabled(true);
                                uxTracking.requestFocus();
                            }
                        });
                    }
                    else
                        General.Alert(activity, getString(R.string.loadError), trailerStatus.ErrorMessage, AfterSuccessAlertCallable());
                }
                else
                    this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            General.Alert(activity, getString(R.string.apiFailure), responseCode + "\r" + responseBody, AfterSuccessAlertCallable());
                        }
                    });
                break;
            default:
                throw new Exception(getString(R.string.unhandledApiCall));
        }
    }

    protected Callable AfterSuccessAlertCallable() {
        return new Callable() {
            public Object call() {
                AfterSuccessAlert();
                return null;
            }

            ;
        };
    }

    protected void AfterSuccessAlert()
    {
        baseApp.SharedScanner.resume();
        this.finish();
    }

    @Override
    public void CallCompleted(int type, Object data, List<ClientConnectionError> errors) {

        if (_lookupDialog != null && _lookupDialog.isShowing()) {
            _lookupDialog.cancel();
        }

        if (errors != null && errors.size() > 0) {
            baseApp.SharedScanner.pause();

            Callable doAfterErrorAlert = new Callable() {
                public Object call() {
                    AfterErrorAlert();
                    return null;
                }
            };

            ClientConnectionError error = errors.get(0);

            switch (error.Type){
                case UnknownHost:
                    General.Alert(this, "Unknown Host Error", "Check the device's internet connectivity and retry. Consult with your system admin if the problem persists.", doAfterErrorAlert);
                    break;
                case SocketTimeout:
                    General.Alert(this, "Socket Timeout Error", "Check the device's internet connectivity and retry. Consult with your system admin if the problem persists.", doAfterErrorAlert);
                    break;
                default:
                    General.Alert(this, "Connection Error", "Check the device's internet connectivity and retry.", doAfterErrorAlert);
                    break;
            }
        }
    }

    private void AfterErrorAlert(){
        baseApp.SharedScanner.resume();
        this.finish();
    }

    protected void startLookupDialog(String title, String message) {
        baseApp.SharedScanner.pause();

        if (_lookupDialog == null || !_lookupDialog.isShowing()) {
            _lookupDialog = new ProgressDialog(this);
            _lookupDialog.setTitle(title);
            _lookupDialog.setMessage(message);
            _lookupDialog.setCancelable(false);
            _lookupDialog.show();
        }
    }

    protected void UpdateAdditionalData()
    {
        String lastScan = this._tracking;
        String cached = String.valueOf(ScanDataCache.Count());
        String totalScanned = String.valueOf(_totalScanned);
        String html = body().attr("style", "font-size: 1em; padding: 0px 0px 0px 0px; margin: 0px 0px 0px 0px;").with(
                div().with(span(getString(R.string.labelLastScan))).with(span(lastScan).attr("style", "font-weight: bold;")),
                div().with(span(cached).attr("style", "font-weight: bold;")).with(span(getString(R.string.labelRecordsCached))),
                div().with(span(totalScanned).attr("style", "font-weight: bold;")).with(span(getString(R.string.labelTotalItems)))
        ).toString();
        SetStatus(html);
    }

    protected void SetStatus(final String html){
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                uxWebView.getSettings().setAppCacheEnabled(false);
                uxWebView.getSettings().setJavaScriptEnabled(false);
                uxWebView.getSettings().setDomStorageEnabled(false);
                uxWebView.getSettings().setDatabaseEnabled(false);
                uxWebView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
                uxWebView.getSettings().setSupportZoom(false);

                uxWebView.loadData(html, "text/html; charset=utf-8", "UTF-8");
            }
        });
    }

}