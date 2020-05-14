package com.douban.rexxar.view;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.douban.rexxar.Constants;
import com.douban.rexxar.R;
import com.douban.rexxar.resourceproxy.network.RexxarContainerAPI;
import com.douban.rexxar.utils.AppContext;
import com.douban.rexxar.utils.BusProvider;
import com.douban.rexxar.utils.MimeUtils;
import com.douban.rexxar.utils.RxLoadError;
import com.douban.rexxar.utils.io.stream.ClosedInputStream;

import java.lang.ref.WeakReference;
import java.util.Map;

/**
 * pull-to-refresh
 * error view
 *
 * Created by luanqian on 16/4/7.
 */
public class RexxarWebView extends FrameLayout implements RexxarWebViewCore.UriLoadCallback, RexxarWebViewCore.WebViewHeightCallback, RexxarWebViewCore.ReloadDelegate{

    public static final String TAG = "RexxarWebView";

    /**
     * Classes that wish to be notified when the swipe gesture correctly
     * triggers a refresh should implement this interface.
     */
    public interface OnRefreshListener {
        void onRefresh();
    }

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RexxarWebViewCore mCore;
    private RexxarErrorView mErrorView;
    private ProgressBar mProgressBar;

    private String mUri;
    private boolean mUsePage;
    private WeakReference<RexxarWebViewCore.UriLoadCallback> mUriLoadCallback = new WeakReference<RexxarWebViewCore.UriLoadCallback>(null);
    // 加载时间
    private long mStartLoadTime;
    private boolean mEnablePageAutoPageVisible = true;

    public RexxarWebView(Context context) {
        super(context);
        init();
    }

