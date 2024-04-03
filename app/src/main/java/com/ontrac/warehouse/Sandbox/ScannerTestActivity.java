package com.ontrac.warehouse.Sandbox;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.ontrac.warehouse.BaseApplication;
import com.ontrac.warehouse.R;
import com.ontrac.warehouse.Utilities.UX.HideKeyboard;
import com.ontrac.warehouse.Utilities.Zebra.Scanner;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import butterknife.OnFocusChange;
import butterknife.OnLongClick;

public class ScannerTestActivity extends AppCompatActivity implements Scanner.IScanListener {

    private com.ontrac.warehouse.Utilities.Zebra.Scanner _scanner = null;
    private HideKeyboard _hidekey;

    @BindView(R.id.editText)
    EditText editText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner_test);
        getSupportActionBar().setTitle("Scanner Test");
        ButterKnife.bind(this);

        BaseApplication baseApp = ((BaseApplication)getApplicationContext());

        _scanner = new com.ontrac.warehouse.Utilities.Zebra.Scanner(this, this, baseApp.EmdkProfile);

        _hidekey = new HideKeyboard(this);
    }

    @OnClick(R.id.uxcmdInit)
    protected void uxcmdInit(){
        _scanner.initialize();
    }

    @OnClick(R.id.uxcmdTerm)
    protected void uxcmdTerm(){
        _scanner.terminate();
    }


    @OnClick(R.id.editText)
    public void editText_Click() {
        if (!_scanner.isScannerConnected()) {
            _scanner.initialize();
        }
    }

    @OnFocusChange({R.id.editText})
    public void OnFocusChange(View view, boolean hasFocus) {
        _hidekey.OnFocusChange(view, hasFocus);
    }

    @OnLongClick({R.id.editText})
    public boolean OnLongClick(View view) {
        _hidekey.OnLongClick(view);
        return true; // hides the paste context popup
    }

    @OnEditorAction({R.id.editText})
    public boolean OnEditorAction(TextView view, int action) {
        _hidekey.OnEditorAction(view, action);
        return false;
    }


    /*
    @OnTextChanged(R.id.editText)
    void onTextChanged(CharSequence text) {
        Toast.makeText(this, "Text changed: " + text, Toast.LENGTH_SHORT).show();
    }
    */

    @Override
    public void Restart() {
        _scanner.destroy();
        _scanner = null;

        BaseApplication baseApp = ((BaseApplication)getApplicationContext());

        _scanner = new com.ontrac.warehouse.Utilities.Zebra.Scanner(this, this, baseApp.EmdkProfile);
    }

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
