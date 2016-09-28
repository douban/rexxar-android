package com.douban.rexxar.example.widget;

import android.net.Uri;
import android.text.TextUtils;
import android.webkit.WebView;
import android.widget.Toast;

import com.douban.rexxar.utils.LogUtils;
import com.douban.rexxar.view.RexxarWidget;

/**
 * Created by luanqian on 15/11/24.
 */
public class ToastWidget implements RexxarWidget {

    static final String TAG = ToastWidget.class.getSimpleName();

    static final String KEY_MESSAGE = "message";
    static final String KEY_LEVEL = "level";

    @Override
    public String getPath() {
        return "/widget/toast";
    }

    @Override
    public boolean handle(WebView view, String url) {
        if (TextUtils.isEmpty(url)) {
            return false;
        }
        Uri uri = Uri.parse(url);
        if (TextUtils.equals(uri.getPath(), getPath())) {
            String message = Uri.decode(uri.getQueryParameter(KEY_MESSAGE));
            String level = uri.getQueryParameter(KEY_LEVEL);
            Toast.makeText(view.getContext(), message, Toast.LENGTH_LONG).show();
            LogUtils.i(TAG,
                    String.format("handle toast success, message = %1$s ", message));
            return true;
        }
        return false;
    }
}
