package com.ontrac.warehouse;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
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
import butterknife.OnClick;
import butterknife.OnEditorAction;
import butterknife.OnFocusChange;
import butterknife.OnLongClick;

import static com.ontrac.warehouse.OnTrac.Utilities.ValidateUser;
import static j2html.TagCreator.body;
import static j2html.TagCreator.div;
import static j2html.TagCreator.span;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.length;

public class DimActivity  extends SharedScannerActivity {
    private static final String Title =  "OnTrac {1} {2} - {0}";
    BaseApplication baseApp = null;
    Action.ScanType scanType = null;
    private HideKeyboard _hidekey;

    @BindView(R.id.uxWebView)
    WebView uxWebView;
    @BindView(R.id.uxStatus)
    Spinner uxStatus;
    @BindView(R.id.uxCode)
    EditText uxCode;
    @BindView(R.id.uxLength)
    EditText uxLength;
    @BindView(R.id.uxWidth)
    EditText uxWidth;
    @BindView(R.id.uxHeight)
    EditText uxHeight;
    @BindView(R.id.buttonRepeat)
    Button buttonRepeat;

    String currentItem = "uxCode";
    String length = "";
    String width = "";
    String height = "";
    String _lastScan = "";
    String _onTrac2DBarcode = "";
    boolean canRepeat = false;
    int _totalScanned = 0;
    final Handler _statusHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dim);

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

            uxCode.requestFocus();
            uxLength.setEnabled(false);
