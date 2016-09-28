package com.douban.rexxar.example.widget;

import android.net.Uri;
import android.text.TextUtils;
import android.webkit.WebView;

import com.douban.rexxar.view.RexxarWebView;
import com.douban.rexxar.view.RexxarWidget;

/**
 * Created by luanqian on 16/4/19.
 */
public class PullToRefreshWidget implements RexxarWidget {

    static final String KEY = "action";
    // 启用下拉刷新
    static final String ACTION_ENABLE = "enable";
    // 下拉刷新完成
    static final String ACTION_COMPLETE = "complete";

    @Override
    public String getPath() {
        return "/widget/pull_to_refresh";
    }

    @Override
    public boolean handle(WebView view, String url) {
        if (TextUtils.isEmpty(url)) {
            return false;
        }
        Uri uri = Uri.parse(url);
        String path = uri.getPath();
        if (TextUtils.equals(path, getPath())) {
            String action = uri.getQueryParameter(KEY);
            if (TextUtils.equals(action, ACTION_ENABLE)) {
                ((RexxarWebView)view.getParent().getParent()).enableRefresh(true);
            } else if (TextUtils.equals(action, ACTION_COMPLETE)) {
                ((RexxarWebView)view.getParent().getParent()).setRefreshing(false);
            }
            return true;
        }
        return false;
    }
}
