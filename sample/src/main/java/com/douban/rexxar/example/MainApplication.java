package com.douban.rexxar.example;

import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.douban.rexxar.Rexxar;
import com.douban.rexxar.resourceproxy.ResourceProxy;
import com.douban.rexxar.route.RouteManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.OkHttpClient;

/**
 * Created by luanqian on 15/11/2.
 */
public class MainApplication extends Application {

    static final List<String> PROXY_HOSTS = new ArrayList<>();
    static {
        PROXY_HOSTS.add("raw.githubusercontent.com");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // 初始化rexxar
        Rexxar.initialize(this, true, " Rexxar/1.2.x com.douban.frodo/4.3 ", new OkHttpClient().newBuilder()
                .retryOnConnectionFailure(true)
                .addNetworkInterceptor(new AuthInterceptor())
                .build(), new RouteManager.RouteConfig("https://raw.githubusercontent.com/douban/rexxar-web/master/example/dist/routes.json", getRouteCacheFileName()));
        Rexxar.setDebug(BuildConfig.DEBUG);
        RouteManager.getInstance().refreshRoute(null);
        // 设置需要代理的资源
        ResourceProxy.getInstance().addProxyHosts(PROXY_HOSTS);
    }

    public String getRouteCacheFileName() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            if (null != info) {
                return String.format(Locale.getDefault(), "routes_%s.json", info.versionName);
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return "routes.json";
    }
}
