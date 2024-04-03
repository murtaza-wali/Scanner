package com.ontrac.warehouse;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.Toast;

import com.ontrac.warehouse.OnTrac.APIs;
import com.ontrac.warehouse.OnTrac.APIsEventListener;
import com.ontrac.warehouse.Entities.LogSyncedScanData;
import com.ontrac.warehouse.Entities.User;
import com.ontrac.warehouse.Entities.ZipCode;
import com.ontrac.warehouse.Utilities.Networks;
import com.ontrac.warehouse.Utilities.Notifications;
import com.ontrac.warehouse.Utilities.Preferences;
import com.ontrac.warehouse.Utilities.Strings;
import com.ontrac.warehouse.Utilities.UX.General;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import okhttp3.Request;
import pub.devrel.easypermissions.EasyPermissions;

public class LoginActivity extends AppCompatActivity implements APIsEventListener, EasyPermissions.PermissionCallbacks {
    private final String Title = "OnTrac {0}";

    BaseApplication baseApp = null;

    @BindView(R.id.uxUserId) EditText uxUserId;
    @BindView(R.id.uxPassword) EditText uxPassword;

    static String[] PERMISSIONS = {
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private boolean _actionUpdateUsers = false;
    private boolean _actionUpdateZipScheme = false;

    APIs apis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            webViewLocalesBugWorkaround();
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        baseApp = ((BaseApplication)getApplicationContext());

        boolean changingOrientation = baseApp.setOrientation(this);

        if (!changingOrientation) {
            ButterKnife.bind(this);

            getSupportActionBar().setTitle(MessageFormat.format(Title, getString(R.string.login)));

            if (baseApp.config == null) {
                General.Alert(this, getString(R.string.error), getString(R.string.fileNotRead));
                System.exit(0);
            } else {
                CheckForRecords();
            }

            if (!EasyPermissions.hasPermissions(this, PERMISSIONS)) {
                EasyPermissions.requestPermissions(this, getString(R.string.writePermission), 123, PERMISSIONS);
            }


            //TODO: REMOVE AUTO LOGIN
            //uxUserId.setText("21");
            //uxPassword.setText("21");
        }
    }

    private void webViewLocalesBugWorkaround() {
        WebView webView = new WebView(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode,permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode,permissions,grantResults,this);
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        // Some permissions have been denied
        // ...
        if (requestCode == 123) {
            if (list.contains("android.permission.WRITE_EXTERNAL_STORAGE")){
                General.Alert(this, getString(R.string.exitingApp), getString(R.string.readWritePerm), AfterAlertExitCallable());
            }
        }
    }

    protected Callable AfterAlertExitCallable() {
        return new Callable() {
            public Object call() {
                AfterAlertExit();
                return null;
            }

            ;
        };
    }

