package com.ontrac.warehouse;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.ActionBar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.TextView;

import com.ontrac.warehouse.Entities.Action;
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

public class SortLabelActivity extends SharedScannerActivity  implements APIsEventListener {
    private static final String Title =  "OnTrac {1} {2} - {0}";

    private UIHelper helper = new UIHelper(this);
    private LabelPrinterHelper labelPrinterHelper = new LabelPrinterHelper(helper);

    private APIs.CallResult _lookupCallResult;
    private ProgressDialog _lookupDialog = null;
    private APIs _apis;

    BaseApplication baseApp = null;
    Action.ScanType scanType = null;

    private HideKeyboard _hidekey;

    @BindView(R.id.uxWebView)
    WebView uxWebView;

    @BindView(R.id.uxTracking)
    EditText uxTracking;

    @BindView(R.id.uxPrinterIP)
    EditText uxPrinterIP;

    boolean _onFirstPrompt = true;

    String _lastScan = "";
    int _totalScanned = 0;

    final Handler _statusHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sort_label);

        baseApp = ((BaseApplication) getApplicationContext());

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

            SpannableString barTitle = new SpannableString(MessageFormat.format(Title, baseApp.authUser.FriendlyName(), buttonText, baseApp.authUser.FacilityCode));
            barTitle.setSpan(new ForegroundColorSpan(buttonForeColor), 0, barTitle.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            actionBar.setTitle(barTitle);
            actionBar.setBackgroundDrawable(new ColorDrawable(buttonBackgroundColor));

            ButterKnife.bind(this);

            CanScan = true;

            _hidekey = new HideKeyboard(this);

            uxWebView.setBackgroundColor(getWindow().getDecorView().getSolidColor());

            uxTracking.requestFocus();

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

    public enum ActionType {
        Scan,
        Keyboard,
        Focus,
    }

    @Override
    public void Scanned(final String data) {
        Process(data, SortLabelActivity.ActionType.Scan);
    }

    @OnLongClick({R.id.uxTracking, R.id.uxPrinterIP})
    public boolean OnLongClick(View view) {
        _hidekey.OnLongClick(view);
        return true; // hides the paste context popup
    }

    @OnFocusChange({R.id.uxTracking, R.id.uxPrinterIP})
    public void OnFocusChange(View view, boolean hasFocus) {
        _hidekey.OnFocusChange(view, hasFocus);
    }

    @OnFocusChange({R.id.uxTracking})
    public void uxTracking_OnFocusChange(View view, boolean hasFocus){
        if (hasFocus) {
            String printerIP = uxPrinterIP.getText().toString();
            boolean allow = false;

            if (!Strings.IsNullOrWhiteSpace(printerIP)) {
                allow = Patterns.IP_ADDRESS.matcher(printerIP).matches();
            }

            _onFirstPrompt = !allow;
            if (!allow) {
                uxPrinterIP.requestFocus();
            }
        }
    }

    @OnEditorAction({R.id.uxPrinterIP})
    public boolean uxPrinterIP_EditorAction(TextView view, int action) {
        if (action == EditorInfo.IME_ACTION_DONE){
            _hidekey.OnEditorAction(view, action);

            String data = uxPrinterIP.getText().toString();
            Process(data, SortLabelActivity.ActionType.Keyboard);
        }
        return false;
    }

    @OnEditorAction({R.id.uxTracking})
    public boolean uxTracking_EditorAction(TextView view, int action) {
        if (action == EditorInfo.IME_ACTION_DONE){
            _hidekey.OnEditorAction(view, action);

            String data = uxTracking.getText().toString();
            Process(data, SortLabelActivity.ActionType.Keyboard);
        }
        return false;
    }

    protected void Process(String data, SortLabelActivity.ActionType actionType)
    {
        if (_onFirstPrompt){
            SetText(uxPrinterIP, data);
            if (Patterns.IP_ADDRESS.matcher(data).matches()){
                _onFirstPrompt = false;
                disableEditText(uxPrinterIP);
            } else {
                baseApp.SharedScanner.pause();
                General.Alert(this, getString(R.string.printerIpError), getString(R.string.notIpAddress), AfterBadDataAlertCallable());
                Notifications.ScanError(baseApp.SharedScanner.getNotificationDevice());
            }
        } else {

//            if (Utilities.ValidateOnTracCode(data)) {
                APIs.SortLabelRequestObject scan = new APIs.SortLabelRequestObject();

                if (Utilities.isValidOnTrac2DBarcode(data))
                {
                    scan.OnTrac2DBarcode = data;
                    String splitArray[] = data.split("\035");
                    data = splitArray[4];
                    scan.DeliveryZip = splitArray[1].substring(2);
                }
                if (Utilities.isValidLaserShip2DBarcode(data))
                {
                    scan.OnTrac2DBarcode = data;
                    String splitArray[] = data.split("\\|");
                    data = splitArray[0];
//                    data = data.substring(0,data.indexOf("|"));
                    scan.DeliveryZip = splitArray[13].substring(0,5);
                }
                if (Utilities.ValidateOnTracCode(data)) {
                    _totalScanned += 1;
                    _lastScan = data;
                    String assetTag = baseApp.onTracDeviceId.GetAssetTag();
                    ScanDataCache.Save("RD", data, "", baseApp.authUser.Id, baseApp.authUser.FacilityCode, assetTag, scan.OnTrac2DBarcode);

                    UpdateAdditionalData();

                    if (Utilities.VerifyTrackingUsps(data)) {
                        data = Utilities.GetTrackingFromUspsTrackingBarcode(data);
                    }
                    scan.Tracking = data;
                    String assetTag2 = baseApp.onTracDeviceId.GetAssetTag();
                    _apis = new APIs(baseApp.config, baseApp.authUser.Id, baseApp.authUser.FacilityCode, assetTag2);
                    _apis.addListener(this);
                    Request request = _apis.BuildPost(baseApp.config.ApiRoutingLabel, APIs.PostType.JSON, scan.toJson());
                    _lookupCallResult = _apis.Call(APIs.Type.RouteLabel, request, scan);
                }
                else{
                    Notifications.ScanError(baseApp.SharedScanner.getNotificationDevice());
                }
  //          } else {
  //              Notifications.ScanError(baseApp.SharedScanner.getNotificationDevice());
  //          }
        }
    }

    protected void UpdateAdditionalData()
    {
        String lastScan = this._lastScan;
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


    protected void SetText(final EditText view, final String data){
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                view.setText(data);
            }
        });
    }


    protected Callable AfterBadDataAlertCallable() {
        return new Callable() {
            public Object call() {
                AfterBadDataAlert();
                return null;
            }
        };
    }

    protected void AfterBadDataAlert()
    {
        baseApp.SharedScanner.resume();
    }

    private void disableEditText(EditText editText) {
        editText.setFocusable(false);
        editText.setEnabled(false);
        editText.setCursorVisible(false);
        editText.setKeyListener(null);
        editText.setBackgroundColor(Color.TRANSPARENT);
    }

    @Override
    public void RequestCompleted(int type, int responseCode, String responseBody, Object data) throws Exception {
        baseApp.SharedScanner.resume();

        switch (type) {
            case APIs.Type.RouteLabel:
                HandleRoutingLabel(responseCode, responseBody);
                break;
            default:
                throw new Exception(getString(R.string.unhandledApiCall));
        }
    }
    public void HandleRoutingLabel(final int responseCode, final String responseBody){
        final Activity activity = this;

        _lookupDialog = null;

        if (responseCode == 200) {
            final String item = responseBody;

            if (item != "null") {
                new Thread(new Runnable() {
                    public void run() {
                        Looper.prepare();
                        labelPrinterHelper.sendFile(item,uxPrinterIP.getText().toString());
                        Looper.loop();
                        Looper.myLooper().quit();
                    }
                }).start();
            }
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
}
