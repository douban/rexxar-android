package com.douban.rexxar.view;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.AndroidRuntimeException;
import android.util.AttributeSet;
import android.webkit.WebView;
import android.widget.Toast;

import com.douban.rexxar.R;
import com.douban.rexxar.utils.AppContext;
import com.douban.rexxar.utils.Utils;

import java.util.Map;


/**
 * 解决Android 4.2以下的WebView注入Javascript对象引发的安全漏洞
 *
 * Created by luanqian on 15/10/28.
 */
public class SafeWebView extends NestedWebView {

    protected boolean mIsDestroy = false;

    public SafeWebView(Context context) {
        super(context);
        removeSearchBoxJavaBridgeInterface();
    }

    public SafeWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        removeSearchBoxJavaBridgeInterface();
    }

    public SafeWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        removeSearchBoxJavaBridgeInterface();
    }

    @SuppressLint("NewApi")
    private void removeSearchBoxJavaBridgeInterface() {
        if (Utils.hasHoneycomb() && !Utils.hasJellyBeanMR1()) {
            removeJavascriptInterface("searchBoxJavaBridge_");
        }
    }

    @Override
    public void destroy() {
        mIsDestroy = true;
        super.destroy();
    }

    @Override
    public void loadUrl(String url) {
        if (mIsDestroy) {
            return;
        }
        super.loadUrl(url);
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        try {
            super.onWindowVisibilityChanged(visibility);
        } catch (AndroidRuntimeException e) {
            String message = e.getMessage();
            if (!TextUtils.isEmpty(message) && message.contains("Failed to load WebView provider")) {
                // WebView missing, toast & finish activity
                Toast.makeText(AppContext.getInstance(), R.string.webview_missing, Toast.LENGTH_SHORT).show();
                if (null != getContext() && getContext() instanceof Activity) {
                    ((Activity) getContext()).finish();
                }
            }
        }
    }

    @Override
    public void setOverScrollMode(int mode) {
        try {
            super.setOverScrollMode(mode);
        } catch (AndroidRuntimeException e) {
            String message = e.getMessage();
            if (!TextUtils.isEmpty(message) && message.contains("Failed to load WebView provider")) {
                // WebView missing, toast & finish activity
                Toast.makeText(AppContext.getInstance(), R.string.webview_missing, Toast.LENGTH_SHORT).show();
                if (null != getContext() && getContext() instanceof Activity) {
                    ((Activity) getContext()).finish();
                }
            }
        }
    }

    @Override
    public void loadUrl(String url, Map<String, String> additionalHttpHeaders) {
        if (mIsDestroy) {
            return;
        }
        super.loadUrl(url, additionalHttpHeaders);
    }
}
