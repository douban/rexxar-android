package com.douban.rexxar.resourceproxy;

import android.text.TextUtils;

import com.douban.rexxar.resourceproxy.cache.AssetCache;
import com.douban.rexxar.resourceproxy.cache.CacheHelper;
import com.douban.rexxar.resourceproxy.network.HtmlHelper;
import com.douban.rexxar.resourceproxy.network.INetwork;
import com.douban.rexxar.resourceproxy.network.NetworkImpl;
import com.douban.rexxar.route.RouteManager;
import com.douban.rexxar.route.Routes;

import java.util.ArrayList;
import java.util.List;

/**
 * ResourceProxy负责资源管理，比如获取缓存的资源，写入缓存资源，请求线上资源
 *
 * Created by luanqian on 15/12/23.
 */
public class ResourceProxy {

    public static final String TAG = ResourceProxy.class.getSimpleName();

    private static ResourceProxy sInstance;
    private INetwork mNetwork;
    private List<String> mProxyHosts = new ArrayList<>();

    private ResourceProxy(){
        CacheHelper.getInstance().registerCache(AssetCache.getInstance());
    }
    public static ResourceProxy getInstance() {
        if (null == sInstance) {
            synchronized (ResourceProxy.class) {
                if (null == sInstance) {
                    sInstance = new ResourceProxy();
                }
            }
        }
        return sInstance;
    }

    public ResourceProxy enableCache(boolean enableCache) {
        CacheHelper.getInstance().enableCache(enableCache);
        return this;
    }

    /**
     * 预加载html
     */
    public void prepareHtmlFiles(Routes routes, RouteManager.RouteRefreshCallback callback) {
        HtmlHelper.prepareHtmlFiles(routes, callback);
    }

    public void clearCache() {
        CacheHelper.getInstance().clearCache();
    }

    public INetwork getNetwork() {
        if (null == mNetwork) {
            mNetwork = new NetworkImpl();
        }
        return mNetwork;
    }

    public void addProxyHosts(List<String> hosts) {
        if (null != hosts && !hosts.isEmpty()) {
            mProxyHosts.addAll(hosts);
        }
    }

    public List<String> getProxyHosts() {
        return mProxyHosts;
    }

}
