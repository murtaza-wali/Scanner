package com.ontrac.warehouse;

import android.Manifest;
import android.content.pm.ApplicationInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;

import com.ontrac.warehouse.Utilities.Notifications;
import com.ontrac.warehouse.Utilities.Preferences;
import com.ontrac.warehouse.Utilities.UX.General;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.List;
import java.util.concurrent.Callable;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import pub.devrel.easypermissions.EasyPermissions;

public class RecordAssetTagActivity extends AppCompatActivity {

//    private static final String Title = "Record Asset Tag";

    BaseApplication baseApp = null;

    @BindView(R.id.uxAssetTagPassword)
    EditText uxAssetTagPassword;
    @BindView(R.id.uxAssetTag)
    EditText uxAssetTag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_asset_tag);

        baseApp = ((BaseApplication)getApplicationContext());
//        boolean changingOrientation = baseApp.setOrientation(this);
//        if (!changingOrientation) {
            ButterKnife.bind(this);
            uxAssetTag.setText(baseApp.onTracDeviceId.GetAssetTag());
            getSupportActionBar().setTitle(getString(R.string.actionRecordAsset));
//        }
    }

    @OnClick(R.id.uxButtonSaveAssetTag)
    protected void uxcmdSave(){
        Save();
    }

    protected void Save(){
        String assetTagPassword = uxAssetTagPassword.getText().toString();
        String assetTag = uxAssetTag.getText().toString();
        if (!TextUtils.isEmpty(assetTagPassword)){
            if (assetTagPassword.toString().compareTo("1223334444")==0) {
                if (!TextUtils.isEmpty(assetTag)) {
                    //Preferences.Save(baseApp, BaseApplication.Preference.AssetTag, assetTag);
                    //baseApp.DeviceSerial = assetTag;
                    baseApp.onTracDeviceId.SetAssetTag(assetTag);
                    General.Alert(this,getString(R.string.recordAssetSuccess),getString(R.string.recordingAssetTag),AfterAlertFinishCallable());
                } else
                    General.Alert(this, getString(R.string.recordAssetError), getString(R.string.assetTagEmpty));
            }
            else {
                General.Alert(this, getString(R.string.recordAssetError), getString(R.string.invalidPassword));
                uxAssetTagPassword.setText("");
                uxAssetTagPassword.requestFocus();
            }
        }
        else
            General.Alert(this,getString(R.string.recordAssetError),getString(R.string.passwordEmpty));
    }

    protected Callable AfterAlertFinishCallable() {
        return new Callable() {
            public Object call() {
                AfterAlertFinish();
                return null;
            }

            ;
        };
    }

    protected void AfterAlertFinish()
    {
        System.exit(1);
    }

}