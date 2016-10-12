package com.douban.rexxar.utils;

import android.util.Log;

import com.douban.rexxar.Rexxar;

/**
 * Created by luanqian on 15/10/30.
 */
public class LogUtils {

    public static void i(String subTag, String message) {
        if (Rexxar.DEBUG) {
            Log.i(Rexxar.TAG, String.format("[%1$s] %2$s", subTag, message));
        }
    }

    public static void e(String subTag, String message) {
        if (Rexxar.DEBUG) {
            Log.e(Rexxar.TAG, String.format("[%1$s] %2$s", subTag, message));
        }
    }
}
