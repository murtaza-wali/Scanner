package com.ontrac.warehouse;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.Toast;

import com.ontrac.warehouse.Utilities.Notifications;
import com.ontrac.warehouse.Utilities.UX.Orientation;
import com.ontrac.warehouse.Utilities.Zebra.SharedScannerActivity;

import java.text.MessageFormat;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.ontrac.warehouse.OnTrac.Utilities.ValidateUser;

public class TrailerMgmtActivity extends SharedScannerActivity {
    private static final String Title = "Trailer Management {1} - {0}";
    BaseApplication baseApp = null;

    @BindView(R.id.uxcmdOpen)
    Button uxcmdOpen;

    @BindView(R.id.uxcmdClose)
    Button uxcmdClose;

    @BindView(R.id.uxcmdUnload)
    Button uxcmdUnload;

    @BindView(R.id.uxcmdUnloadComplete)
    Button uxcmdUnloadComplete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        baseApp = ((BaseApplication)getApplicationContext());

        boolean changingOrientation = baseApp.setOrientation(this);

        if (!changingOrientation) {
            ValidateUser(this, baseApp.authUser);

//            setContentView(R.layout.activity_trailermgmtmenu_portrait);

            if (Orientation.isLandscape(this)) {
                setContentView(R.layout.activity_trailermgmtmenu_portrait);
            } else {
                setContentView(R.layout.activity_trailermgmtmenu_portrait);
            }

            getSupportActionBar().setTitle(MessageFormat.format(Title, baseApp.authUser.FriendlyName(), baseApp.authUser.FacilityCode));
            ButterKnife.bind(this);

            CanScan = false;

        }
    }

    @OnClick({R.id.uxcmdOpen})
    public void LoadOpenMenu(final Button button) {
        final Toast toast = Notifications.ToastLong(getString(R.string.loading));

        Notifications.Vibrate();

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                int backgroundColor = ((ColorDrawable)button.getBackground()).getColor();
                String buttonText = button.getText().toString();

                Intent intent = new Intent(BaseApplication.getAppContext(), TrailerOpenActivity.class);

                intent.putExtra("ButtonBackgroundColor", backgroundColor);
                intent.putExtra("ButtonText", buttonText);
                intent.putExtra("ButtonForeColor", button.getTextColors().getDefaultColor());

                startActivity(intent);

                toast.cancel();
            }
        });
    }

    @OnClick({R.id.uxcmdClose})
    public void LoadTrailerMgmtCloseMenu(final Button button) {
        final Toast toast = Notifications.ToastLong(getString(R.string.loading));

        Notifications.Vibrate();

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                ;
                int backgroundColor = ((ColorDrawable)button.getBackground()).getColor();
                String buttonText = button.getText().toString();

                Intent intent = new Intent(BaseApplication.getAppContext(), TrailerCloseActivity.class);

//                intent.putExtra("ScanType", action);
                intent.putExtra("ButtonBackgroundColor", backgroundColor);
                intent.putExtra("ButtonText", buttonText);
                intent.putExtra("ButtonForeColor", button.getTextColors().getDefaultColor());

                startActivity(intent);

                toast.cancel();
            }
        });
    }

    @OnClick({R.id.uxcmdUnload})
    public void LoadTrailerMgmtUnloadMenu(final Button button) {
        final Toast toast = Notifications.ToastLong(getString(R.string.loading));

        Notifications.Vibrate();

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                ;
                int backgroundColor = ((ColorDrawable)button.getBackground()).getColor();
                String buttonText = button.getText().toString();

                Intent intent = new Intent(BaseApplication.getAppContext(), TrailerUnloadActivity.class);

//                intent.putExtra("ScanType", action);
                intent.putExtra("ButtonBackgroundColor", backgroundColor);
                intent.putExtra("ButtonText", buttonText);
                intent.putExtra("ButtonForeColor", button.getTextColors().getDefaultColor());

                startActivity(intent);

                toast.cancel();
            }
        });
    }

    @OnClick({R.id.uxcmdUnloadComplete})
    public void LoadTrailerMgmtUnloadCompleteMenu(final Button button) {
        final Toast toast = Notifications.ToastLong(getString(R.string.loading));

        Notifications.Vibrate();

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                ;
                int backgroundColor = ((ColorDrawable)button.getBackground()).getColor();
                String buttonText = button.getText().toString();

                Intent intent = new Intent(BaseApplication.getAppContext(), TrailerUnloadCompleteActivity.class);

//                intent.putExtra("ScanType", action);
                intent.putExtra("ButtonBackgroundColor", backgroundColor);
                intent.putExtra("ButtonText", buttonText);
                intent.putExtra("ButtonForeColor", button.getTextColors().getDefaultColor());

                startActivity(intent);

                toast.cancel();
            }
        });
    }


}