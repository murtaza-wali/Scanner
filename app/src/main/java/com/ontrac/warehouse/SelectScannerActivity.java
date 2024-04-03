package com.ontrac.warehouse;

import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemSelectedListener;

import com.ontrac.warehouse.Utilities.UX.General;
import com.ontrac.warehouse.Utilities.Zebra.SharedScannerActivity;
import com.symbol.emdk.EMDKManager;
import com.symbol.emdk.barcode.ScannerInfo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnItemSelected;

import static android.widget.AdapterView.*;
import static com.ontrac.warehouse.OnTrac.Utilities.ValidateUser;

    public class SelectScannerActivity extends SharedScannerActivity {

//    private static final String Title = "Select Scan Device";
    private int scannerIndex = 0; // Keep the selected scanner
    private int defaultIndex = 0; // Keep the default scanner
    private List<ScannerInfo> deviceList = null;
    private List<ScannerInfo> connectedDeviceList = new ArrayList<ScannerInfo>();

    BaseApplication baseApp = null;

    @BindView(R.id.uxSpinnerScanDevices)
    Spinner uxSpinnerScanDevices;
    @BindView(R.id.uxButtonSave)
    Button uxButtonSave;
    @BindView(R.id.uxCurrentScanner)
    EditText uxCurrentScanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_scanner);

        baseApp = ((BaseApplication)getApplicationContext());
        boolean changingOrientation = baseApp.setOrientation(this);

 //       if (!changingOrientation) {
            ValidateUser(this, baseApp.authUser);
            ButterKnife.bind(this);
            getSupportActionBar().setTitle(getString(R.string.selectScanDevice));
            uxCurrentScanner.setEnabled(false);
            uxCurrentScanner.setTextColor(ContextCompat.getColor(this, R.color.red));
            String test  = baseApp.SharedScanner._scanner.getScannerInfo().getFriendlyName();
            uxCurrentScanner.setText(test);
            enumerateScannerDevices();
            // Set default scanner
            uxSpinnerScanDevices.setSelection(defaultIndex);
 //       }
    }

    @Override
    protected void onStart(){
/*
        String test  = baseApp.SharedScanner._scanner.getScannerInfo().getFriendlyName();
        uxCurrentScanner.setText(test);
        enumerateScannerDevices();
        // Set default scanner
        uxSpinnerScanDevices.setSelection(defaultIndex);
 */
        super.onStart();
    }


    @OnClick(R.id.uxButtonSave)
    protected void uxcmdSave(){
        baseApp.ScanDevice = connectedDeviceList.get(scannerIndex);
        Restart();
        General.Alert(this,getString(R.string.selectDeviceSuccess), getString(R.string.setScanDevice)+baseApp.ScanDevice.getFriendlyName()+".",AfterSuccessAlertCallable());
//        this.finish();
    }

        @OnItemSelected(R.id.uxSpinnerScanDevices)
        public void spinnerItemSelected(Spinner spinner, int position) {
            // code here
            scannerIndex = position;
        }


        private void enumerateScannerDevices() {
            if (baseApp.SharedScanner._barcodeManager != null) {
                List<String> friendlyNameList = new ArrayList<String>();
                int spinnerIndex = 0;
                deviceList = baseApp.SharedScanner._barcodeManager.getSupportedDevicesInfo();
                if ((deviceList != null) && (deviceList.size() != 0)) {
                    Iterator<ScannerInfo> it = deviceList.iterator();
                    while(it.hasNext()) {
                        ScannerInfo scnInfo = it.next();
                        if (scnInfo.isConnected()) {
                            connectedDeviceList.add(scnInfo);
                            friendlyNameList.add(scnInfo.getFriendlyName());
                            ++spinnerIndex;
                        }
                    }
                }
                else {
                    Callable doAfterErrorAlert = new Callable() {
                        public Object call() {
                            AfterErrorAlert();
                            return null;
                        }
                    };
                    General.Alert(this, getString(R.string.selectDeviceError), getString(R.string.restartAppScanning), doAfterErrorAlert);
                }
                ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>( this, android.R.layout.simple_spinner_item, friendlyNameList);
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                uxSpinnerScanDevices.setAdapter(spinnerAdapter);
            }
        }

        private void AfterErrorAlert(){
            this.finish();
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
            this.finish();
        }
    }