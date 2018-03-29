package com.douban.rexxar.view;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.douban.rexxar.Constants;
import com.douban.rexxar.Rexxar;
import com.douban.rexxar.resourceproxy.ResourceProxy;
import com.douban.rexxar.resourceproxy.cache.CacheEntry;
import com.douban.rexxar.resourceproxy.cache.CacheHelper;
import com.douban.rexxar.resourceproxy.network.RexxarContainerAPI;
import com.douban.rexxar.resourceproxy.network.RexxarContainerAPIHelper;
import com.douban.rexxar.utils.BusProvider;
import com.douban.rexxar.utils.LogUtils;
import com.douban.rexxar.utils.MimeUtils;
import com.douban.rexxar.utils.RxLoadError;
import com.douban.rexxar.utils.Utils;
import com.douban.rexxar.utils.io.IOUtils;

import org.apache.http.conn.ConnectTimeoutException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.GzipSource;

/**
 * Created by luanqian on 15/10/28.
 */

public class RexxarWebViewClient extends WebViewClient {

    static final String TAG = RexxarWebViewClient.class.getSimpleName();

    // webView支持的widget
    private List<RexxarWidget> mWidgets = new ArrayList<>();
    // webView支持的container api
    private List<RexxarContainerAPI> mContainerApis = new ArrayList<>();

    /**
     * 自定义url拦截处理
     *
     * @param widget
     */
    public void addRexxarWidget(RexxarWidget widget) {
        if (null != widget) {
            mWidgets.add(widget);
        }
    }

    /**
     * 自定义container api
     *
     * @param containerAPI
     */
    public void addContainerApi(RexxarContainerAPI containerAPI) {
        if (null != containerAPI) {
            mContainerApis.add(containerAPI);
        }
    }

    public List<RexxarWidget> getRexxarWidgets() {
        return mWidgets;
    }

    public List<RexxarContainerAPI> getRexxarContainerApis() {
        return mContainerApis;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        LogUtils.i(TAG, "[shouldOverrideUrlLoading] : url = " + url);
        boolean handled;
        for (RexxarWidget widget : mWidgets) {
            if (null != widget) {
                handled = widget.handle(view, url);
                if (handled) {
                    return true;
                }
            }
        }
        return super.shouldOverrideUrlLoading(view, url);
    }

