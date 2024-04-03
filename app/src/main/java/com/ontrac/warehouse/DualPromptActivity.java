package com.ontrac.warehouse;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.ActionBar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.TextView;

import com.ontrac.warehouse.Entities.Action;
import com.ontrac.warehouse.Entities.ScanDataCache;
import com.ontrac.warehouse.OnTrac.Utilities;
import com.ontrac.warehouse.Utilities.Strings;
import com.ontrac.warehouse.Utilities.UX.General;
import com.ontrac.warehouse.Utilities.UX.HideKeyboard;
import com.ontrac.warehouse.Utilities.Zebra.Notifications;
import com.ontrac.warehouse.Utilities.Zebra.SharedScannerActivity;

import java.text.MessageFormat;
import java.util.concurrent.Callable;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnEditorAction;
import butterknife.OnFocusChange;
import butterknife.OnLongClick;

import static com.ontrac.warehouse.OnTrac.Utilities.CheckStaplesCustomerReferenceNumber;
import static com.ontrac.warehouse.OnTrac.Utilities.ValidatePs;
import static com.ontrac.warehouse.OnTrac.Utilities.ValidateUser;
import static com.ontrac.warehouse.OnTrac.Utilities.VerifyTrackingUsps;
import static com.ontrac.warehouse.OnTrac.Utilities.VerifyTrackingUspsTrayLabel;
import static j2html.TagCreator.body;
import static j2html.TagCreator.div;
import static j2html.TagCreator.span;

public class DualPromptActivity extends SharedScannerActivity {
    private final static String TAG = DualPromptActivity.class.getName();
    private static final String Title =  "OnTrac {1} {2} - {0}";
    BaseApplication baseApp = null;

    Action.ScanType scanType = null;

    private HideKeyboard _hidekey;

    @BindView(R.id.uxCode1)
    EditText uxCode1;

    @BindView(R.id.uxCode1Wrapper)
    TextInputLayout uxCode1Wrapper;

    @BindView(R.id.uxCode2)
    EditText uxCode2;

    @BindView(R.id.uxCode2Wrapper)
    TextInputLayout uxCode2Wrapper;

    @BindView(R.id.uxWebView)
    WebView uxWebView;

    String _lastScan = "";
    int _totalScanned = 0;
    boolean _onFirstPrompt = true;
    final Handler _statusHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dual_prompt);

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

            SpannableString barTitle = new SpannableString(MessageFormat.format(Title, baseApp.authUser.FriendlyName(), scanType.toString(), baseApp.authUser.FacilityCode));
            barTitle.setSpan(new ForegroundColorSpan(buttonForeColor), 0, barTitle.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            actionBar.setTitle(barTitle);
            actionBar.setBackgroundDrawable(new ColorDrawable(buttonBackgroundColor));

            ButterKnife.bind(this);

            CanScan = true;

            _hidekey = new HideKeyboard(this);

            uxWebView.setBackgroundColor(getWindow().getDecorView().getSolidColor());

            switch (scanType) {
                case Staples:
                    uxCode1.setHint(getString(R.string.scanCustomerRef));
                    break;
                case PS:
                    uxCode1.setHint(getString(R.string.scanPosBarcode));
                    break;
                case UspsOS:
                    uxCode1.setHint(getString(R.string.scanTrayLabel));
                    break;
                case UspsRD:
                    uxCode1.setHint(getString(R.string.scanTrayLabel));
                    break;
                case TrailerLoad:
                    uxCode1.setHint(getString(R.string.trailerWrapper));
                    break;
            }
            uxCode1Wrapper.setHint(uxCode1.getHint());

            uxCode1.setImeOptions(EditorInfo.IME_ACTION_DONE);
            uxCode2.setImeOptions(EditorInfo.IME_ACTION_DONE);

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
        Process(data, ActionType.Scan);
    }

    @OnLongClick({R.id.uxCode1, R.id.uxCode2})
    public boolean OnLongClick(View view) {
        _hidekey.OnLongClick(view);
        return true; // hides the paste context popup
    }

    @OnFocusChange({R.id.uxCode1, R.id.uxCode2})
    public void OnFocusChange(View view, boolean hasFocus) {
        _hidekey.OnFocusChange(view, hasFocus);
    }

