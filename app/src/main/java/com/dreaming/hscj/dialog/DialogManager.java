package com.dreaming.hscj.dialog;

import android.app.Activity;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dreaming.hscj.R;
import com.dreaming.hscj.base.BaseActivity;
import com.dreaming.hscj.utils.DensityUtils;
import com.dreaming.hscj.utils.ToastUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import priv.songxusheng.easydialog.EasyDialog;
import priv.songxusheng.easydialog.EasyDialogHolder;

public class DialogManager {
    public static void showAlertDialogWithConfirm(Activity activity, String alertContent, DialogInterface.OnDismissListener listener){
        showAlertDialogWithConfirm(activity,"通知", alertContent, listener);
    }

    public static void showAlertDialogWithConfirm(Activity activity,String tilte, String alertContent, DialogInterface.OnDismissListener listener){
        new EasyDialog(R.layout.dialog_common_alert,activity)
                .setOnBindDialogListener(new EasyDialog.OnBindDialogListener() {
                    @Override
                    public void onBindDialog(EasyDialogHolder easyDialogHolder) {
                        easyDialogHolder.setText(R.id.tv_title,tilte);
                        easyDialogHolder.setText(R.id.tv_content,alertContent);

                        easyDialogHolder.setOnClickListener(R.id.tv_confirm,v -> {
                            easyDialogHolder.dismissDialog();
                            if(listener!=null) listener.onDismiss(easyDialogHolder.getDialog());
                        });
                    }
                })
                .setDialogHeight((int) DensityUtils.dp2px(150f))
                .setForegroundResource(R.drawable.shape_common_dialog)
                .setAllowDismissWhenBackPressed(false)
                .setAllowDismissWhenTouchOutside(false)
                .showDialog();
    }

    public static void showAlertDialog(Activity activity, String title, String content, View.OnClickListener onCancelClicked, View.OnClickListener onConfirmClicked){
        final AtomicBoolean bIsConfirmClicked = new AtomicBoolean(false);
        new EasyDialog(R.layout.dialog_common_alert_with_2_btn,activity)
                .setOnBindDialogListener(new EasyDialog.OnBindDialogListener() {
                    @Override
                    public void onBindDialog(EasyDialogHolder easyDialogHolder) {
                        easyDialogHolder.setText(R.id.tv_title,title);
                        easyDialogHolder.setText(R.id.tv_content,content);
                        easyDialogHolder.setOnClickListener(R.id.tv_cancel, v->{
                            bIsConfirmClicked.set(true);
                            easyDialogHolder.dismissDialog();
                            if(onCancelClicked!=null) onCancelClicked.onClick(v);
                        });

                        easyDialogHolder.setOnClickListener(R.id.tv_confirm,v->{
                            bIsConfirmClicked.set(true);
                            easyDialogHolder.dismissDialog();
                            if(onConfirmClicked!=null)
                                onConfirmClicked.onClick(v);
                        });
                    }
                })
                .setDialogHeight((int) DensityUtils.dp2px(150f))
                .setForegroundResource(R.drawable.shape_common_dialog)
                .setAllowDismissWhenBackPressed(false)
                .setAllowDismissWhenTouchOutside(false)
                .showDialog();
    }

    public static final int INPUT_TYPE_NUMBER = InputType.TYPE_CLASS_NUMBER;
    public static final int INPUT_TYPE_TEXT   = InputType.TYPE_CLASS_TEXT;
    public static void showInputDialog(BaseActivity activity, String digits, int inputType, String shownText, String initText, String confirmText, View.OnClickListener onConfirmClicked){
        new EasyDialog(R.layout.dialog_input,activity)
                .setOnBindDialogListener(new EasyDialog.OnBindDialogListener() {
                    @Override
                    public void onBindDialog(EasyDialogHolder easyDialogHolder) {
                        LinearLayout llRoot    = easyDialogHolder.getView(R.id.ll_input_replace_content);
                        TextView     tvName    = llRoot.findViewById(R.id.tv_input_replace_content_name);
                        EditText     etInput   = llRoot.findViewById(R.id.et_input_replace_content_input);
                        TextView     tvMoveL   = llRoot.findViewById(R.id.tv_input_replace_content_last);
                        TextView     tvMoveR   = llRoot.findViewById(R.id.tv_input_replace_content_next);
                        TextView     tvClear   = llRoot.findViewById(R.id.tv_input_replace_content_clear);
                        TextView     tvConfirm = llRoot.findViewById(R.id.tv_input_replace_content_confirm);
                        View         vIntercept= llRoot.findViewById(R.id.v_intercept);
                        vIntercept.setOnClickListener(v -> {});

                        tvMoveL.setOnClickListener(v -> {
                            int iIndex = etInput.getSelectionStart();
                            if(iIndex<1) return;
                            etInput.setSelection(iIndex-1);
                        });

                        tvMoveR.setOnClickListener(v -> {
                            int iIndex = etInput.getSelectionStart();
                            int length = etInput.getText().toString().length();
                            if(iIndex+1<length){
                                etInput.setSelection(iIndex+1);
                            }
                        });

                        if(digits!=null && !digits.isEmpty()){
                            try {
                                Field fEditor = TextView.class.getField("mEditor");
                                fEditor.setAccessible(true);
                                Object oEditor= fEditor.get(etInput);
                                Method mGetInstance = DigitsKeyListener.class.getMethod("getInstance",new Class[]{String.class});
                                Class cEditor = Class.forName("android.widget.Editor");
                                Field fEditorKeyListener = cEditor.getField("mKeyListener");
                                fEditorKeyListener.setAccessible(true);
                                fEditorKeyListener.set(oEditor,mGetInstance.invoke(null,digits));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        if(inputType>0){
                            etInput.setInputType(inputType);
                        }

                        if(shownText == null || shownText.isEmpty()){
                            tvName.setVisibility(View.GONE);
                        }
                        else{
                            tvName.setVisibility(View.VISIBLE);
                            tvName.setText(shownText);
                        }

                        tvConfirm.setText(confirmText);

                        tvClear.setOnClickListener(v -> etInput.setText(""));

                        tvConfirm.setOnClickListener(v -> {
                            easyDialogHolder.dismissDialog();
                            onConfirmClicked.onClick(etInput);
                            activity.hideKeyboard();
                        });

                        activity.showKeyboard(etInput);

                        if(initText!=null){
                            etInput.setText(initText);
                            etInput.setSelection(initText.length());
                        }
                    }
                })
                .setDialogParams(-1,(int) DensityUtils.dp2px(36f), Gravity.BOTTOM)
                .setAllowDismissWhenTouchOutside(false)
                .setAllowDismissWhenBackPressed(false)
                .setForegroundResource(R.drawable.shape_input_dialog)
                .showDialog();
    }
}
