package com.douban.rexxar.example.widget;

import android.app.Activity;
import android.net.Uri;
import android.text.TextUtils;
import android.webkit.WebView;

import com.douban.rexxar.view.RexxarWidget;

/**
 * Created by luanqian on 15/11/24.
 */
public class TitleWidget implements RexxarWidget {


    static final String KEY_TITLE = "title";

    @Override
    public String getPath() {
        return "/widget/nav_title";
    }

    @Override
    public boolean handle(WebView view, String url) {
        if (TextUtils.isEmpty(url)) {
            return false;
        }
        Uri uri = Uri.parse(url);
        if (TextUtils.equals(uri.getPath(), getPath())) {
            String title = uri.getQueryParameter(KEY_TITLE);
            if (null != view && view.getContext() instanceof Activity) {
                ((Activity)view.getContext()).setTitle(Uri.decode(title));
            }
            return true;
        }
        return false;
    }
}