/*
    @OnFocusChange({R.id.uxCode1})
    public void uxCode1_OnFocusChange(View view, boolean hasFocus){
        if (hasFocus) {
            if ((!_onFirstPrompt) && (scanType == Action.ScanType.TrailerLoad))
                view.clearFocus();
                uxCode2.requestFocus();
        }
    }
*/
    @OnFocusChange({R.id.uxCode2})
    public void uxCode2_OnFocusChange(View view, boolean hasFocus){
        if (hasFocus) {
            String code1 = uxCode1.getText().toString();
            boolean allow = false;

            //_onFirstPrompt = true;
            //Process(code1, ActionType.Focus);

            if (!Strings.IsNullOrWhiteSpace(code1)) {
                switch (scanType) {
                    case Staples:
                        allow = CheckStaplesCustomerReferenceNumber(code1);
                        break;
                    case PS:
                        allow = ValidatePs(code1);
                        break;
                    case UspsOS:
                        allow = VerifyTrackingUspsTrayLabel(code1);
                        break;
                    case UspsRD:
                        allow = VerifyTrackingUspsTrayLabel(code1);
                        break;
                    case TrailerLoad:
                        allow = true;
                        break;
                }
            }

            _onFirstPrompt = !allow;
            if (!allow) {
                view.clearFocus();
            }

            //Process(code1, ActionType.Focus);
        }
    }

    @OnEditorAction({R.id.uxCode1})
    public boolean uxCode1_EditorAction(TextView view, int action, KeyEvent key) {
        if (action == EditorInfo.IME_ACTION_DONE  || (key.getKeyCode() == KeyEvent.KEYCODE_ENTER)){
            _hidekey.OnEditorAction(view, action);
            String data = uxCode1.getText().toString();
            Process(data, ActionType.Keyboard);
        }
        return false;
    }

    @OnEditorAction({R.id.uxCode2})
    public boolean uxCode2_EditorAction(TextView view, int action) {
        if (action == EditorInfo.IME_ACTION_DONE){
            _hidekey.OnEditorAction(view, action);

            String data = uxCode2.getText().toString();
            Process(data, ActionType.Keyboard);
        }
        return false;
    }

    protected void Process(String data, ActionType actionType)
    {
        if (Strings.IsNullOrWhiteSpace(data)){
            return;
        }

        switch (scanType){
            case Staples:
                ProcessStaples(data, actionType);
                break;
            case PS:
                ProcessPs(data, actionType);
                break;
            case UspsOS:
                ProcessUsps(data, actionType);
                break;
            case UspsRD:
                ProcessUsps(data, actionType);
                break;
            case TrailerLoad:
                ProcessTrailerLoad(data, actionType);
                break;
        }
    }

    protected void ProcessStaples(String data, ActionType actionType)
    {
        if (_onFirstPrompt){
            SetText(uxCode1, data);
            if (CheckStaplesCustomerReferenceNumber(data)){
                _onFirstPrompt = false;
            } else {
                baseApp.SharedScanner.pause();
                General.Alert(this, getString(R.string.barcodeError), getString(R.string.refNumInvalid), AfterBadDataAlertCallable());
                Notifications.ScanError(baseApp.SharedScanner.getNotificationDevice());
            }
        } else {
            if (Utilities.isValidOnTracTracking(data)) {
                _totalScanned += 1;
                _lastScan = data;

                String promptOneValue = uxCode1.getText().toString();

                SetText(uxCode1, "");
                SetText(uxCode2, "");
                _onFirstPrompt = true;

                String statusCode = Action.GetCode(scanType);
                String assetTag = baseApp.onTracDeviceId.GetAssetTag();
                ScanDataCache.Save(statusCode, promptOneValue, data, baseApp.authUser.Id, baseApp.authUser.FacilityCode, assetTag,"");

                UpdateAdditionalData();
            } else {
                baseApp.SharedScanner.pause();
                if (actionType == ActionType.Scan || actionType == ActionType.Keyboard){
                    SetText(uxCode2, data);
                }
                General.Alert(this, getString(R.string.barcodeError), getString(R.string.barcodeInvalidated), AfterBadDataAlertCallable());
                Notifications.ScanError(baseApp.SharedScanner.getNotificationDevice());
            }
        }
    }

    protected void ProcessPs(String data, ActionType actionType)
    {
        if (_onFirstPrompt){
            SetText(uxCode1, data);
            if (ValidatePs(data)){
                _onFirstPrompt = false;
            } else {
                baseApp.SharedScanner.pause();
                General.Alert(this, getString(R.string.barcodeError), getString(R.string.psCodeInvalid), AfterBadDataAlertCallable());
                Notifications.ScanError(baseApp.SharedScanner.getNotificationDevice());
            }
        } else {
            if (ValidatePs(data)){
                SetText(uxCode1, "");
                SetText(uxCode2, "");
                _onFirstPrompt = true;
                ProcessPs(data, actionType);
                return;
            }
            String onTrac2DBarcode = "";
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
                _totalScanned += 1;
                _lastScan = data;

                String promptOneValue = uxCode1.getText().toString();

                SetText(uxCode2, "");

                String statusCode = Action.GetCode(scanType);
                String assetTag = baseApp.onTracDeviceId.GetAssetTag();
                ScanDataCache.Save(statusCode, promptOneValue, data, baseApp.authUser.Id, baseApp.authUser.FacilityCode, assetTag, onTrac2DBarcode);

                UpdateAdditionalData();
            } else {
                baseApp.SharedScanner.pause();
                if (actionType == ActionType.Scan || actionType == ActionType.Keyboard){
                    SetText(uxCode2, data);
                }
                General.Alert(this, getString(R.string.barcodeError), getString(R.string.barcodeInvalidated), AfterBadDataAlertCallable());
                Notifications.ScanError(baseApp.SharedScanner.getNotificationDevice());
            }
        }
    }

    protected void ProcessUsps(String data, ActionType actionType)
    {
        if (_onFirstPrompt){
            SetText(uxCode1, data);
            if (VerifyTrackingUspsTrayLabel(data)){
                _onFirstPrompt = false;
            } else {
                baseApp.SharedScanner.pause();
                General.Alert(this, getString(R.string.barcodeError), getString(R.string.trayLabelInvalid), AfterBadDataAlertCallable());
                Notifications.ScanError(baseApp.SharedScanner.getNotificationDevice());
            }
        } else {
            if (VerifyTrackingUspsTrayLabel(data)){
                SetText(uxCode1, "");
                SetText(uxCode2, "");
                _onFirstPrompt = true;
                ProcessUsps(data, actionType);
                return;
            }

            if (VerifyTrackingUsps(data)) {
                String trayCode = uxCode1.getText().toString();

                String zipOfPackage = Utilities.GetZipFromUspsTrackingBarcode(data);
                String trackingOfPackage = Utilities.GetTrackingFromUspsTrackingBarcode(data);
                String zipOfTray = Utilities.GetZipFromUspsTray(trayCode);

                Utilities.CheckUspsConsolidationResult consolidatonResult = Utilities.CheckUspsConsolidation(trayCode, trackingOfPackage, zipOfTray, zipOfPackage);

                if (consolidatonResult == Utilities.CheckUspsConsolidationResult.None)
                {
                    _totalScanned += 1;
                    _lastScan = data;

                    SetText(uxCode2, "");

                    String statusCode = Action.GetCode(scanType);
                    String assetTag = baseApp.onTracDeviceId.GetAssetTag();
                    ScanDataCache.Save(statusCode, trayCode, data, baseApp.authUser.Id, baseApp.authUser.FacilityCode, assetTag, "");

                    UpdateAdditionalData();
                } else {
                    String errorMessage = "";
                    switch (consolidatonResult){
                        case BagForFirstClassParcelsOnly:
                            errorMessage = getString(R.string.firstClassOnly);
                            break;
                        case FirstClassParcelsAndBelongsInFirstClassBag:
                            errorMessage = getString(R.string.belongsFirstClass);
                            break;
                        case BagForOutOfFootprintParcelsOnly:
                            errorMessage = getString(R.string.footprintParcelOnly);
                            break;
                        case OutOfFootprintParcelsAndBelongsInOutOfFootprintBag:
                            errorMessage = getString(R.string.belongsOutofFootprint);
                            break;
                        case ParcelZipDoesNotBelongInBag:
                            errorMessage = getString(R.string.zipDoesNotBelong);
                            break;
                        case ZipLookupException:
                            errorMessage = getString(R.string.zipNotFound);
                            break;
                    }

                    baseApp.SharedScanner.pause();
                    SetText(uxCode2, data);
                    General.Alert(this, getString(R.string.barcodeError), errorMessage, AfterBadDataAlertCallable());
                    Notifications.ScanError(baseApp.SharedScanner.getNotificationDevice());
                }
            } else {
                baseApp.SharedScanner.pause();
                if (actionType == ActionType.Scan || actionType == ActionType.Keyboard){
                    SetText(uxCode2, data);
                }
                General.Alert(this, getString(R.string.barcodeError), getString(R.string.barcodeInvalidated), AfterBadDataAlertCallable());
                Notifications.ScanError(baseApp.SharedScanner.getNotificationDevice());
            }
        }
    }

    protected void ProcessTrailerLoad(String data, ActionType actionType)
    {
        if (_onFirstPrompt){
            SetText(uxCode1, data);
            _onFirstPrompt = false;
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    uxCode1.setEnabled(false);
                    uxCode2.requestFocus();
                }
            });
        } else {
            String onTrac2DBarcode = "";
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
                _totalScanned += 1;
                _lastScan = data;
                String promptOneValue = uxCode1.getText().toString();
                SetText(uxCode2, "");
                String statusCode = Action.GetCode(scanType);
                String assetTag = baseApp.onTracDeviceId.GetAssetTag();
                ScanDataCache.Save(statusCode, promptOneValue, data, baseApp.authUser.Id, baseApp.authUser.FacilityCode, assetTag, onTrac2DBarcode);

                UpdateAdditionalData();
            } else {
                baseApp.SharedScanner.pause();
                if (actionType == ActionType.Scan || actionType == ActionType.Keyboard){
                    SetText(uxCode2, data);
                }
                General.Alert(this, getString(R.string.barcodeError), getString(R.string.barcodeInvalidated), AfterBadDataAlertCallable());
                Notifications.ScanError(baseApp.SharedScanner.getNotificationDevice());
            }
        }
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

    protected void UpdateAdditionalData() {
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
}
