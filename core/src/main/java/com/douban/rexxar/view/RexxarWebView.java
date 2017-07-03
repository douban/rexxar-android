package com.douban.rexxar.view;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import com.douban.rexxar.Constants;
import com.douban.rexxar.R;
import com.douban.rexxar.resourceproxy.network.RexxarContainerAPI;
import com.douban.rexxar.utils.BusProvider;
import com.douban.rexxar.utils.RxLoadError;

import java.lang.ref.WeakReference;
import java.util.Map;

/**
 * pull-to-refresh
 * error view
 *
 * Created by luanqian on 16/4/7.
 */
public class RexxarWebView extends FrameLayout implements RexxarWebViewCore.UriLoadCallback{

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
        mCore = (RexxarWebViewCore) findViewById(R.id.webview);
        mErrorView = (RexxarErrorView) findViewById(R.id.rexxar_error_view);
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        BusProvider.getInstance().register(this);
    }

    /**
     * 设置下拉刷新监听
     * @param listener
     */
    public void setOnRefreshListener(final OnRefreshListener listener) {
        if (null != listener) {
            mSwipeRefreshLayout.setOnRefreshListener(new android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener() {
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
        mCore.setWebViewClient(client);
    }

    public void setWebChromeClient(RexxarWebChromeClient client) {
        mCore.setWebChromeClient(client);
    }

    public void loadUri(String uri) {
        this.mUri = uri;
        this.mUsePage = true;
        mCore.loadUri(uri,this);
    }

    public void loadUri(String uri, final RexxarWebViewCore.UriLoadCallback callback) {
        this.mUri = uri;
        this.mUsePage = true;
        if (null != callback) {
            this.mUriLoadCallback = new WeakReference<RexxarWebViewCore.UriLoadCallback>(callback);
        }

        mCore.loadUri(uri, this);
    }

    public void loadPartialUri(String uri) {
        mCore.loadPartialUri(uri);
        this.mUri = uri;
        this.mUsePage = false;
    }

    public void loadPartialUri(String uri, final RexxarWebViewCore.UriLoadCallback callback) {
        this.mUri = uri;
        this.mUsePage = false;
        if (null != callback) {
            this.mUriLoadCallback = new WeakReference<RexxarWebViewCore.UriLoadCallback>(callback);
        }

        mCore.loadPartialUri(uri, this);
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
        mSwipeRefreshLayout.removeView(mCore);
        mCore.stopLoading();
        // 退出时调用此方法，移除绑定的服务，否则某些特定系统会报错
        mCore.getSettings().setJavaScriptEnabled(false);
        mCore.clearHistory();
        mCore.clearView();
        mCore.removeAllViews();

        try {
            mCore.destroy();
        } catch (Throwable ex) {

        }
        mCore = null;
    }

    public void loadUrl(String url) {
        mCore.loadUrl(url);
    }

    public void loadData(String data, String mimeType, String encoding) {
        mCore.loadData(data, mimeType, encoding);
    }

    public void loadUrl(String url, Map<String, String> additionalHttpHeaders) {
        mCore.loadUrl(url, additionalHttpHeaders);
    }

    public void loadDataWithBaseURL(String baseUrl, String data, String mimeType, String encoding,
                                    String historyUrl) {
        mCore.loadDataWithBaseURL(baseUrl, data, mimeType, encoding, historyUrl);
    }

    public void onPause() {
        mCore.onPause();
    }

    public void onResume() {
        mCore.onResume();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == View.VISIBLE) {
            onPageVisible();
        } else {
            onPageInvisible();
        }
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
        mCore.addRexxarWidget(widget);
    }

    /**
     * 自定义container api
     *
     * @param containerAPI
     */
    public void addContainerApi(RexxarContainerAPI containerAPI) {
        if (null != containerAPI) {
            mCore.addContainerApi(containerAPI);
        }
    }

    public void onPageVisible() {
        callFunction("Rexxar.Lifecycle.onPageVisible");
    }

    public void onPageInvisible() {
        callFunction("Rexxar.Lifecycle.onPageInvisible");
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
        if (mUsePage) {
            mCore.loadUri(mUri, this);
        } else {
            mCore.loadPartialUri(mUri, this);
        }
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
        if (TextUtils.isEmpty(jsonString)) {
            mCore.loadUrl(String.format(Constants.FUNC_FORMAT, functionName));
        } else {
            jsonString = jsonString.replaceAll("(\\\\)([^utrn])", "\\\\\\\\$1$2");
            jsonString = jsonString.replaceAll("(\\\\)([utrn])", "\\\\$1$2");
            jsonString = jsonString.replaceAll("(?<=[^\\\\])(\")", "\\\\\"");
            mCore.loadUrl(String.format(Constants.FUNC_FORMAT_WITH_PARAMETERS, functionName, jsonString));
        }
    }
}
