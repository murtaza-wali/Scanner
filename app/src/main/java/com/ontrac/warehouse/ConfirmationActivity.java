package com.ontrac.warehouse;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.ontrac.warehouse.Entities.Action;
import com.ontrac.warehouse.Utilities.Notifications;
import com.ontrac.warehouse.Utilities.Zebra.SharedScannerActivity;

import java.text.MessageFormat;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ConfirmationActivity extends SharedScannerActivity {
    private final static String TAG = ConfirmationActivity.class.getName();
    private static final String Title = "OnTrac {2} {1} - {0}";

    BaseApplication baseApp = null;

    Action.ScanType scanType = null;

    @BindView(R.id.uxcmdYes)
    Button uxcmdYes;

    @BindView(R.id.uxcmdNo)
    Button uxcmdNo;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        baseApp = ((BaseApplication)getApplicationContext());

        Intent source = getIntent();
        scanType = (Action.ScanType) source.getSerializableExtra("ScanType");
        String buttonText = source.getStringExtra("ButtonText");
        int buttonForeColor = source.getIntExtra("ButtonForeColor", -1);
        int buttonBackgroundColor = source.getIntExtra("ButtonBackgroundColor", -1);
        switch (scanType) {
            case RD:
                setContentView(R.layout.activity_rd_confirmation);
                break;
            case OS:
                setContentView(R.layout.activity_os_confirmation);
                break;
        }
        ActionBar actionBar = getSupportActionBar();
        SpannableString barTitle = new SpannableString((MessageFormat.format(Title, baseApp.authUser.FriendlyName(), baseApp.authUser.FacilityCode, scanType.toString())));
        barTitle.setSpan(new ForegroundColorSpan(buttonForeColor), 0, barTitle.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        actionBar.setTitle(barTitle);
        actionBar.setBackgroundDrawable(new ColorDrawable(buttonBackgroundColor));

        CanScan = false;

        ButterKnife.bind(this);
    }

    @OnClick({R.id.uxcmdYes})
    public void LoadSinglePrompt(final Button button) {
        final Toast toast = Notifications.ToastLong("Loading...");
        Notifications.Vibrate();

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                Intent source = getIntent();
                int buttonBackgroundColor = source.getIntExtra("ButtonBackgroundColor", -1);
                int buttonForeColor = source.getIntExtra("ButtonForeColor", button.getTextColors().getDefaultColor());

                if (scanType == Action.ScanType.RD) {
                    Intent intent = new Intent(BaseApplication.getAppContext(), PrintConfirmationActivity.class);
                    intent.putExtra("ScanType", scanType);
                    intent.putExtra("ButtonBackgroundColor", buttonBackgroundColor);
                    intent.putExtra("ButtonForeColor", buttonForeColor);
                    intent.putExtra("ButtonText", "RD");
                    startActivity(intent);
                    toast.cancel();
                } else {
                    Intent intent = new Intent(BaseApplication.getAppContext(), SinglePromptActivity.class);
                    intent.putExtra("ScanType", scanType);
                    intent.putExtra("ButtonBackgroundColor", buttonBackgroundColor);
                    intent.putExtra("ButtonForeColor", buttonForeColor);
                    intent.putExtra("ButtonText", "OS");
                    startActivity(intent);
                    toast.cancel();
                }


            }
        });

    }

    @OnClick({R.id.uxcmdNo})
    public void finishActivity() {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        });
    }
}
