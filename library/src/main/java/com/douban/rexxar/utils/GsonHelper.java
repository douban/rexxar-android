package com.douban.rexxar.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Created by luanqian on 16/8/11.
 */
public class GsonHelper {

    private static Gson sInstance;

    public static Gson getInstance() {
        if (null == sInstance) {
            synchronized (GsonHelper.class) {
                if (null == sInstance) {
                    sInstance = new GsonBuilder().serializeNulls()
                            .create();
                }
            }
        }
        return sInstance;
    }
}
