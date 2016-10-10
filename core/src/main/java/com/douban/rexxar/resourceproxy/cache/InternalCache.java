package com.douban.rexxar.resourceproxy.cache;

import android.content.Context;
import android.text.TextUtils;

import com.douban.rexxar.utils.AppContext;
import com.douban.rexxar.utils.io.IOUtils;
import com.douban.rexxar.Constants;
import com.douban.rexxar.utils.LogUtils;
import com.jakewharton.disklrucache.DiskLruCache;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 默认缓存池，js，css，png等资源会用默认缓存池来存储，是有大小限制的{@code DiskLruCache}
 *
 * 存储位置在/data/data/cache下
 *
 * Created by luanqian on 15/10/29.
 */
class InternalCache implements ICache {

    public static final String TAG = InternalCache.class.getSimpleName();

    private DiskLruCache mDiskCache;

    public InternalCache() {
        File directory = new File(AppContext.getInstance().getDir(Constants.CACHE_HOME_DIR,
                Context.MODE_PRIVATE), Constants.DEFAULT_DISK_FILE_PATH);
        try {
            mDiskCache = DiskLruCache.open(directory, Constants.VERSION, 2,
                    Constants.CACHE_SIZE);
        } catch (IOException e) {
            LogUtils.e(TAG, e.getMessage());
        }
    }

    public boolean putCache(String url, byte[] bytes) {
        if (TextUtils.isEmpty(url) || null == mDiskCache) {
            return false;
        }
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

    @Override
    public CacheEntry findCache(String url) {
        return getCache(url);
    }

    @Override
    public boolean removeCache(String url) {
        try {
            LogUtils.i(TAG, "remove cache  : url " + url);
            return mDiskCache.remove(CacheHelper.getInstance().urlToKey(url));
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

}
