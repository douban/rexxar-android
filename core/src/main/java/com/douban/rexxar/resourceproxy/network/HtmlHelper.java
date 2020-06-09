package com.douban.rexxar.resourceproxy.network;

import com.douban.rexxar.Constants;
import com.douban.rexxar.Rexxar;
import com.douban.rexxar.resourceproxy.cache.CacheEntry;
import com.douban.rexxar.resourceproxy.cache.CacheHelper;
import com.douban.rexxar.route.Route;
import com.douban.rexxar.route.RouteManager;
import com.douban.rexxar.route.Routes;
import com.douban.rexxar.utils.BusProvider;
import com.douban.rexxar.utils.GsonHelper;
import com.douban.rexxar.utils.LogUtils;
import com.douban.rexxar.utils.io.IOUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by luanqian on 15/10/29.
 */
public class HtmlHelper {

    public static final String TAG = HtmlHelper.class.getSimpleName();
    public static final List<String> mDownloadingProcess = new ArrayList<>();

    /**
     * 下载html文件
     *
     * @param url
     * @param callback
     */
    private static void doDownloadHtmlFile(String url, Callback callback) {
        LogUtils.i(TAG, "url = " + url);
        Request request = new Request.Builder().url(url)
                .addHeader("User-Agent", Rexxar.getUserAgent())
                .build();
        Rexxar.getOkHttpClient().newCall(request)
                .enqueue(callback);
    }

    /**
     * 下载html文件，然后缓存
     *
     * @param url
     * @param callback
     */
    public static void prepareHtmlFile(final String url, final Callback callback) {
        HtmlHelper.doDownloadHtmlFile(url, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (response.isSuccessful()) {
                        // 1. 存储到本地
                        byte[] data = IOUtils.toByteArray(response.body()
                                .byteStream());
                        boolean result = CacheHelper.getInstance().saveHtmlCache(url, data);
                        // 存储失败，则失败
                        if (!result) {
                            boolean checkUrl = CacheHelper.getInstance().checkUrl(url);
                            boolean checkHtmlFile = CacheHelper.getInstance().checkHtmlFile(url, data);
                            onFailure(call, new IOException("html file save fail! url:" + url +  " ; checkUrl:" + checkUrl + " ; checkHtmlFile" + checkHtmlFile));
                        } else {
                            if (null != callback) {
                                callback.onResponse(call, response);
                            }
                        }
                    } else {
                        onFailure(call, new IOException(String.valueOf(response.code())));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    onFailure(call, new IOException("file save fail!" + e.getMessage()));
                    LogUtils.i(TAG, "prepare html fail");
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                if (null != callback) {
                    callback.onFailure(call, e);
                }
            }
        });
    }

    /**
     * 空闲时间下载html文件
     * // FIXME 考虑并发问题
     */
    public static void prepareHtmlFiles(Routes routes, final RouteManager.RouteRefreshCallback callback) {
        if (null == routes || routes.isEmpty()) {
            return;
        }
        ArrayList<Route> validRoutes = new ArrayList<>();
        for (Route route : routes.items) {
            if (route.necessaryUpdate) {
                validRoutes.add(route);
            }
        }
        for (Route route : routes.partialItems) {
            if (route.necessaryUpdate) {
                validRoutes.add(route);
            }
        }
        // 重新下载
        mDownloadingProcess.clear();
        int totalSize = validRoutes.size();
        // 需要下载的route数量
        int newRouteCount = 0;
        if (Rexxar.DEBUG) {
            LogUtils.i(TAG, "routes:" + GsonHelper.getInstance().toJson(routes));
            LogUtils.i(TAG, "download total count:" + totalSize);
        }
        for (int i = 0; i < totalSize ; i ++) {
            final Route tempRoute = validRoutes.get(i);
            if (!CacheHelper.getInstance().hasHtmlCached(tempRoute.getHtmlFile())) {
                newRouteCount ++;
                if (!mDownloadingProcess.contains(tempRoute.getHtmlFile())) {
                    mDownloadingProcess.add(tempRoute.getHtmlFile());
                    HtmlHelper.prepareHtmlFile(tempRoute.getHtmlFile(), new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            // 如果下载失败，则不移除
                            LogUtils.i(TAG, "download html failed" + tempRoute.getHtmlFile() + e.getMessage());
                            // TODO 怀疑点1：下载失败
                            if (null != callback) {
                                callback.onHtmlFileCacheFail(e.getMessage());
                            }
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            // TODO 怀疑点2：移除失败
                            mDownloadingProcess.remove(tempRoute.getHtmlFile());
                            LogUtils.i(TAG, "download html success " + tempRoute.getHtmlFile());
                            // 如果全部文件下载成功，则发送校验成功事件
                            if (mDownloadingProcess.isEmpty()) {
                                // TODO 怀疑点3：没有调用到这，或者没人接收
                                LogUtils.i(TAG, "download html complete");
                                BusProvider.getInstance().post(new BusProvider.BusEvent(Constants.BUS_EVENT_ROUTE_CHECK_VALID, null));
                            }
                        }
                    });
                }
            } else {
                LogUtils.i(TAG, "download exist " + tempRoute.getHtmlFile());
                // 如果所有html文件都已经缓存了,也可以更新route
                if (newRouteCount == 0 && i == totalSize - 1) {
                    BusProvider.getInstance().post(new BusProvider.BusEvent(Constants.BUS_EVENT_ROUTE_CHECK_VALID, null));
                }
            }
        }
        LogUtils.i(TAG, "download new count:" + newRouteCount);
    }
}
