package com.douban.rexxar.view;

import android.app.Activity;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Patterns;
import android.webkit.ConsoleMessage;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import com.douban.rexxar.utils.LogUtils;

import java.util.regex.Matcher;

/**
 * Created by luanqian on 15/10/28.
 */
public class RexxarWebChromeClient extends WebChromeClient{

    static final String TAG = RexxarWebChromeClient.class.getSimpleName();

    @Override
    public void onReceivedTitle(WebView view, String title) {
        super.onReceivedTitle(view, title);
        if (TextUtils.isEmpty(title)) {
            return;
        }
        Matcher matcher = Patterns.WEB_URL.matcher(title);
        if (matcher.matches()) {
            return;
        }
        // 部分系统会优先用页面的location作为title，这种情况需要过滤掉
        if (Patterns.WEB_URL.matcher(title).matches()) {
            return;
        }
        // Hack：过滤掉rexxar页面
        if (title.contains(".html?uri=")) {
            return;
        }
        // 设置title
        if (view.getContext() instanceof Activity) {
            ((Activity) view.getContext()).setTitle(Uri.decode(title));
        }
    }

    @Override
    public void onGeolocationPermissionsShowPrompt(String origin,
                                                   GeolocationPermissions.Callback callback) {
        callback.invoke(origin, true, false);
    }

    @Override
    public void onProgressChanged(WebView view, int newProgress) {
        super.onProgressChanged(view, newProgress);
        LogUtils.i(TAG, "process is " + newProgress);
    }

    @Override
    public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
        LogUtils.i(TAG, consoleMessage.message());
        return super.onConsoleMessage(consoleMessage);
    }

    @Override
    public void onConsoleMessage(String message, int lineNumber, String sourceID) {
        super.onConsoleMessage(message, lineNumber, sourceID);
        LogUtils.i(TAG, message);
    }
}
