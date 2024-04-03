package com.ontrac.warehouse;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.Toast;

import com.ontrac.warehouse.Entities.Action;
import com.ontrac.warehouse.Utilities.Notifications;
import com.ontrac.warehouse.Utilities.Zebra.SharedScannerActivity;

import java.text.MessageFormat;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ExceptionActivity extends SharedScannerActivity {
    private static final String Title = "OnTrac Exceptions {1} - {0}";

    BaseApplication baseApp = null;

    @BindView(R.id.uxcmdDIM)
    Button uxcmdDIM;

//    @BindView(R.id.uxcmdSD)
//    Button uxcmdSD;

    @BindView(R.id.uxcmdAH)
    Button uxcmdAH;

    @BindView(R.id.uxcmdHW)
    Button uxcmdHW;

    @BindView(R.id.uxcmdLD)
    Button uxcmdLD;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        baseApp = ((BaseApplication)getApplicationContext());
        setContentView(R.layout.activity_main_exceptions);
        getSupportActionBar().setTitle(MessageFormat.format(Title, baseApp.authUser.FriendlyName(), baseApp.authUser.FacilityCode));
        ButterKnife.bind(this);
        CanScan = false;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // refresh your views here
        super.onConfigurationChanged(newConfig);
    }

//    @OnClick({R.id.uxcmdSD, R.id.uxcmdHW, R.id.uxcmdLD})
@OnClick({R.id.uxcmdHW, R.id.uxcmdLD})
    public void LoadSinglePrompt(final Button button) {
        final Toast toast = Notifications.ToastLong(getString(R.string.loading));
        Notifications.Vibrate();

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                Action.ScanType action = Action.ScanType.None;
//                if (button == uxcmdSD){
//                    action = Action.ScanType.SD;
//                }
//                else
                if (button == uxcmdHW){
                    action = Action.ScanType.HW;
                }
                else if (button == uxcmdLD) {
                    action = Action.ScanType.LD;
                }

                int backgroundColor = ((ColorDrawable)button.getBackground()).getColor();
                String buttonText = button.getText().toString();
                Intent intent = new Intent(BaseApplication.getAppContext(), SinglePromptActivity.class);
                intent.putExtra("ScanType", action);
                intent.putExtra("ButtonBackgroundColor", backgroundColor);
                intent.putExtra("ButtonText", buttonText);
                intent.putExtra("ButtonForeColor", button.getTextColors().getDefaultColor());
                startActivity(intent);
                toast.cancel();
            }
        });
    }

    @OnClick({R.id.uxcmdAH})
    public void LoadAH(final Button button) {
        final Toast toast = Notifications.ToastLong("Loading...");

        Notifications.Vibrate();

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                Action.ScanType action = Action.ScanType.AH;
                int backgroundColor = ((ColorDrawable)button.getBackground()).getColor();
                String buttonText = button.getText().toString();
                Intent intent = new Intent(BaseApplication.getAppContext(), AHActivity.class);
                intent.putExtra("ScanType", action);
                intent.putExtra("ButtonBackgroundColor", backgroundColor);
                intent.putExtra("ButtonText", buttonText);
                intent.putExtra("ButtonForeColor", button.getTextColors().getDefaultColor());
                startActivity(intent);
                toast.cancel();
            }
        });
    }

    @OnClick({R.id.uxcmdDIM})
    public void LoadDIM(final Button button) {
        final Toast toast = Notifications.ToastLong(getString(R.string.loading));

        Notifications.Vibrate();

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                Action.ScanType action = Action.ScanType.DIM;
                int backgroundColor = ((ColorDrawable)button.getBackground()).getColor();
                String buttonText = button.getText().toString();
                Intent intent = new Intent(BaseApplication.getAppContext(), DimActivity.class);
                intent.putExtra("ScanType", action);
                intent.putExtra("ButtonBackgroundColor", backgroundColor);
                intent.putExtra("ButtonText", buttonText);
                intent.putExtra("ButtonForeColor", button.getTextColors().getDefaultColor());
                startActivity(intent);
                toast.cancel();
            }
        });
    }

}
