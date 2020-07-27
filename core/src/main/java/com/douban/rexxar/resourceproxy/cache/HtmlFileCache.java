package com.douban.rexxar.resourceproxy.cache;

import android.content.Context;
import android.text.TextUtils;

import com.douban.rexxar.Constants;
import com.douban.rexxar.utils.AppContext;
import com.douban.rexxar.utils.LogUtils;
import com.douban.rexxar.utils.Utils;
import com.douban.rexxar.utils.io.IOUtils;
import com.jakewharton.disklrucache.DiskLruCache;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 缓存html文件
 *
 * html作为页面入口，在缓存上单独处理。
 *
 * 存储位置默认在/data/data/html下
 *
 * <p>
 * Created by luanqian on 15/11/2.
 */
public class HtmlFileCache implements ICache {

    public static final String TAG = "HtmlFileCache";

    private DiskLruCache mDiskCache;

    public HtmlFileCache() {
        checkState();
    }

    @Override
    public CacheEntry findCache(String url) {
        return getCache(url);
    }

    @Override
    public boolean removeCache(String url) {
        checkState();
        try {
            LogUtils.i(TAG, "remove cache  : url " + url);
            return mDiskCache.remove(CacheHelper.getInstance().urlToKey(url));
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void checkState() {
        if (null == mDiskCache || mDiskCache.isClosed()) {
            File directory = new File(AppContext.getInstance().getDir(Constants.CACHE_HOME_DIR,
                    Context.MODE_PRIVATE), Constants.DEFAULT_DISK_FILE_PATH + "_html");
            try {
                mDiskCache = DiskLruCache.open(directory, Constants.VERSION, 2,
                        Constants.CACHE_SIZE);
            } catch (IOException e) {
                LogUtils.e(TAG, e.getMessage());
            }
        }
    }

    /**
     * 保存文件缓存
     *
     * @param url         html的url
     * @param bytes html数据
     */
    public boolean saveCache(String url, byte[] bytes) {
        if (TextUtils.isEmpty(url) || null == mDiskCache) {
            return false;
        }
        checkState();
        DiskLruCache.Editor editor = null;
        String key = CacheHelper.getInstance().urlToKey(url);
        // 如果存在，则先删掉之前的缓存
        removeCache(url);
        OutputStream outputStream = null;
        try {
            editor = mDiskCache.edit(key);
            if (null == editor) {
                return false;
            }
            editor.set(0, String.valueOf(bytes.length));
            outputStream = editor.newOutputStream(1);
            outputStream.write(bytes);
            outputStream.flush();
            editor.commit();
            editor = null;
            return true;
        } catch (Exception e) {
            LogUtils.e(TAG, e.getMessage());
            try {
                mDiskCache.remove(key);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        } finally {
            if (null != editor) {
                try {
                    editor.commit();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (null != outputStream) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    public CacheEntry getCache(String url) {
        if (TextUtils.isEmpty(url) || null == mDiskCache) {
            return null;
        }
        checkState();
        InputStream inputStream = null;
        try {
            DiskLruCache.Snapshot snapshot = mDiskCache.get(
                    CacheHelper.getInstance().urlToKey(url));
            if (null == snapshot) {
                return null;
            }
            LogUtils.i(TAG, "hit");
            long length = 0;
            String storedLength = snapshot.getString(0);
            if (!TextUtils.isEmpty(storedLength)) {
                length = Long.parseLong(storedLength);
            }
            inputStream = snapshot.getInputStream(1);
            return new CacheEntry(length, new ByteArrayInputStream(IOUtils.toByteArray(inputStream)));
        } catch (Exception e) {
            LogUtils.e(TAG, e.getMessage());
        } finally {
            if (null != inputStream) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public void clear() {
        if (null != mDiskCache) {
            try {
                mDiskCache.delete();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
