package com.ontrac.warehouse;

import android.content.Intent;
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

public class PrintConfirmationActivity extends SharedScannerActivity {
    private final static String TAG = PrintConfirmationActivity.class.getName();

    private static final String Title = "OnTrac RD {1} - {0}";

    BaseApplication baseApp = null;

    Action.ScanType scanType = null;

    @BindView(R.id.uxcmdYesPrint)
    Button uxcmdYesPrint;

    @BindView(R.id.uxcmdNoPrint)
    Button uxcmdNoPrint;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        baseApp = ((BaseApplication)getApplicationContext());
        setContentView(R.layout.activity_print_confirmation);

        getSupportActionBar().setTitle(MessageFormat.format(Title, baseApp.authUser.FriendlyName(), baseApp.authUser.FacilityCode));
        ButterKnife.bind(this);
        CanScan = false;
    }

    @OnClick({R.id.uxcmdYesPrint})
    public void LoadSortLabel(final Button button) {
        final Toast toast = Notifications.ToastLong(getString(R.string.loading));
        Notifications.Vibrate();
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                Action.ScanType action = Action.ScanType.RD;
                Intent source = getIntent();
                int buttonBackgroundColor = source.getIntExtra("ButtonBackgroundColor", -1);
                int buttonForeColor = source.getIntExtra("ButtonForeColor", button.getTextColors().getDefaultColor());
                Intent intent = new Intent(BaseApplication.getAppContext(), SortLabelActivity.class);
                intent.putExtra("ScanType", action);
                intent.putExtra("ButtonBackgroundColor", buttonBackgroundColor);
                intent.putExtra("ButtonForeColor", buttonForeColor);
                intent.putExtra("ButtonText", "RD");
                startActivity(intent);
                toast.cancel();
            }
        });
    }

    @OnClick({R.id.uxcmdNoPrint})
    public void LoadSinglePrompt(final Button button) {
        final Toast toast = Notifications.ToastLong(getString(R.string.loading));
        Notifications.Vibrate();

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                Action.ScanType action = Action.ScanType.RD;
                Intent source = getIntent();
                int buttonBackgroundColor = source.getIntExtra("ButtonBackgroundColor", -1);
                int buttonForeColor = source.getIntExtra("ButtonForeColor", button.getTextColors().getDefaultColor());
                Intent intent = new Intent(BaseApplication.getAppContext(), SinglePromptActivity.class);
                intent.putExtra("ScanType", action);
                intent.putExtra("ButtonBackgroundColor", buttonBackgroundColor);
                intent.putExtra("ButtonForeColor", buttonForeColor);
                intent.putExtra("ButtonText", "RD");
                startActivity(intent);
                toast.cancel();
            }
        });
    }
}

