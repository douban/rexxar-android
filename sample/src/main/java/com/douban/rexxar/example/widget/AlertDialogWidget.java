package com.douban.rexxar.example.widget;

import android.app.Activity;
import android.content.DialogInterface;
import android.net.Uri;
import android.support.annotation.Keep;
import android.text.TextUtils;
import android.webkit.WebView;

import com.douban.rexxar.utils.GsonHelper;
import com.douban.rexxar.view.RexxarWidget;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by luanqian on 16/4/28.
 */
public class AlertDialogWidget implements RexxarWidget {

    static final String KEY = "data";
    static boolean sShowing = false;

    @Override
    public String getPath() {
        return "/widget/alert_dialog";
    }

    @Override
    public boolean handle(WebView view, String url) {
        if (TextUtils.isEmpty(url)) {
            return false;
        }
        Uri uri = Uri.parse(url);
        String path = uri.getPath();
        if (TextUtils.equals(path, getPath())) {
            String data = uri.getQueryParameter(KEY);
            Data alertDialogData = null;
            if (!TextUtils.isEmpty(data)) {
                alertDialogData = GsonHelper.getInstance().fromJson(data, Data.class);
            }
            if (null == alertDialogData || !alertDialogData.valid()) {
                return false;
            }

            return renderDialog((Activity) view.getContext(), view, alertDialogData);
        }
        return false;
    }


    /**
     * 根据dialog数据显示dialog
     *
     * @param data
     * @return
     */
    private static boolean renderDialog(Activity activity, final WebView webView, Data data) {
        if (null == data) {
            return false;
        }
        // 正在显示dialog，则不再显示
        if (sShowing) {
            return false;
        }
        if (activity.isFinishing()) {
            return false;
        }
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(activity)
                .setTitle(data.title)
                .setMessage(data.message)
                // 不可消失
                .setCancelable(false)
                // dialog消失后，sShowing设置为false
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        sShowing = false;
                    }
                });
        switch (data.buttons.size()) {
            // 一个button
            case 1: {
                final Button positive = data.buttons.get(0);
                builder.setPositiveButton(positive.text, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        webView.loadUrl("javascript:" + positive.action);
                    }
                });
                break;
            }
            // 两个button
            case 2: {
                final Button positive = data.buttons.get(1);
                final Button negative = data.buttons.get(0);
                builder.setPositiveButton(positive.text, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        webView.loadUrl("javascript:" + positive.action);
                    }
                });
                builder.setNegativeButton(negative.text, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        webView.loadUrl("javascript:" + negative.action);
                    }
                });
                break;
            }
            // 3个button
            case 3: {
                final Button positive = data.buttons.get(2);
                final Button negative = data.buttons.get(0);
                final Button neutral = data.buttons.get(1);
                builder.setPositiveButton(positive.text, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        webView.loadUrl("javascript:" + positive.action);
                    }
                });
                builder.setNegativeButton(negative.text, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        webView.loadUrl("javascript:" + negative.action);
                    }
                });
                builder.setNeutralButton(neutral.text, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        webView.loadUrl("javascript:" + neutral.action);
                    }
                });
                break;
            }
            default:{
                return false;
            }
        }
        builder.create().show();
        sShowing = true;
        return true;
    }

    @Keep
    static class Button {
        String text;
        String action;

        public Button() {}
    }

    @Keep
    static class Data {
        String title;
        String message;
        List<Button> buttons = new ArrayList<>();

        public Data(){}

        public boolean valid() {
            if (TextUtils.isEmpty(message)) {
                return false;
            }
            if (null == buttons || buttons.size() == 0) {
                return false;
            }
            return true;
        }
    }
}