    public RexxarWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RexxarWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.view_rexxar_webview, this, true);
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        try {
            mCore = new RexxarWebViewCore(getContext());
            mCore.addWebViewHeightCallback(this);
            mCore.setReloadDelegate(this);
            mSwipeRefreshLayout.addView(mCore, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        } catch (Exception e) {
            e.printStackTrace();
            // WebView missing, toast & finish activity
            Toast.makeText(AppContext.getInstance(), R.string.webview_missing, Toast.LENGTH_SHORT).show();
            if (null != getContext() && getContext() instanceof Activity) {
                ((Activity) getContext()).finish();
                return;
            }
        }
        mErrorView = (RexxarErrorView) findViewById(R.id.rexxar_error_view);
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        BusProvider.getInstance().register(this);
    }

    RexxarWebViewCore.WebViewHeightCallback mCallback;

    public void addWebViewHeightCallback(RexxarWebViewCore.WebViewHeightCallback callback) {
        if (null != callback) {
            mCallback = callback;
        }
    }

    @Override
    public void onHeightChange(int height) {
        // 优先用callback
        if (null != mCallback) {
            mCallback.onHeightChange(height);
        }
        // mSwipeRefreshLayout
        ViewGroup.LayoutParams layoutParams = mSwipeRefreshLayout.getLayoutParams();
        if (null == layoutParams) {
            layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height);
        } else {
            layoutParams.height = height;
        }
        mSwipeRefreshLayout.setLayoutParams(layoutParams);
    }

    /**
     * 设置下拉刷新监听
     * @param listener
     */
    public void setOnRefreshListener(final OnRefreshListener listener) {
        if (null != listener) {
            mSwipeRefreshLayout.setOnRefreshListener(new androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    listener.onRefresh();
                }
            });
        }
    }

    /**
     * 下拉刷新颜色
     *
     * @param color
     */
    public void setRefreshMainColor(int color) {
        if (color > 0) {
            mSwipeRefreshLayout.setMainColor(color);
        }
    }

    /**
     * 启用/禁用SwipeRefreshLayout
     * @param enable
     */
    public void enableSwipeRefreshLayoutNestesScroll(boolean enable) {
        if (null != mSwipeRefreshLayout) {
            mSwipeRefreshLayout.setNestedScrollingEnabled(enable);
        }
    }

    /**
     * 启用/禁用 下拉刷新手势
     *
     * @param enable
     */
    public void enableRefresh(boolean enable) {
        mSwipeRefreshLayout.setEnabled(enable);
    }

    /**
     * 设置刷新
     * @param refreshing
     */
    public void setRefreshing(boolean refreshing) {
        mSwipeRefreshLayout.setRefreshing(refreshing);
    }

    public WebView getWebView() {
        return mCore;
    }

    /***************************设置RexxarWebViewCore的一些方法代理****************************/

    public void setWebViewClient(RexxarWebViewClient client) {
        if (null != mCore) {
            mCore.setWebViewClient(client);
        }
    }

    public void enableExpandContentHeight(boolean enable) {
        if (null != mCore) {
            mCore.enableExpandContentHeight(enable);
        }
    }

    public void setWebviewCallback(RexxarWebViewCore.WebCallbacks callback) {
        if (null != mCore) {
            mCore.setWebviewCallback(callback);
        }
    }

    public int getWebContentHeight() {
        if (null != mCore) {
            return mCore.getWebViewContentHeight();
        }
        return 0;
    }

    public void setWebViewScrollListener(RexxarWebViewCore.WebViewScrollListener scrollListener) {
        if (null != mCore) {
            mCore.setWebViewScrollListener(scrollListener);
        }
    }

    /**
     * 启用/禁用 嵌套滑动
     */
    public void enableNestedScroll(boolean enable) {
        mCore.enableNestedScroll(enable);
    }

    public void setWebChromeClient(RexxarWebChromeClient client) {
        if (null != mCore) {
            mCore.setWebChromeClient(client);
        }
    }

    public void loadUri(String uri) {
        if (null != mCore) {
            this.mUri = uri;
            this.mUsePage = true;
            mCore.loadUri(uri,this);
            mStartLoadTime = System.currentTimeMillis() / 1000;
        }
    }

    public void loadUri(String uri, final RexxarWebViewCore.UriLoadCallback callback) {
        if (null != mCore) {
            this.mUri = uri;
            this.mUsePage = true;
            if (null != callback) {
                this.mUriLoadCallback = new WeakReference<RexxarWebViewCore.UriLoadCallback>(callback);
            }

            mCore.loadUri(uri, this);
            mStartLoadTime = System.currentTimeMillis() / 1000;
        }
    }

    public void loadPartialUri(String uri) {
        if (null != mCore) {
            mCore.loadPartialUri(uri);
            this.mUri = uri;
            this.mUsePage = false;
            mStartLoadTime = System.currentTimeMillis() / 1000;
        }
    }

    public void loadPartialUri(String uri, final RexxarWebViewCore.UriLoadCallback callback) {
        if (null != mCore) {
            this.mUri = uri;
            this.mUsePage = false;
            if (null != callback) {
                this.mUriLoadCallback = new WeakReference<RexxarWebViewCore.UriLoadCallback>(callback);
            }

            mCore.loadPartialUri(uri, this);
            mStartLoadTime = System.currentTimeMillis() / 1000;
        }
    }

    @Override
    public boolean onStartLoad() {
        post(new Runnable() {
            @Override
            public void run() {
                if (null == mUriLoadCallback.get() || !mUriLoadCallback.get().onStartLoad()) {
                    mProgressBar.setVisibility(View.VISIBLE);
                }
            }
        });
        return true;
    }

    @Override
    public boolean onStartDownloadHtml() {
        post(new Runnable() {
            @Override
            public void run() {
                if (null == mUriLoadCallback.get() || !mUriLoadCallback.get().onStartDownloadHtml()) {
                    mProgressBar.setVisibility(View.VISIBLE);
                }
            }
        });
        return true;
    }

    @Override
    public boolean onSuccess() {
        post(new Runnable() {
            @Override
            public void run() {
                if (null == mUriLoadCallback.get() || !mUriLoadCallback.get().onSuccess()) {
                    mProgressBar.setVisibility(View.GONE);
                }
            }
        });
        return true;
    }

    @Override
    public boolean onFail(final RxLoadError error) {
        post(new Runnable() {
            @Override
            public void run() {
                if (null == mUriLoadCallback.get() || !mUriLoadCallback.get().onFail(error)) {
                    mProgressBar.setVisibility(View.GONE);
                    mErrorView.show(error.message);
                }
            }
        });
        return true;
    }

    public void destroy() {
        if (null != mCore) {
            // 调用生命周期函数
            onPageDestroy();
            setWebViewClient(new NullWebViewClient());

            // 页面加载时间超过4s之后才可以直接销毁
            if (System.currentTimeMillis() / 1000 - mStartLoadTime > 4) {
                destroyWebViewCore();
            } else {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        destroyWebViewCore();
                    }
                }, 3000);
            }
        }
    }

    private void destroyWebViewCore() {
        try {
            if (null != mCore) {
                mSwipeRefreshLayout.removeView(mCore);
                mCore.loadUrl("about:blank");
                mCore.stopLoading();
                // 退出时调用此方法，移除绑定的服务，否则某些特定系统会报错
                mCore.getSettings().setJavaScriptEnabled(false);
                mCore.clearHistory();
                mCore.clearView();
                mCore.removeAllViews();
                mCore.destroy();
            }
        } catch (Throwable ex) {
        }
        mCore = null;
    }

    public void loadUrl(String url) {
        if (null != mCore) {
            mCore.loadUrl(url);
            mStartLoadTime = System.currentTimeMillis() / 1000;
        }
    }

    public void loadData(String data, String mimeType, String encoding) {
        if (null != mCore) {
            mCore.loadData(data, mimeType, encoding);
            mStartLoadTime = System.currentTimeMillis() / 1000;
        }
    }

    public void loadUrl(String url, Map<String, String> additionalHttpHeaders) {
        if (null != mCore) {
            mCore.loadUrl(url, additionalHttpHeaders);
            mStartLoadTime = System.currentTimeMillis() / 1000;
        }
    }

    public void loadDataWithBaseURL(String baseUrl, String data, String mimeType, String encoding,
                                    String historyUrl) {
        if (null != mCore) {
            mCore.loadDataWithBaseURL(baseUrl, data, mimeType, encoding, historyUrl);
            mStartLoadTime = System.currentTimeMillis() / 1000;
        }
    }

    public void onPause() {
        if (null != mCore) {
            mCore.onPause();
        }
    }

    public void onResume() {
        if (null != mCore) {
            mCore.onResume();
        }
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (null != mCore) {
            if (mEnablePageAutoPageVisible) {
                if (visibility == View.VISIBLE) {
                    onPageVisible();
                } else {
                    onPageInvisible();
                }
            }
        }
    }

    public void disableAutoPageVisible() {
        mEnablePageAutoPageVisible = false;
    }

    /**
     * 自定义url拦截处理
     *
     * @param widget
     */
    public void addRexxarWidget(RexxarWidget widget) {
        if (null == widget) {
            return;
        }
        if (null != mCore) {
            mCore.addRexxarWidget(widget);
        }
    }

    /**
     * 自定义container api
     *
     * @param containerAPI
     */
    public void addContainerApi(RexxarContainerAPI containerAPI) {
        if (null != containerAPI && null != mCore) {
            mCore.addContainerApi(containerAPI);
        }
    }

    public void onPageVisible() {
        callFunction("Rexxar.Lifecycle.onPageVisible");
    }

    public void onPageInvisible() {
        callFunction("Rexxar.Lifecycle.onPageInvisible");
    }

    public void onPageDestroy() {
        callFunction("Rexxar.Lifecycle.onPageDestroy");
    }

    @Override
    protected void onDetachedFromWindow() {
        BusProvider.getInstance().unregister(this);
        super.onDetachedFromWindow();
    }

    public void onEventMainThread(BusProvider.BusEvent event) {
        if (event.eventId == Constants.EVENT_REXXAR_RETRY) {
            mErrorView.setVisibility(View.GONE);
            reload();
        } else if (event.eventId == Constants.EVENT_REXXAR_NETWORK_ERROR) {
            boolean handled = false;
            RxLoadError error = RxLoadError.UNKNOWN;
            if (null != event.data) {
                error = event.data.getParcelable(Constants.KEY_ERROR);
            }
            if (null != mUriLoadCallback && null != mUriLoadCallback.get()) {
                handled = mUriLoadCallback.get().onFail(error);
            }
            if (!handled) {
                mProgressBar.setVisibility(View.GONE);
                mErrorView.show(error.message);
            }
        }
    }

    /**
     * 重新加载页面
     */
    public void reload() {
        if (null != mCore) {
            if (mUsePage) {
                mCore.loadUri(mUri, this);
            } else {
                mCore.loadPartialUri(mUri, this);
            }
        }
    }

    @Override
    public void onReload() {
        reload();
    }

    /**
     * Native调用js方法, 传递参数
     *
     * @param functionName 方法名
     */
    public void callFunction(String functionName) {
        callFunction(functionName, null);
    }

    /**
     * Native调用js方法, 传递参数
     *
     * @param functionName 方法名
     * @param jsonString 参数,需要是json格式
     */
    public void callFunction(String functionName, String jsonString) {
        if (TextUtils.isEmpty(functionName)) {
            return;
        }
        if (null == mCore) {
            return;
        }
        if (TextUtils.isEmpty(jsonString)) {
            mCore.loadUrl(String.format(Constants.FUNC_FORMAT, functionName));
        } else {
            jsonString = jsonString.replaceAll("(\\\\)([^utrn])", "\\\\\\\\$1$2");
            jsonString = jsonString.replaceAll("(\\\\)([utrn])", "\\\\$1$2");
            jsonString = jsonString.replaceAll("(?<=[^\\\\])(\")", "\\\\\"");
            jsonString = jsonString.replaceAll("(?<=[^\\\\])(\')", "\\\\\'");
            String command = String.format(Constants.FUNC_FORMAT_WITH_PARAMETERS, functionName, jsonString);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                try {
                    mCore.evaluateJavascript(command, null);
                } catch (Exception e) {
                    mCore.loadUrl(String.format(Constants.FUNC_FORMAT_WITH_PARAMETERS, functionName, jsonString));
                }
            } else {
                mCore.loadUrl(String.format(Constants.FUNC_FORMAT_WITH_PARAMETERS, functionName, jsonString));
            }
        }
    }

    /**
     * 存在的原因
     * 因为我们通过shouldInterceptRequest来实现拦截，经测试发现快速打开rexxar页面再退出，连续5次左右会出现rexxar页无法打开的情况;
     * 而原生的webview不存在这个问题，经过定位发现如果不覆写shouldInterceptRequest这个方法，就不会出现这个问题。
     *
     * 清除WebViewClient是在WebView的destroy方法实现的，所以rexxar的webview必须尽快调用destory方法。
     *
     * 但因为退出时要调用js方法，稍微延迟destory，所以通过主动设置一个没有实现shouldInterceptRequest的RexxarWebViewClient来避免能上面的问题。
     */
    private static class NullWebViewClient extends RexxarWebViewClient{

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return true;
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            return true;
        }

        @TargetApi(21)
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(request.getUrl().toString());
            String mimeType = MimeUtils.guessMimeTypeFromExtension(fileExtension);
            return new WebResourceResponse(mimeType, "UTF-8", new ClosedInputStream());
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(url);
            String mimeType = MimeUtils.guessMimeTypeFromExtension(fileExtension);
            return new WebResourceResponse(mimeType, "UTF-8", new ClosedInputStream());
        }
    }
}
