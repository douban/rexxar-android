package com.douban.rexxar.utils;

import android.content.Context;
import android.os.Build;
import android.view.accessibility.AccessibilityManager;
import android.webkit.WebSettings;

import java.lang.reflect.Method;

/**
 * Created by luanqian on 16/12/22.
 */

public class WebViewCompatUtils {

    /**
     * fix bug: NPE in android.webkit.AccessibilityInjector$TextToSpeechWrapper (Android 4.2.1)
     * see: https://code.google.com/p/android/issues/detail?id=40944
     * 
     * @param context
     * @param webSettings
     */
    public static void enableJavaScriptForWebView(Context context, WebSettings webSettings) {
        if (null == webSettings) {
            return;
        }
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR1) {
            disableAccessibility(context);
        }
        try {
            webSettings.setJavaScriptEnabled(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * m:lorss
     * 关闭辅助功能，针对4.2.1和4.2.2 崩溃问题
     * java.lang.NullPointerException
     * at android.webkit.AccessibilityInjector$TextToSpeechWrapper$1.onInit(AccessibilityInjector.java:753)
     * ... ...
     * at android.webkit.CallbackProxy.handleMessage(CallbackProxy.java:321)
     */
    private static void disableAccessibility(Context context) {
        if (Build.VERSION.SDK_INT == 17/*4.2 (Build.VERSION_CODES.JELLY_BEAN_MR1)*/) {
            if (context != null) {
                try {
                    AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
                    if (!am.isEnabled()) {
                        //Not need to disable accessibility
                        return;
                    }

                    Method setState = am.getClass().getDeclaredMethod("setState", int.class);
                    setState.setAccessible(true);
                    setState.invoke(am, 0);/**{@link AccessibilityManager#STATE_FLAG_ACCESSIBILITY_ENABLED}*/
                } catch (Exception ignored) {
                    ignored.printStackTrace();
                }
            }
        }
    }
}
