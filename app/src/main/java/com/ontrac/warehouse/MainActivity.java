package com.ontrac.warehouse;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import com.ontrac.warehouse.Entities.Action;
import com.ontrac.warehouse.Entities.LogSyncedScanData;
import com.ontrac.warehouse.OnTrac.Utilities;
import com.ontrac.warehouse.Utilities.Notifications;
import com.ontrac.warehouse.Utilities.UX.Orientation;
import com.ontrac.warehouse.Utilities.Zebra.SharedScannerActivity;

import java.text.MessageFormat;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.ontrac.warehouse.OnTrac.Utilities.ValidateUser;

import org.joda.time.DateTime;

public class MainActivity extends SharedScannerActivity {
    private final static String TAG = MainActivity.class.getName();
    private static final String Title =  "OnTrac {1} - {0}";
    BaseApplication baseApp = null;

    private boolean _emsFlag;

    @BindView(R.id.uxcmdOS)
    Button uxcmdOS;

    @BindView(R.id.uxcmdRD)
    Button uxcmdRD;

    @BindView(R.id.uxcmdStaples)
    Button uxcmdStaples;

    @BindView(R.id.uxcmdPS)
    Button uxcmdPS;

    @BindView(R.id.uxcmdZipLookup)
    Button uxcmdZipLookup;

//    @BindView(R.id.uxcmdSortLabel)
//    Button uxcmdSortLabel;

    @BindView(R.id.uxcmdExceptions)
    Button uxcmdExceptions;



    protected void disableButton(Button... buttons){
        for (Button button : buttons) {
            button.setEnabled(false);
            button.setBackgroundColor(Color.parseColor("#e7e7e7"));
            button.setTextColor(Color.parseColor("#c7c7c7"));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        baseApp = ((BaseApplication)getApplicationContext());
        boolean changingOrientation = baseApp.setOrientation(this);
        if (!changingOrientation) {
            ValidateUser(this, baseApp.authUser);
            if (Orientation.isLandscape(this)) {
                setContentView(R.layout.activity_main_portrait);
            } else {
                setContentView(R.layout.activity_main_portrait);
            }
//            recyclerView = (RecyclerView)findViewById(R.id.recyclerView);
            _emsFlag = baseApp.config.Ems;
            getSupportActionBar().setTitle(MessageFormat.format(Title, baseApp.authUser.FriendlyName(), baseApp.authUser.FacilityCode));
            ButterKnife.bind(this);
            CanScan = false;
            if (_emsFlag) {
                disableButton(uxcmdStaples);
                disableButton(uxcmdPS);
//                disableButton(uxcmdUspsOS);
//                disableButton(uxcmdUspsRD);
                disableButton(uxcmdZipLookup);
            }

        }
//        SharedPreferences sessionTimer = getSharedPreferences("SessionTimer", Context.MODE_PRIVATE);
//        SharedPreferences.Editor editor = sessionTimer.edit();
//        editor.putString("loginTime", DateTime.now().toString());
//        editor.commit();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // refresh your views here
        super.onConfigurationChanged(newConfig);
    }

    //    @OnClick({R.id.uxcmdOS, R.id.uxcmdRD, R.id.uxcmdXL, R.id.uxcmdSD, R.id.uxcmdHW, R.id.uxcmdWfRD, R.id.uxcmdWfBin})
    @OnClick({R.id.uxcmdOS, R.id.uxcmdRD})
    public void LoadSinglePrompt(final Button button) {
        final Toast toast = Notifications.ToastLong(getString(R.string.loading));
        Notifications.Vibrate();

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                Action.ScanType action = Action.ScanType.None;
                if (button == uxcmdOS){
                    action = Action.ScanType.OS;
                }
                else if (button == uxcmdRD){
                    action = Action.ScanType.RD;
                }

                int backgroundColor = ((ColorDrawable)button.getBackground()).getColor();
                String buttonText = button.getText().toString();
//                Intent intent = new Intent(BaseApplication.getAppContext(), SinglePromptActivity.class);
                Intent intent = new Intent(BaseApplication.getAppContext(), ConfirmationActivity.class);
                intent.putExtra("ScanType", action);
                intent.putExtra("ButtonBackgroundColor", backgroundColor);
                intent.putExtra("ButtonText", buttonText);
                intent.putExtra("ButtonForeColor", button.getTextColors().getDefaultColor());
                startActivity(intent);
                toast.cancel();
            }
        });
    }

    @OnClick({R.id.uxcmdZipLookup})
    public void LoadZipLookup(final Button button) {
        final Toast toast = Notifications.ToastLong(getString(R.string.loading));

        Notifications.Vibrate();

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                int backgroundColor = ((ColorDrawable)button.getBackground()).getColor();
                String buttonText = button.getText().toString();

                Intent intent = new Intent(BaseApplication.getAppContext(), ZipInfoActivity.class);

                intent.putExtra("ButtonBackgroundColor", backgroundColor);
                intent.putExtra("ButtonText", buttonText);
                intent.putExtra("ButtonForeColor", button.getTextColors().getDefaultColor());

                startActivity(intent);

                toast.cancel();
            }
        });
    }

