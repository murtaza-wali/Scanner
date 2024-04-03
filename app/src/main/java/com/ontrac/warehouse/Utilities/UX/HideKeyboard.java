package com.ontrac.warehouse.Utilities.UX;

import android.app.Activity;
import android.content.Context;
import android.text.InputType;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import java.util.HashMap;
import java.util.Map;

public class HideKeyboard {

    private Activity _activity;
    private View _lastLongClick = null;
    private Map<View, Integer> _views;

    public HideKeyboard(Activity activity) {
        this._activity = activity;
        _views = new HashMap();
    }

    private void Hide(View view){
        if (view != null) {
            if (view instanceof EditText) {
                EditText et = (EditText) view;

                et.setInputType(InputType.TYPE_NULL);
                et.setCursorVisible(false);
            }

            InputMethodManager imm = (InputMethodManager)_activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void StoreInputState(View view) {
        if (!_views.containsKey(view))
        {
            if (view instanceof EditText) {
                EditText et = (EditText) view;

                if (et.getInputType() != InputType.TYPE_NULL) {
                    _views.put(view, et.getInputType());
                }
            }
        }
    }

    private void Show(View view){
        if (view != null) {
            //view.setFocusable(true);
            //view.setFocusableInTouchMode(true);
            //view.requestFocus();

            if (_views.containsKey(view))
            {
                if (view instanceof EditText) {
                    EditText et = (EditText)view;

                    int inputType = _views.get(view);
                    et.setInputType(inputType);

                    et.setImeOptions(EditorInfo.IME_ACTION_DONE);

                    et.setCursorVisible(true);
                }
            }

            InputMethodManager imm = (InputMethodManager)_activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(view, 0);
        }
    }

    public void OnFocusChange(View view, boolean hasFocus) {
        StoreInputState(view);

        if (view instanceof EditText) {
            if (hasFocus) {
                Hide(view);
            }

            if (_lastLongClick == view){
                Show(view);
                _lastLongClick = null;
            }
        }
    }

    public void OnLongClick(View view){
        StoreInputState(view);
        _lastLongClick = view;
        Show(view);
    }

    public void OnEditorAction(View view, int action) {
        if (action == EditorInfo.IME_ACTION_DONE){
            if (view instanceof EditText) {
                EditText et = (EditText) view;
                et.setCursorVisible(false);
            }
        }
    }

}