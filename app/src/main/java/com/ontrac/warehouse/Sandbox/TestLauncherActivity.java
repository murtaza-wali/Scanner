package com.ontrac.warehouse.Sandbox;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;

import com.ontrac.warehouse.R;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class TestLauncherActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_launcher);

        ButterKnife.bind(this);
    }

    @OnClick({R.id.button1})
    public void button1(Button button) {
        Intent intent = new Intent(this, TestScanOneActivity.class);
        startActivity(intent);
    }

    @OnClick({R.id.button2})
    public void button2(Button button) {
        Intent intent = new Intent(this, TestScanTwoActivity.class);
        startActivity(intent);
    }

}
