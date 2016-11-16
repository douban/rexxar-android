package com.douban.rexxar.resourceproxy.network;

import com.douban.rexxar.Constants;
import com.douban.rexxar.Rexxar;
import com.douban.rexxar.resourceproxy.cache.CacheEntry;
import com.douban.rexxar.resourceproxy.cache.CacheHelper;
import com.douban.rexxar.route.Route;
import com.douban.rexxar.route.Routes;
import com.douban.rexxar.utils.BusProvider;
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
                        boolean result = CacheHelper.getInstance().saveHtmlCache(url, IOUtils.toByteArray(response.body()
                                .byteStream()));
                        // 存储失败，则失败
                        if (!result) {
                            onFailure(call, new IOException("file save fail!"));
                            return;
                        }
                    }
                    // 2. 通知外面去查找
                    if (null != callback) {
                        callback.onResponse(call, response);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    onFailure(call, new IOException("file save fail!"));
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
     */
    public static void prepareHtmlFiles(Routes routes) {
        if (null == routes || routes.isEmpty()) {
            return;
        }
        ArrayList<Route> validRoutes = new ArrayList<>();
        validRoutes.addAll(routes.items);
        validRoutes.addAll(routes.partialItems);
        // 重新下载
        mDownloadingProcess.clear();
        for (final Route route : validRoutes) {
            CacheEntry htmlFile = CacheHelper.getInstance().findHtmlCache(route.getHtmlFile());
            if (null == htmlFile) {
                if (!mDownloadingProcess.contains(route.getHtmlFile())) {
                    mDownloadingProcess.add(route.getHtmlFile());
                    HtmlHelper.prepareHtmlFile(route.getHtmlFile(), new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            // 如果下载失败，则不移除
//                            mDownloadingProcess.remove(route.getHtmlFile());
                            LogUtils.i(TAG, "download html failed" + e.getMessage());
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            mDownloadingProcess.remove(route.getHtmlFile());
                            LogUtils.i(TAG, "download html success");
                            // 如果全部文件下载成功，则发送校验成功事件
                            if (mDownloadingProcess.isEmpty()) {
                                LogUtils.i(TAG, "download html complete");
                                BusProvider.getInstance().post(new BusProvider.BusEvent(Constants.BUS_EVENT_ROUTE_CHECK_VALID, null));
                            }
                        }
                    });
                }
            } else {
                htmlFile.close();
                if (mDownloadingProcess.isEmpty()) {
                    BusProvider.getInstance().post(new BusProvider.BusEvent(Constants.BUS_EVENT_ROUTE_CHECK_VALID, null));
                }
            }
        }
    }
}
