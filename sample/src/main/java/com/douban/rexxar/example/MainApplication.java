package com.douban.rexxar.example;

import android.app.Application;

import com.douban.rexxar.Rexxar;
import com.douban.rexxar.resourceproxy.ResourceProxy;
import com.douban.rexxar.resourceproxy.network.RexxarContainerAPIHelper;
import com.douban.rexxar.route.RouteManager;

import java.util.ArrayList;
import java.util.List;

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
        Rexxar.initialize(this);
        Rexxar.setDebug(BuildConfig.DEBUG);
        // 设置并刷新route
        RouteManager.getInstance().setRouteApi("https://raw.githubusercontent.com/douban/rexxar-web/master/example/dist/routes.json");
        RouteManager.getInstance().refreshRoute(null);
        // 设置需要代理的资源
        ResourceProxy.getInstance().addProxyHosts(PROXY_HOSTS);
        // 设置local api
        RexxarContainerAPIHelper.registerAPIs(FrodoContainerAPIs.sAPIs);
        // 设置自定义的OkHttpClient
        Rexxar.setOkHttpClient(new OkHttpClient().newBuilder()
                .retryOnConnectionFailure(true)
                .addNetworkInterceptor(new AuthInterceptor())
                .build());
        Rexxar.setHostUserAgent(" Rexxar/1.2.x com.douban.frodo/4.3 ");
    }

}