    @TargetApi(21)
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        WebResourceResponse resourceResponse;
        if (Utils.hasLollipop()) {
            resourceResponse = handleResourceRequest(view, request.getUrl().toString());
        } else {
            resourceResponse = super.shouldInterceptRequest(view, request);
        }
        return resourceResponse;
    }


    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        return handleResourceRequest(view, url);
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        LogUtils.i(TAG, "onPageStarted");
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        LogUtils.i(TAG, "onPageFinished");
    }

    @Override
    public void onLoadResource(WebView view, String url) {
        super.onLoadResource(view, url);
        LogUtils.i(TAG, "onLoadResource : " + url);
    }

    /**
     * 拦截资源请求，部分资源需要返回本地资源
     * <p>
     * <p>
     * html，js资源直接渲染进程返回,图片等其他资源先返回空的数据流再异步向流中写数据
     * <p>
     * <p>
     * <note>这个方法会在渲染线程执行，如果做了耗时操作会block渲染</note>
     */
    protected WebResourceResponse handleResourceRequest(final WebView webView, String requestUrl) {
        if (!shouldIntercept(requestUrl)) {
            return super.shouldInterceptRequest(webView, requestUrl);
        }
        LogUtils.i(TAG, "[handleResourceRequest] url =  " + requestUrl);

        // html直接返回
        if (Helper.isHtmlResource(requestUrl)) {
            // decode resource
            if (requestUrl.startsWith(Constants.FILE_AUTHORITY)) {
                requestUrl = requestUrl.substring(Constants.FILE_AUTHORITY.length());
            }
            final CacheEntry cacheEntry = CacheHelper.getInstance().findHtmlCache(requestUrl);
            if (null == cacheEntry) {
                // 没有cache，显示错误界面
                RxLoadError error = RxLoadError.HTML_NO_CACHE.clone();
                error.extra = "cacheEntry is null";
                showError(error);
                return super.shouldInterceptRequest(webView, requestUrl);
            } else if (!cacheEntry.isValid()) {
                // 有cache但无效，显示错误界面且清除缓存
                RxLoadError error = RxLoadError.HTML_NO_CACHE.clone();
                error.extra = "cacheEntry is invalid";
                showError(error);
                CacheHelper.getInstance().removeHtmlCache(requestUrl);
            } else {
                LogUtils.i(TAG, "cache hit :" + requestUrl);
                String data = "";
                try {
                    data = IOUtils.toString(cacheEntry.inputStream);
                    // hack 检查cache是否完整
                    if (TextUtils.isEmpty(data) || !data.endsWith("</html>")) {
                        RxLoadError error = RxLoadError.HTML_CACHE_INVALID.clone();
                        if (TextUtils.isEmpty(data)) {
                            error.extra = "html is empty";
                        } else {
                            error.extra = "html is not end with </html>";
                        }
                        showError(error);
                        CacheHelper.getInstance().removeHtmlCache(requestUrl);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    // hack 检查cache是否完整
                    RxLoadError error = RxLoadError.HTML_CACHE_INVALID.clone();
                    error.extra = e.getMessage();
                    showError(error);
                    CacheHelper.getInstance().removeHtmlCache(requestUrl);
                }
                return new WebResourceResponse(Constants.MIME_TYPE_HTML, "utf-8", IOUtils.toInputStream(data));
            }
        }

        // js直接返回
        if (CacheHelper.getInstance().cacheEnabled() && Helper.isJsResource(requestUrl)) {
            final CacheEntry cacheEntry = CacheHelper.getInstance().findCache(requestUrl);
            if (null == cacheEntry) {
                // 后面逻辑会通过network去加载
                // 加载后再显示
            } else if (!cacheEntry.isValid()){
                // 后面逻辑会通过network去加载
                // 加载后再显示
                // 清除缓存
                CacheHelper.getInstance().removeInternalCache(requestUrl);
            } else {
                String data = "";
                try {
                    data = IOUtils.toString(cacheEntry.inputStream);
                    if (TextUtils.isEmpty(data) || (cacheEntry.length > 0 && cacheEntry.length != data.getBytes().length)) {
                        RxLoadError error = RxLoadError.JS_CACHE_INVALID.clone();
                        if (TextUtils.isEmpty(data)) {
                            // 发生0次
                            error.extra = "js is empty";
                        } else {
                            // 发生0次
                            error.extra = "cache length : " + cacheEntry.length + "; data length : " + data.getBytes().length;
                        }
                        showError(error);
                        CacheHelper.getInstance().removeInternalCache(requestUrl);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    RxLoadError error = RxLoadError.JS_CACHE_INVALID.clone();
                    error.extra = e.getMessage();
                    showError(error);
                    CacheHelper.getInstance().removeInternalCache(requestUrl);
                }
                LogUtils.i(TAG, "cache hit :" + requestUrl);
                return new WebResourceResponse(Constants.MIME_TYPE_HTML, "utf-8", IOUtils.toInputStream(data));
            }
        }

        // 图片等其他资源使用先返回空流，异步写数据
        String fileExtension = MimeTypeMap.getFileExtensionFromUrl(requestUrl);
        String mimeType = MimeUtils.guessMimeTypeFromExtension(fileExtension);
        CacheEntry cacheEntry = null;
        // 静态文件缓存也直接返回，避免创建更多地线程
        if (CacheHelper.getInstance().cacheEnabled()) {
            cacheEntry = CacheHelper.getInstance().findCache(requestUrl);
        }
        if (null != cacheEntry && cacheEntry.isValid()) {
            LogUtils.i(TAG, "file cache hit :" + requestUrl);
            return new WebResourceResponse(mimeType, "utf-8", cacheEntry.inputStream);
        }

        // 当缓存无法命中时，启动一个线程去获取数据
        try {
            LogUtils.i(TAG, "start load async :" + requestUrl);
            WebResourceResponse xResponse = new WebResourceResponse(mimeType, "UTF-8", new RexxarNetworkInputStream(requestUrl, mContainerApis));
            if (Utils.hasLollipop()) {
                Map<String, String> headers = new HashMap<>();
                headers.put("Access-Control-Allow-Origin", "*");
                xResponse.setResponseHeaders(headers);
            }
            return xResponse;
        } catch (Throwable e) {
            e.printStackTrace();
            LogUtils.e(TAG, "url : " + requestUrl + " " + e.getMessage());
            return super.shouldInterceptRequest(webView, requestUrl);
        }
    }

    /**
     * html或js加载错误，页面无法渲染，通知{@link RexxarWebView}显示错误界面，重新加载
     *
     * @param error 错误
     */
    public void showError(RxLoadError error) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(Constants.KEY_ERROR, error);
        BusProvider.getInstance().post(new BusProvider.BusEvent(Constants.EVENT_REXXAR_NETWORK_ERROR, bundle));
    }

    /**
     * @param requestUrl
     * @return
     */
    private boolean shouldIntercept(String requestUrl) {
        if (TextUtils.isEmpty(requestUrl)) {
            return false;
        }
        // file协议需要替换,用于html
        if (requestUrl.startsWith(Constants.FILE_AUTHORITY)) {
            return true;
        }

        // rexxar container api，需要拦截
        if (requestUrl.startsWith(Constants.CONTAINER_API_BASE)) {
            return true;
        }

        // 非合法uri，不拦截
        Uri uri = null;
        try {
            uri = Uri.parse(requestUrl);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (null == uri) {
            return false;
        }

        // 非合法host，不拦截
        String host = uri.getHost();
        if (TextUtils.isEmpty(host)) {
            return false;
        }

        // 不能拦截的uri，不拦截
        Pattern pattern;
        Matcher matcher;
        for (String interceptHostItem : ResourceProxy.getInstance().getProxyHosts()) {
            pattern = Pattern.compile(interceptHostItem);
            matcher = pattern.matcher(host);
            if (matcher.find()) {
                return true;
            }
        }
        return false;
    }


    private static class Helper {

        /**
         * 是否是html文档
         *
         * @param requestUrl
         * @return
         */
        public static boolean isHtmlResource(String requestUrl) {
            if (TextUtils.isEmpty(requestUrl)) {
                return false;
            }
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(requestUrl);
            return TextUtils.equals(fileExtension, Constants.EXTENSION_HTML)
                    || TextUtils.equals(fileExtension, Constants.EXTENSION_HTM);
        }

        /**
         * 是否是js文档
         *
         * @param requestUrl
         * @return
         */
        public static boolean isJsResource(String requestUrl) {
            if (TextUtils.isEmpty(requestUrl)) {
                return false;
            }
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(requestUrl);
            return TextUtils.equals(fileExtension, Constants.EXTENSION_JS);
        }

        /**
         * 构建网络请求
         *
         * @param requestUrl
         * @return
         */
        public static Request buildRequest(String requestUrl) {
            if (TextUtils.isEmpty(requestUrl)) {
                return null;
            }
            Request.Builder builder = new Request.Builder();
            Uri uri = Uri.parse(requestUrl);
            String method = uri.getQueryParameter(Constants.KEY_METHOD);
            //  如果没有值则视为get
            if (Constants.METHOD_POST.equalsIgnoreCase(method)) {
                FormBody.Builder formBodyBuilder = new FormBody.Builder();
                Set<String> names = uri.getQueryParameterNames();
                for (String key : names) {
                    if(!Constants.KEY_METHOD.equalsIgnoreCase(key))
                        formBodyBuilder.add(key, uri.getQueryParameter(key));
                }
                builder.method("POST", formBodyBuilder.build()).url(requestUrl.substring(0,requestUrl.indexOf("?")));
            } else {
                builder.method("GET", null).url(requestUrl);
            }
            builder.addHeader("User-Agent", Rexxar.getUserAgent());
            return builder.build();
        }

    }

    private static class RexxarNetworkInputStream extends InputStream {

        private String url;
        List<RexxarContainerAPI> containerAPIs;
        private InputStream inputStream;
        private boolean initialized=false;

        public RexxarNetworkInputStream(String url, List<RexxarContainerAPI> containerAPIs) {
            this.url = url;
            this.containerAPIs = new ArrayList<>();
            if (null != containerAPIs) {
                this.containerAPIs.addAll(containerAPIs);
            }
        }

        @Override
        public int read() throws IOException {
            if (!initialized) {
                Log.i("xxxxxx", "start initial : " + url);
                Response response = null;
                try {
                    // request network
                    Request request = Helper.buildRequest(url);
                    // 优先用container-api处理, 如果container-api无法处理, 再去网络发出请求
                    response = RexxarContainerAPIHelper.handle(request, this.containerAPIs);
                    if (null == response) {
                        response = ResourceProxy.getInstance().getNetwork().handle(request);
                    }
                    // write cache
                    if (response.isSuccessful()) {
                        if (CacheHelper.getInstance().checkUrl(url) && null != response.body()) {
                            CacheHelper.getInstance().saveCache(url, IOUtils.toByteArray(response.body().byteStream()));
                            CacheEntry cacheEntry = CacheHelper.getInstance().findCache(url);
                            if (null != cacheEntry && cacheEntry.isValid()) {
                                this.inputStream = cacheEntry.inputStream;
                            }
                        }
                        if (null == inputStream && null != response.body()) {
                            inputStream = response.body().byteStream();
                        } else if (null == response.body()){
                            inputStream = IOUtils.toInputStream("{}");
                        }
                    } else {
                        LogUtils.i(TAG, "load async failed :" + url);
                        if (Helper.isJsResource(url)) {
                            // 如果是404的话，html加载成功了，js出错了, 是否意味着需要刷新一次route.
                            RxLoadError error = RxLoadError.JS_CACHE_INVALID.clone();
                            error.extra = "request is fail, response code: " + response.code() + " : " + url;
                            showError(error);
                            initialized = true;
                            return -1;
                        }

                        // return request error
                        byte[] result = wrapperErrorResponse(response);
                        if (Rexxar.DEBUG) {
                            LogUtils.i(TAG, "Api Error: " + new String(result));
                        }
                        inputStream = IOUtils.toInputStream(new String(result));
                    }
                } catch (SocketTimeoutException e) {
                    byte[] result = wrapperErrorResponse(e);
                    if (Rexxar.DEBUG) {
                        LogUtils.i(TAG, "SocketTimeoutException: " + new String(result));
                    }
                    inputStream = IOUtils.toInputStream(new String(result));
                } catch (ConnectTimeoutException e) {
                    byte[] result = wrapperErrorResponse(e);
                    if (Rexxar.DEBUG) {
                        LogUtils.i(TAG, "ConnectTimeoutException: " + new String(result));
                    }
                    inputStream = IOUtils.toInputStream(new String(result));
                } catch (Exception e) {
                    e.printStackTrace();
                    LogUtils.i(TAG, "load async exception :" + url + " ; " + e.getMessage());
                    if (Helper.isJsResource(url)) {
                        RxLoadError error = RxLoadError.JS_CACHE_INVALID.clone();
                        error.extra = e.getMessage() + " : " + url;
                        showError(error);
                    }
                    byte[] result = wrapperErrorResponse(e);
                    if (Rexxar.DEBUG) {
                        LogUtils.i(TAG, "Exception: " + new String(result));
                    }
                    inputStream = IOUtils.toInputStream(new String(result));
                } finally {
                    initialized = true;
                }
            }
            // 返回数据
            if (null != inputStream) {
                return inputStream.read();
            }
            return  -1;
        }

        private boolean responseGzip(Map<String, String> headers) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                if (entry.getKey()
                        .toLowerCase()
                        .equals(Constants.HEADER_CONTENT_ENCODING.toLowerCase())
                        && entry.getValue()
                        .toLowerCase()
                        .equals(Constants.ENCODING_GZIP.toLowerCase())) {
                    return true;
                }
            }
            return false;
        }

        private byte[] parseGzipResponseBody(ResponseBody body) throws IOException{
            Buffer buffer = new Buffer();
            GzipSource gzipSource = new GzipSource(body.source());
            while (gzipSource.read(buffer, Integer.MAX_VALUE) != -1) {
            }
            gzipSource.close();
            return buffer.readByteArray();
        }

        private byte[] wrapperErrorResponse(Exception exception){
            if (null == exception) {
                return new byte[0];
            }

            try {
                // generate json response
                JSONObject result = new JSONObject();
                result.put(Constants.KEY_NETWORK_ERROR, true);
                return (Constants.ERROR_PREFIX + result.toString()).getBytes();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return new byte[0];
        }

        private byte[] wrapperErrorResponse(Response response){
            if (null == response) {
                return new byte[0];
            }
            try {
                // read response content
                Map<String, String> responseHeaders = new HashMap<>();
                for (String field : response.headers()
                        .names()) {
                    responseHeaders.put(field, response.headers()
                            .get(field));
                }
                byte[] responseContents = new byte[0];
                if (null != response.body()) {
                    if (responseGzip(responseHeaders)) {
                        responseContents = parseGzipResponseBody(response.body());
                    } else {
                        responseContents = response.body().bytes();
                    }
                }

                // generate json response
                JSONObject result = new JSONObject();
                result.put(Constants.KEY_RESPONSE_CODE, response.code());
                String apiError = new String(responseContents, "utf-8");
                try {
                    JSONObject content = new JSONObject(apiError);
                    result.put(Constants.KEY_RESPONSE_ERROR, content);
                } catch (Exception e) {
                    e.printStackTrace();
                    result.put(Constants.KEY_RESPONSE_ERROR, apiError);
                }
                return (Constants.ERROR_PREFIX + result.toString()).getBytes();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return new byte[0];
        }

        public void showError(RxLoadError error) {
            Bundle bundle = new Bundle();
            bundle.putParcelable(Constants.KEY_ERROR, error);
            BusProvider.getInstance().post(new BusProvider.BusEvent(Constants.EVENT_REXXAR_NETWORK_ERROR, bundle));
        }
    }


}
