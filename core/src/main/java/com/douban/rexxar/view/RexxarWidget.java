package com.douban.rexxar.view;

import android.webkit.WebView;

/**
 * Created by luanqian on 16/9/26.
 */
public interface RexxarWidget {

    /**
     * a special path for the widget
     *
     * @return
     */
    String getPath();

    /**
     * whether we can handle the url
     * @param view
     * @param url
     * @return
     */
    boolean handle(WebView view, String url);
}
