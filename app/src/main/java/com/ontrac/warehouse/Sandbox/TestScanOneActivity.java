package com.ontrac.warehouse.Sandbox;

import android.os.Bundle;
import android.widget.EditText;

import com.ontrac.warehouse.R;
import com.ontrac.warehouse.Utilities.Notifications;
import com.ontrac.warehouse.Utilities.Zebra.SharedScannerActivity;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class TestScanOneActivity extends SharedScannerActivity {

    @BindView(R.id.editText)
    EditText editText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_scan_one);

        ButterKnife.bind(this);
    }

    @OnClick(R.id.button)
    protected void uxcmdLogin(){
        Notifications.Vibrate();
    }


    // called when scanner returns scan data
    @Override
    public void Scanned(final String data) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                editText.setText(data);
            }
        });
    }

}
