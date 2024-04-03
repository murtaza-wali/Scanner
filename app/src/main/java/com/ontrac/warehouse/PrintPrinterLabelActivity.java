package com.ontrac.warehouse;

import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.EditText;

import com.ontrac.warehouse.Utilities.Notifications;
import com.ontrac.warehouse.Utilities.UX.General;
import com.ontrac.warehouse.Utilities.LabelPrinterHelper;
import com.ontrac.warehouse.Utilities.Zebra.UIHelper;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class PrintPrinterLabelActivity extends AppCompatActivity {
//    private static final String Title = "Print Printer Label";

    private UIHelper helper = new UIHelper(this);

    private LabelPrinterHelper labelPrinterHelper = new LabelPrinterHelper(helper);

    BaseApplication baseApp = null;

    @BindView(R.id.uxPrinterIP)
    EditText uxPrinterIPAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_print_printer_label);

        baseApp = ((BaseApplication)getApplicationContext());

        boolean changingOrientation = baseApp.setOrientation(this);

        if (!changingOrientation) {
            ButterKnife.bind(this);

            getSupportActionBar().setTitle(getString(R.string.printPrinterLabel));
        }

    }

    @OnClick(R.id.buttonPrint)
    protected void uxcmdPrintPrinterLabel(){
        Notifications.Vibrate();
        if (Patterns.IP_ADDRESS.matcher(uxPrinterIPAddress.getText().toString()).matches()){
            final String zplString = "^XA^FO040,20^BY2^BCN,150,N,Y,N^FD"+uxPrinterIPAddress.getText().toString()+"^FS^FT40,200^A0N,35,35^FD"+uxPrinterIPAddress.getText().toString()+"^FS^XZ";
            new Thread(new Runnable() {
                public void run() {
                    Looper.prepare();
                    labelPrinterHelper.sendFile(zplString,uxPrinterIPAddress.getText().toString());
                    Looper.loop();
                    Looper.myLooper().quit();
                }
            }).start();

        } else {
            General.Alert(this, getString(R.string.printerIpError), getString(R.string.notIpAddress));
        }
    }

}
