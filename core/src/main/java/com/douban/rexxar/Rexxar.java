package com.douban.rexxar;

import android.content.Context;
import android.text.TextUtils;

import com.douban.rexxar.resourceproxy.ResourceProxy;
import com.douban.rexxar.route.RouteManager;
import com.douban.rexxar.utils.AppContext;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

/**
 * Created by luanqian on 15/10/28.
 */
public class Rexxar {

    public static final String TAG = Rexxar.class.getSimpleName();
    public static boolean DEBUG = false;

    /**
     * 可以额外设置主app的user-agent
     */
    private static String mHostUserAgent;

    /**
     * 可以通过设置OkHttpClient的方式实现共用
     */
    private static OkHttpClient mOkHttpClient;

    public static void initialize(final Context context, boolean asyncLoadRoute, String hostUserAgent, OkHttpClient okHttpClient, RouteManager.RouteConfig config) {
        AppContext.init(context);
        RouteManager.config(config);
        setHostUserAgent(hostUserAgent);
        setOkHttpClient(okHttpClient);
        RouteManager.getInstance(asyncLoadRoute);
        ResourceProxy.getInstance();
    }

    public static void setDebug(boolean debug) {
        DEBUG = debug;
    }

    /**
     * 设置额外的User-Agent信息，在发起请求的时候会带上
     *
     * @param hostUserAgent
     */
    public static void setHostUserAgent(String hostUserAgent) {
        mHostUserAgent = hostUserAgent;
    }

    /**
     * 获取User-Agent信息，在发起请求的时候会带上
     *
     * @return
     */
    public static String getUserAgent() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Rexxar-Core/").append(BuildConfig.VERSION_NAME);
        if (!TextUtils.isEmpty(mHostUserAgent)) {
            stringBuilder.append(" ").append(mHostUserAgent);
        }
        return stringBuilder.toString();
    }

    public static void setOkHttpClient(OkHttpClient okHttpClient) {
        if (null != okHttpClient) {
            mOkHttpClient = okHttpClient;
        }
    }

    public static OkHttpClient getOkHttpClient() {
        if (null == mOkHttpClient) {
            mOkHttpClient = new OkHttpClient.Builder()
                    .retryOnConnectionFailure(false)
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .writeTimeout(10, TimeUnit.SECONDS)
                    .build();
        }
        return mOkHttpClient;
    }
}
