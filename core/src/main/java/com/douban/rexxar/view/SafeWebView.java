package com.douban.rexxar.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebView;

import com.douban.rexxar.utils.Utils;


/**
 * 解决Android 4.2以下的WebView注入Javascript对象引发的安全漏洞
 *
 * Created by luanqian on 15/10/28.
 */
public class SafeWebView extends WebView {

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
}