    protected void AfterAlertExit()
    {
        System.exit(1);
    }


    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        // Some permissions have been granted
        // ...
        if (requestCode ==123)
        {
            baseApp.onTracDeviceId.ReadFile();
        }
    }


    @OnClick(R.id.uxcmdLogin)
    protected void uxcmdLogin(){
        SharedPreferences sessionTimer = getSharedPreferences("SessionTimer", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sessionTimer.edit();
        editor.putString("loginTime", DateTime.now().toString());
        editor.commit();
        Notifications.Vibrate();
        Login();

        /*
        for(int i = 1; i < 151; i++){
            //ScanDataCache.Save("OS", "123456789", "", baseApp.authUser.Id, baseApp.authUser.FacilityCode, baseApp.identity.Asset);
            ScanDataCache.Save("OS", String.valueOf(i), "", 1, "COR", baseApp.identity.Asset);
        }
        */
    }

    protected void Login(){
        String userId = uxUserId.getText().toString();
        String password = uxPassword.getText().toString();

        if (Strings.IsNumber(userId)){
            final User.AuthenticateResult auth = User.Authenticate(Strings.TryParseInt(userId), password);

            if (auth.Authorized) {
                final Toast toast = Notifications.ToastLong(getString(R.string.loggingIn));

                new Handler().post(new Runnable() {
                    @Override
                    public void run() {
                        baseApp.authUser = User.Clone(auth.User);
                        baseApp.CacheSyncStart();

                        Intent intent = new Intent(BaseApplication.getAppContext(), MainActivity.class);
                        startActivity(intent);

                        toast.cancel();
                    }
                });

                this.finish();
            } else {
                General.SetTextInputLayoutError(this, R.id.uxUserIdWrapper, getString(R.string.loginFailed));
            }
        } else {
            General.SetTextInputLayoutError(this, R.id.uxUserIdWrapper, getString(R.string.userNotNumerical));
        }
    }

    @OnEditorAction(R.id.uxPassword)
    public boolean uxPassword_EditorAction(int action) {
        if (action == EditorInfo.IME_ACTION_DONE){
            Login();
        }

        return false;
    }

    protected void CheckForRecords() {
        _actionUpdateUsers = User.Count() == 0;
        _actionUpdateZipScheme = ZipCode.Count() == 0;

        if (Networks.hasDataConnection(this)){
            UpdateRecords();
        } else {
            General.Alert(this, getString(R.string.networkError), getString(R.string.enableWifiError));
        }
    }

    protected void UpdateRecords() {
        String assetTag = baseApp.onTracDeviceId.GetAssetTag();
        apis = new APIs(baseApp.config, 1, "COR", assetTag);
        apis.addListener(this);

        if (_actionUpdateUsers){
            startSyncDialog();

            syncDialogHandler.sendEmptyMessage(SyncMessageWhat.UserStart);

            Request request = apis.BuildGet(baseApp.config.ApiUserInfo);
            _syncCallResult = apis.Call(APIs.Type.UserInfo, request, null);
        } else if (_actionUpdateZipScheme) {
            startSyncDialog();

            syncDialogHandler.sendEmptyMessage(SyncMessageWhat.ZipSchemeStart);

            Request request = apis.BuildGet(baseApp.config.ApiZipScheme);
            _syncCallResult = apis.Call(APIs.Type.ZipScheme, request, null);
        } else {
            syncDialogHandler.sendEmptyMessage(SyncMessageWhat.Cancel);
        }
    }

    public void setLocale(String lang) {


        Locale locale = new Locale(lang);
        Locale.setDefault(locale);

        Resources resources = getResources();

        Configuration configuration = resources.getConfiguration();
        configuration.setLocale(locale);
        configuration.locale = locale;

        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
        Intent refresh = new Intent(this, LoginActivity.class);
        finish();
        startActivity(refresh);
    }

    private Menu uxMenu;
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.login, menu);
        uxMenu = menu;

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.uxSyncUsers:
                _actionUpdateUsers = true;
                UpdateRecords();
                return true;
            case R.id.uxSyncZips:
                _actionUpdateZipScheme = true;
                UpdateRecords();
                return true;
            case R.id.uxSyncAll:
                _actionUpdateUsers = true;
                _actionUpdateZipScheme = true;
                UpdateRecords();
                return true;
            case R.id.uxClearUsers:
                User.DeleteAll();
                Preferences.Save(baseApp, BaseApplication.Preference.SyncUserDate, new DateTime(DateTimeZone.UTC).toString());
                Toast.makeText(this, getText(R.string.clearedUserData), Toast.LENGTH_SHORT).show();
                return true;
            case R.id.uxClearZipScheme:
                ZipCode.DeleteAll();
                Preferences.Save(baseApp, BaseApplication.Preference.SyncZipSchemeDate, new DateTime(DateTimeZone.UTC).toString());
                Toast.makeText(this, getText(R.string.clearedZipSchemeData), Toast.LENGTH_SHORT).show();
                return true;
            case R.id.uxClearScannedLog:
                LogSyncedScanData.DeleteAll();
                Toast.makeText(this, getText(R.string.clearedSyncLog), Toast.LENGTH_SHORT).show();
                return true;
            case R.id.uxEnglish:
                setLocale("en");
                return true;
            case R.id.uxSpanish:
                setLocale("es");
                return true;
            case R.id.uxFrench:
                setLocale("fr");
                return true;
            case R.id.uxHaitianCreole:
                setLocale("ht");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private APIs.CallResult _syncCallResult = null;
    private ProgressDialog _syncDialog = null;

    protected void startSyncDialog()
    {
        if (_syncDialog == null || !_syncDialog.isShowing()) {
            _syncDialog = new ProgressDialog(this);
            _syncDialog.setTitle(getString(R.string.loading));
            _syncDialog.setMessage("");
            _syncDialog.setCancelable(false);
            _syncDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    syncDialogHandler.sendEmptyMessage(SyncMessageWhat.Cancel);
                }
            });
            _syncDialog.show();
        }
        /*
        else {
            _syncDialog.setMessage(message);
        }
        */
    }

     class SyncMessageWhat {
        public static final int Cancel = 0;
        public static final int UserStart = 10;
//        public static final String UserStartMessage = getString(R.string.retrievingUserData);
        public static final int UserSaving = 11;
//        public  final String UserSavingMessage = getString(R.string.savingUserData);
//        public  final String UserSavedMessage = getString(R.string.savedUserRecords);

        public static final int ZipSchemeStart = 20;
//        public  final String ZipSchemeStartMessage = getString(R.string.retrievingZipSchemeData);
        public static final int ZipSchemeSaving = 21;
//        public  final String ZipSchemeSavingMessage = getString(R.string.savingZipSchemeData);
//        public  final String ZipSchemeSavedMessage = getString(R.string.savedZipRecords);
    }

    Handler syncDialogHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SyncMessageWhat.Cancel:
                    if (_syncCallResult != null) {
                        if (_syncCallResult.Call != null) {
                            _syncCallResult.Call.cancel();
                        }

                        if (_syncCallResult.Thread != null) {
                            _syncCallResult.Thread.interrupt();
                            _syncCallResult.Thread = null;
                        }
                    }

                    if (_syncDialog != null) {
                        _syncDialog.dismiss();
                        _syncDialog = null;
                    }
                    break;
                case SyncMessageWhat.UserStart:
                        setSyncMessage(getString(R.string.retrievingUserData));
                    break;
                case SyncMessageWhat.UserSaving:
                    setSyncMessage(getString(R.string.savingUserData));
                    break;
                case SyncMessageWhat.ZipSchemeStart:
                    setSyncMessage(getString(R.string.retrievingZipSchemeData));
                    break;
                case SyncMessageWhat.ZipSchemeSaving:
                    setSyncMessage(getString(R.string.savingZipSchemeData));
                    break;
            }
        }
    };

    private void setSyncMessage(String message) {
        if (_syncDialog != null) {
            _syncDialog.setMessage(message);
        }
    }

    @Override
    public void RequestCompleted(int type, int responseCode, String responseBody, Object data) throws Exception {
        switch (type) {

            case APIs.Type.ZipScheme:
                HandleZipScheme(responseCode, responseBody);
                break;
            case APIs.Type.UserInfo:
                HandleUserInfo(responseCode, responseBody);
                break;
            default:
                throw new Exception(getString(R.string.unhandledApiCall));
        }
    }

    @Override
    public void CallCompleted(int type, Object data, List<ClientConnectionError> errors) {
        if (errors != null && errors.size() > 0) {
            if (_syncDialog != null) {
                //|| _syncDialog.isShowing()
                _syncDialog.dismiss();
                _syncDialog = null;
            }

            ClientConnectionError error = errors.get(0);

            switch (error.Type){
                case UnknownHost:
                    General.Alert(this, getString(R.string.unknownHostError), getString(R.string.checkInternetConnection));
                    break;
                case SocketTimeout:
                    General.Alert(this, getString(R.string.socketTimeoutError), getString(R.string.checkInternetConnection));
                    break;
                default:
                    //General.Alert(this, "Connection Error", "Check the device's internet connectivity and retry.");
                    break;
            }
        }
    }


    public void HandleUserInfo(final int responseCode, final String responseBody){
        final Activity activity = this;

        if (responseCode == 200) {
            syncDialogHandler.sendEmptyMessage(SyncMessageWhat.UserSaving);

            User.DeleteAll();

            APIs.UserInfoObject[] items = APIs.UserInfoObject.fromJson(responseBody);

            int count = User.Save(items);

            Preferences.Save(baseApp, BaseApplication.Preference.SyncUserDate, new DateTime(DateTimeZone.UTC).toString());

            final String msg = MessageFormat.format(getString(R.string.savedUserRecords), count);

            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();

                    _actionUpdateUsers = false;
                    UpdateRecords();
                }
            });
        }
        else {
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    General.Alert(activity, getString(R.string.synchronizeFailure), responseCode + "\r" + responseBody);
                }
            });
        }
    }

    public void HandleZipScheme(final int responseCode, final String responseBody){
        final Activity activity = this;

        if (responseCode == 200) {
            syncDialogHandler.sendEmptyMessage(SyncMessageWhat.ZipSchemeSaving);

            ZipCode.DeleteAll();

            APIs.ZipSchemeObject[] items = APIs.ZipSchemeObject.fromJson(responseBody);

            int count = ZipCode.Save(items);

            Preferences.Save(baseApp, BaseApplication.Preference.SyncZipSchemeDate, new DateTime(DateTimeZone.UTC).toString());

            final String msg = MessageFormat.format(getString(R.string.savedZipRecords), count);

            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();

                    _actionUpdateZipScheme = false;
                    UpdateRecords();
                }
            });
        }
        else {
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    General.Alert(activity, getString(R.string.synchronizeFailure), responseCode + "\r" + responseBody);
                }
            });
        }
    }
}