//            uxLength.setFocusable(false);
            uxWidth.setEnabled(false);
            uxHeight.setEnabled(false);
            buttonRepeat.setEnabled(false);
            uxWebView.setFocusable(false);
            buttonRepeat.setFocusable(false);

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

    @OnFocusChange({R.id.uxCode, R.id.uxLength, R.id.uxWidth, R.id.uxHeight})
    public void OnFocusChange(View view, boolean hasFocus) {
        if ((view.getId() == R.id.uxLength) && hasFocus && canRepeat)
            buttonRepeat.setEnabled(true);
        else
            buttonRepeat.setEnabled(false);
        if ((view.getId() == R.id.uxLength) && (currentItem == "uxCode") && hasFocus)
            uxCode.requestFocus();
        _hidekey.OnFocusChange(view, hasFocus);
    }

    @OnLongClick({R.id.uxCode, R.id.uxLength, R.id.uxWidth, R.id.uxHeight})
    public boolean OnLongClick(View view) {
        _hidekey.OnLongClick(view);
        return true; // hides the paste context popup
    }

    @OnEditorAction({R.id.uxCode, R.id.uxLength, R.id.uxWidth, R.id.uxHeight})
    public boolean OnEditorAction(TextView view, int action) {
        switch(view.getId()) {
            case R.id.uxCode : {
                Process(uxCode.getText().toString());
                break;
            }
            case R.id.uxLength : {
                Process(uxLength.getText().toString());
                break;
            }
            case R.id.uxWidth : {
                Process(uxWidth.getText().toString());
                break;
            }
            case R.id.uxHeight : {
                Process(uxHeight.getText().toString());
                break;
            }
        }
        _hidekey.OnEditorAction(view, action);
        return false;
    }

    @OnClick(R.id.buttonRepeat)
    protected void uxcmdRepeat(){
        com.ontrac.warehouse.Utilities.Notifications.Vibrate();
        Process(length);
        Process(width);
        Process(height);
    }

    protected void Process(String data)
    {
        boolean doContinue = true;
        String status =  uxStatus.getSelectedItem().toString();

        Callable doAfter = new Callable() {
            public Object call() {
                AfterBadDataAlert();
                return null;
            }
        };

        if (Strings.IsNullOrWhiteSpace(status)) {
            doContinue = false;
            General.Alert(this, getString(R.string.dimError), getString(R.string.statusSelectionError), doAfter);
        }

        if (!Strings.IsNullOrWhiteSpace(data) && doContinue && (currentItem == "uxCode")){
            _onTrac2DBarcode = "";
            if (Utilities.isValidOnTrac2DBarcode(data)) {
                _onTrac2DBarcode = data;
                String splitArray[] = data.split("\035");
                data = splitArray[4];
            }
            if (Utilities.isValidLaserShip2DBarcode(data))
            {
                _onTrac2DBarcode = data;
                data = data.substring(0,data.indexOf("|"));
            }

            if (baseApp.config.Ems | scanType == Action.ScanType.WfBin | scanType == Action.ScanType.WfRD) {
                GoodData(data);
            } else {
                if (Utilities.ValidateOnTracCode(data)) {
                    GoodData(data);
                } else {
                    BadData(data);
                }
            }
        }
        else if (!Strings.IsNullOrWhiteSpace(data) && doContinue && (currentItem == "uxLength" || currentItem == "uxWidth" || currentItem == "uxHeight" )) {
                if (data.substring(0,1).equals("0"))
                    data = data.substring(1,length(data));
                if (data.matches("^[1-9][0-9]{1,2}$|^[1-9]$")) {
                    GoodData(data);
                } else {
                    BadData(data);
                }
        }
    }
    protected void GoodData(final String data){
        String status =  uxStatus.getSelectedItem().toString();
        switch(currentItem){
            case "uxCode" :{
                this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        uxCode.setText(data);
                        uxLength.setText("");
                        uxWidth.setText("");
                        uxHeight.setText("");
                        uxStatus.setEnabled(false);
                        uxCode.setEnabled(false);
                        uxLength.setEnabled(true);
//                        uxLength.setFocusable(true);
                        uxLength.requestFocus();
//                        uxCode.setFocusable(false);
                    }
                });
                currentItem = "uxLength";
                _lastScan = data;
                break;
            }
            case "uxLength" :{
                this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        uxLength.setText(data);
                        uxLength.setEnabled(false);
                        uxWidth.setEnabled(true);
                        uxWidth.requestFocus();
                    }
                });
                currentItem = "uxWidth";
                length = data;
                break;
            }

            case "uxWidth" : {
                this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        uxWidth.setText(data);
                        uxWidth.setEnabled(false);
                        uxHeight.setEnabled(true);
                        uxHeight.requestFocus();
                    }
                });
                currentItem = "uxHeight";
                width = data;
                break;
            }

            case "uxHeight" : {
                this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        uxHeight.setText(data);
                        uxHeight.setEnabled(false);
                        uxCode.setEnabled(true);
                        uxCode.requestFocus();
                        uxCode.setText("");
                        canRepeat = true;
                    }
                });
                currentItem = "uxCode";
                height = data;
                _totalScanned += 1;
                String assetTag = baseApp.onTracDeviceId.GetAssetTag();
                ScanDataCache.Save(status, _lastScan, "", baseApp.authUser.Id, baseApp.authUser.FacilityCode, assetTag, _onTrac2DBarcode);
                ScanDataCache.Save("DIM", _lastScan, length+","+width+","+height,baseApp.authUser.Id, baseApp.authUser.FacilityCode, assetTag,"");
                UpdateAdditionalData();
                break;
            }
        }
    }

    protected void BadData(final String data){
        baseApp.SharedScanner.pause();

        Callable doAfter = new Callable() {
            public Object call() {
                AfterBadDataAlert();
                return null;
            }
        };

        switch(currentItem){
            case "uxCode" : {
                this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        uxCode.setText(data);
                    }
                });
                General.Alert(this, getString(R.string.barcodeError), getString(R.string.barcodeInvalidated), doAfter);
                break;
            }
            case "uxLength" : {
                this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        uxLength.setText(data);
                    }
                });
                General.Alert(this, getString(R.string.lengthError), getString(R.string.lengthInvalid), doAfter);
                break;
            }

            case "uxWidth" : {
                this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        uxWidth.setText(data);
                    }
                });
                General.Alert(this, getString(R.string.widthError), getString(R.string.widthInvalid), doAfter);
                break;
            }

            case "uxHeight" : {
                this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        uxHeight.setText(data);
                    }
                });
                General.Alert(this, getString(R.string.heightError), getString(R.string.heightInvalid), doAfter);
                break;
            }
        }
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
