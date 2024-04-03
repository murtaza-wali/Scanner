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
import android.widget.EditText;
import android.widget.TextView;

import com.google.gson.Gson;
import com.ontrac.warehouse.Entities.Action;
import com.ontrac.warehouse.Entities.TrailerProcessResults;
import com.ontrac.warehouse.Entities.TrailerStatus;
import com.ontrac.warehouse.OnTrac.APIs;
import com.ontrac.warehouse.OnTrac.APIsEventListener;
import com.ontrac.warehouse.OnTrac.Utilities;
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

public class TrailerUnloadActivity extends SharedScannerActivity implements APIsEventListener {
    private static final String Title =  "Unload Trailer {1} - {0}";

    private UIHelper helper = new UIHelper(this);

    private APIs.CallResult _lookupCallResult;
    private ProgressDialog _lookupDialog = null;
    private APIs _apis;

    BaseApplication baseApp = null;
    Action.ScanType scanType = null;

    private HideKeyboard _hidekey;

    @BindView(R.id.uxTrailer)
    EditText uxTrailer;

    @BindView(R.id.uxTrailerSeal)
    EditText uxTrailerSeal;

    @BindView(R.id.uxUnloadDoor)
    EditText uxUnloadDoor;

    String currentItem = "uxTrailer";
    String trailer = "";
    String unloadDoor = "";
    String trailerSeal = "";
    final Handler _statusHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trailer_unload);

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
            actionBar.setTitle(barTitle);
            actionBar.setBackgroundDrawable(new ColorDrawable(buttonBackgroundColor));

            ButterKnife.bind(this);
            CanScan = true;
            _hidekey = new HideKeyboard(this);
            uxTrailer.requestFocus();
            uxTrailerSeal.setEnabled(false);

            final int interval = 2000;
            _statusHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
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

    @OnFocusChange({R.id.uxTrailer, R.id.uxTrailerSeal})
    public void OnFocusChange(View view, boolean hasFocus) {
        _hidekey.OnFocusChange(view, hasFocus);
    }

    @OnLongClick({R.id.uxTrailer, R.id.uxTrailerSeal})
    public boolean OnLongClick(View view) {
        _hidekey.OnLongClick(view);
        return true; // hides the paste context popup
    }

    @OnEditorAction({R.id.uxTrailer, R.id.uxTrailerSeal})
    public boolean OnEditorAction(TextView view, int action) {
        switch(view.getId()) {
            case R.id.uxTrailer : {
                Process(uxTrailer.getText().toString());
                break;
            }
            case R.id.uxTrailerSeal : {
                Process(uxTrailerSeal.getText().toString());
                break;
            }
        }
        _hidekey.OnEditorAction(view, action);
        return false;
    }


    protected void Process(String data)
    {
        boolean doContinue = true;

        Callable doAfter = new Callable() {
            public Object call() {
                AfterBadDataAlert();
                return null;
            }
        };

        if (!Strings.IsNullOrWhiteSpace(data) && doContinue && (currentItem == "uxTrailer")){
            if (Utilities.isValidTrailerBarcode(data)) {
                GoodData(data);
            } else {
                BadData(data);
            }
        }
        else if (!Strings.IsNullOrWhiteSpace(data) && doContinue && (currentItem == "uxUnloadDoor")){
            if (Utilities.isValidDoorBarcode(data)){
                GoodData(data);
            }
            else {
                BadData(data);
            }

        }
        else if (!Strings.IsNullOrWhiteSpace(data) && doContinue && (currentItem == "uxTrailerSeal")) {
            if (Utilities.isValidTrailerSealBarcode(data)) {
                GoodData(data);
            } else {
                BadData(data);
            }
        }
    }
    protected void GoodData(final String data){

        switch(currentItem){
            case "uxTrailer" :{
                this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        uxTrailer.setText(data);
                        uxUnloadDoor.setText("");
                        uxTrailer.setEnabled(false);
                        uxUnloadDoor.setEnabled(true);
                        uxUnloadDoor.requestFocus();
                        startLookupDialog("Searching...", "Checking trailer status.");
                    }
                });
                currentItem = "uxUnloadDoor";
                trailer = data;
                APIs.TrailerStatusRequestObject trailerStatusRequest = new APIs.TrailerStatusRequestObject();
                trailerStatusRequest.Trailer = trailer;
                trailerStatusRequest.LoadDoor = "NA";
                trailerStatusRequest.ProcessType = "UNLOAD";
                CheckIfTrailerIsClosed(trailerStatusRequest);
                break;
            }
            case "uxUnloadDoor" : {
                this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        uxUnloadDoor.setText(data);
                        uxTrailerSeal.setText("");
                        uxUnloadDoor.setEnabled(false);
                        uxTrailerSeal.setEnabled(true);
                        uxTrailerSeal.requestFocus();
                    }
                });
                currentItem = "uxTrailerSeal";
                unloadDoor = data;
                break;
            }
            case "uxTrailerSeal" : {
                this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        uxTrailerSeal.setText(data);
                        uxTrailerSeal.setEnabled(false);
                        startLookupDialog("Prcoessing...", "Attempting to unload trailer.");

                    }
                });
                trailerSeal = data;

                APIs.TrailerProcessRequestObject trailerProcessRequest = new APIs.TrailerProcessRequestObject();
                trailerProcessRequest.Trailer = trailer;
                trailerProcessRequest.TrailerSeal = trailerSeal;
                trailerProcessRequest.UnloadDoor = unloadDoor;
                trailerProcessRequest.ProcessType = "UNLOAD";
                UnloadTrailer(trailerProcessRequest);

                break;
            }
        }
    }

    protected void CheckIfTrailerIsClosed(APIs.TrailerStatusRequestObject trailerStatusRequest)
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

    protected void UnloadTrailer(APIs.TrailerProcessRequestObject trailerProcessRequest)
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
        Request request = _apis.BuildPost(baseApp.config.ApiProcessTrailer, APIs.PostType.JSON, trailerProcessRequest.toJson());
        _lookupCallResult = _apis.Call(APIs.Type.TrailerProcess, request, trailerProcessRequest);
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
            case "uxTrailer" : {
                this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        uxTrailer.setText(data);
                    }
                });
                General.Alert(this, "Trailer Barcode Error", "This is not a valid trailer barcode.", doAfter);
                break;
            }
            case "uxUnloadDoor" : {
                this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        uxUnloadDoor.setText(data);
                    }
                });
                General.Alert(this, "Unload Door Error", "This is not a valid unload door barcode.", doAfter);
                break;
            }
            case "uxTrailerSeal" : {
                this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        uxTrailerSeal.setText(data);
                    }
                });
                General.Alert(this, "Trailer Seal Barcode Error", "This is not a valid trailer seal barcode.", doAfter);
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
                        this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                uxTrailerSeal.setEnabled(true);
                                uxTrailerSeal.requestFocus();
                            }
                        });
                    }
                    else
                        General.Alert(activity, "Unload Error", trailerStatus.ErrorMessage, AfterSuccessAlertCallable());
                }
                else
                    this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            General.Alert(activity, "API Failure", responseCode + "\r" + responseBody, AfterSuccessAlertCallable());
                        }
                    });
                break;
            case APIs.Type.TrailerProcess:
                if (responseCode == 200)
                {
                    TrailerProcessResults trailerProcessResults = new Gson().fromJson(responseBody, TrailerProcessResults.class) ;
                    General.Alert(this,"Trailer Unload Message", trailerProcessResults.message ,AfterSuccessAlertCallable());
                }
                else
                    this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            General.Alert(activity, "API Failure", responseCode + "\r" + responseBody,AfterSuccessAlertCallable());
                        }
                    });
                break;
            default:
                throw new Exception("Unhandled API call!");
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
/*
            _lookupDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    CancelLookup();
                }
            });
*/
            _lookupDialog.show();
        }
    }
}