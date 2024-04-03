package com.ontrac.warehouse;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.Spinner;
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

import static com.ontrac.warehouse.OnTrac.Utilities.ValidateUser;
import static j2html.TagCreator.body;
import static j2html.TagCreator.div;
import static j2html.TagCreator.span;

public class AHActivity extends SharedScannerActivity {
    private static final String Title =  "OnTrac {1} {2} - {0}";

    BaseApplication baseApp = null;
    Action.ScanType scanType = null;

    private HideKeyboard _hidekey;

    @BindView(R.id.uxWebView)
    WebView uxWebView;

    @BindView(R.id.uxCode)
    EditText uxCode;

    @BindView(R.id.uxSubScan)
    Spinner uxSubScan;

    @BindView(R.id.uxReason)
    Spinner uxReason;

    String _lastScan = "";
    int _totalScanned = 0;
    final Handler _statusHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ah);
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

//            uxCodeWrapper.setHint(uxCode.getHint());

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
    public void Scanned(final String data) {
        Process(data);
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
        Process(uxCode.getText().toString());
        _hidekey.OnEditorAction(view, action);
        return false;
    }

    protected void Process(String data)
    {
        boolean doContinue = true;
        String onTrac2DBarcode = "";
        String subScan =  uxSubScan.getSelectedItem().toString();
        String reason = uxReason.getSelectedItem().toString();

        Callable doAfter = new Callable() {
            public Object call() {
                AfterBadDataAlert();
                return null;
            }
        };

        if (Strings.IsNullOrWhiteSpace(subScan)) {
            doContinue = false;
            General.Alert(this, getString(R.string.ahError), getString(R.string.subscanSelectionError), doAfter);
        }
        if (Strings.IsNullOrWhiteSpace(reason) && doContinue) {
            doContinue = false;
            General.Alert(this, getString(R.string.ahError), getString(R.string.reasonSelectionError), doAfter);
        }

        if (!Strings.IsNullOrWhiteSpace(data) && doContinue){
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

            if (baseApp.config.Ems | scanType == Action.ScanType.WfBin | scanType == Action.ScanType.WfRD) {
                GoodData(data, onTrac2DBarcode);
            } else {
                if (Utilities.ValidateOnTracCode(data)) {
                    GoodData(data, onTrac2DBarcode);
                } else {
                    BadData(data);
                }
            }
        }
    }

    protected void GoodData(String data, String onTrac2DBarcode){
        _totalScanned += 1;
        _lastScan = data;
        String subScan =  uxSubScan.getSelectedItem().toString();
        String reason = uxReason.getSelectedItem().toString();

        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                uxCode.setText("");
            }
        });

//        String statusCode = Action.GetCode(scanType);
        String assetTag = baseApp.onTracDeviceId.GetAssetTag();
        ScanDataCache.Save(subScan, data, "", baseApp.authUser.Id, baseApp.authUser.FacilityCode, assetTag, onTrac2DBarcode);
        ScanDataCache.Save("AH", data, reason,baseApp.authUser.Id, baseApp.authUser.FacilityCode, assetTag,"");
        UpdateAdditionalData();
    }

    protected void BadData(final String data){
        baseApp.SharedScanner.pause();

        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                uxCode.setText(data);
            }
        });

        Callable doAfter = new Callable() {
            public Object call() {
                AfterBadDataAlert();
                return null;
            }
        };

        General.Alert(this, getString(R.string.barcodeError), getString(R.string.barcodeInvalidated), doAfter);

        Notifications.ScanError(baseApp.SharedScanner.getNotificationDevice());
    }

    protected void AfterBadDataAlert()
    {
        baseApp.SharedScanner.resume();

        UpdateAdditionalData();
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



}
