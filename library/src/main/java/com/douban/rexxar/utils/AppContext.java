package com.douban.rexxar.utils;

import android.content.Context;
import android.content.ContextWrapper;

/**
 * 用来存储app全局context实例
 *
 * created by luanqian on 15/10/16.
 */
public class AppContext extends ContextWrapper{

    private static AppContext sInstance;

    public static void init(Context context) {
        if (null == context) {
            return;
        }
        sInstance = new AppContext(context.getApplicationContext());
    }

    public static AppContext getInstance() {
        if (null == sInstance) {
            throw new IllegalStateException("AppContext must be initialized first!");
        }
        return sInstance;
    }

    public AppContext(Context base) {
        super(base);
    }

}