//    @OnClick({R.id.uxcmdStaples, R.id.uxcmdPS, R.id.uxcmdUspsOS, R.id.uxcmdUspsRD, R.id.uxcmdTrailerLoad})
    @OnClick({R.id.uxcmdStaples, R.id.uxcmdPS})
    public void LoadDualPrompt(final Button button) {
        final Toast toast = Notifications.ToastLong(getString(R.string.loading));
        Notifications.Vibrate();

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                Action.ScanType action = Action.ScanType.None;

                if (button == uxcmdStaples){
                    action = Action.ScanType.Staples;
                }
                else if (button == uxcmdPS){
                    action = Action.ScanType.PS;
                }
//                else if (button == uxcmdUspsOS){
//                    action = Action.ScanType.UspsOS;
//                }
//                else if (button == uxcmdUspsRD){
//                    action = Action.ScanType.UspsRD;
//                }
//                else if (button == uxcmdTrailerLoad){
//                    action = Action.ScanType.TrailerLoad;
//                }

                int backgroundColor = ((ColorDrawable)button.getBackground()).getColor();
                String buttonText = button.getText().toString();

                Intent intent = new Intent(BaseApplication.getAppContext(), DualPromptActivity.class);

                intent.putExtra("ScanType", action);
                intent.putExtra("ButtonBackgroundColor", backgroundColor);
                intent.putExtra("ButtonText", buttonText);
                intent.putExtra("ButtonForeColor", button.getTextColors().getDefaultColor());

                startActivity(intent);

                toast.cancel();
            }
        });
    }



//
//    @OnClick({R.id.uxcmdSortLabel})
//    public void LoadSortLabel(final Button button) {
//        final Toast toast = Notifications.ToastLong("Loading...");
//        Notifications.Vibrate();
//        new Handler().post(new Runnable() {
//            @Override
//            public void run() {
//                Action.ScanType action = Action.ScanType.RD;
//                int backgroundColor = ((ColorDrawable)button.getBackground()).getColor();
//                String buttonText = button.getText().toString();
//                Intent intent = new Intent(BaseApplication.getAppContext(), SortLabelActivity.class);
//                intent.putExtra("ScanType", action);
//                intent.putExtra("ButtonBackgroundColor", backgroundColor);
//                intent.putExtra("ButtonText", buttonText);
//                intent.putExtra("ButtonForeColor", button.getTextColors().getDefaultColor());
//                startActivity(intent);
//                toast.cancel();
//            }
//        });
//    }

    public void LoadPintPrinterLabel(){
        final Toast toast = Notifications.ToastLong(getString(R.string.loading));
        Notifications.Vibrate();
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(BaseApplication.getAppContext(), PrintPrinterLabelActivity.class);
                startActivity(intent);
                toast.cancel();
            }
        });
    }

/*
    @OnClick({R.id.uxcmdTrailerMgmt})
    public void LoadTrailerMgmtMenu(final Button button) {
        final Toast toast = Notifications.ToastLong("Loading...");
        Notifications.Vibrate();
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                int backgroundColor = ((ColorDrawable)button.getBackground()).getColor();
                String buttonText = button.getText().toString();
                Intent intent = new Intent(BaseApplication.getAppContext(), TrailerMgmtActivity.class);
                intent.putExtra("ButtonBackgroundColor", backgroundColor);
                intent.putExtra("ButtonText", buttonText);
                intent.putExtra("ButtonForeColor", button.getTextColors().getDefaultColor());
                startActivity(intent);
                toast.cancel();
            }
        });
    }
*/

/*
    @OnClick({R.id.uxcmdTrailerLoad})
    public void LoadTrailerLoad(final Button button) {
        final Toast toast = Notifications.ToastLong("Loading...");

        Notifications.Vibrate();

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                int backgroundColor = ((ColorDrawable)button.getBackground()).getColor();
                String buttonText = button.getText().toString();
                Intent intent = new Intent(BaseApplication.getAppContext(), TrailerLoad2Activity.class);
                intent.putExtra("ButtonBackgroundColor", backgroundColor);
                intent.putExtra("ButtonText", buttonText);
                intent.putExtra("ButtonForeColor", button.getTextColors().getDefaultColor());
                startActivity(intent);
                toast.cancel();
            }
        });
    }
*/
    @OnClick({R.id.uxcmdExceptions})
    public void LoadExceptions(final Button button) {
        Notifications.Vibrate();


        new Handler().post(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(BaseApplication.getAppContext(), ExceptionActivity.class);
                startActivity(intent);
            }
        });
    }

    public void LoadScannerSelection(){
        final Toast toast = Notifications.ToastLong(getString(R.string.loading));
        Notifications.Vibrate();
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(BaseApplication.getAppContext(), SelectScannerActivity.class);
                startActivity(intent);
                toast.cancel();
            }
        });
    }

    public void LoadRecordAssetTagSelection(){
        final Toast toast = Notifications.ToastLong(getString(R.string.loading));
        Notifications.Vibrate();
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(BaseApplication.getAppContext(), RecordAssetTagActivity.class);
                startActivity(intent);
                toast.cancel();
            }
        });
    }

    public void setLocale(String lang) {
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);

        Resources resources = getResources();

        Configuration configuration = resources.getConfiguration();
        configuration.setLocale(locale);
        configuration.locale = locale;

        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
        Intent refresh = new Intent(this, MainActivity.class);
        finish();
        startActivity(refresh);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_About:
                Utilities.ShowAbout(baseApp, this);
                return true;
            case R.id.action_logoff:
                baseApp.authUser = null;
                Intent loginIntent = new Intent(this, LoginActivity.class);
                startActivity(loginIntent);
                this.finish();
                return true;
            case R.id.uxClearScannedLog:
                LogSyncedScanData.DeleteAll();
                Toast.makeText(this, getText(R.string.clearedSyncLog), Toast.LENGTH_SHORT).show();
                return true;
            case R.id.action_PrintPrinterLabel:
                LoadPintPrinterLabel();
                return true;
            case R.id.action_Exit:
                this.finish();
                System.exit(0);
                return true;
            case R.id.action_SelectScanner:
                LoadScannerSelection();
                return true;
            case R.id.action_RecordAssetTag:
                LoadRecordAssetTagSelection();
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

}
