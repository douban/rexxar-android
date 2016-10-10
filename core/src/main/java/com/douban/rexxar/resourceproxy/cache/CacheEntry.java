package com.douban.rexxar.resourceproxy.cache;

import android.text.TextUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by luanqian on 15/12/23.
 */
public class CacheEntry {

    public InputStream inputStream;
    public long length;

    public CacheEntry(long length, InputStream inputStream) {
        this.length = length;
        this.inputStream = inputStream;
    }

    public boolean isValid() {
        return null != inputStream;
    }

    public void close() {
        if (null != inputStream) {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
