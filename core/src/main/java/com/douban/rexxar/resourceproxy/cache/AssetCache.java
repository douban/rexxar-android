package com.douban.rexxar.resourceproxy.cache;

import android.content.res.AssetManager;
import android.net.Uri;
import android.text.TextUtils;

import com.douban.rexxar.Constants;
import com.douban.rexxar.utils.AppContext;
import com.douban.rexxar.utils.LogUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 预置到asset中的只读cache
 * <p>
 * Created by luanqian on 15/11/2.
 */
public class AssetCache implements ICache {

    public static final String TAG = "AssetCache";

    public static AssetCache getInstance(String filePath) {
        return new AssetCache(filePath);
    }

    public static AssetCache getInstance() {
        return new AssetCache(null);
    }

    private String mFilePath;

    private AssetCache(String filePath) {
        mFilePath = filePath;
        if (TextUtils.isEmpty(mFilePath)) {
            mFilePath = Constants.DEFAULT_ASSET_FILE_PATH;
        }
    }

    @Override
    public CacheEntry findCache(String url) {
        // url为空，返回空
        if (TextUtils.isEmpty(url)) {
            return null;
        }

        // 不包含目录层级，返回空
        if (!url.contains(File.separator)) {
            return null;
        }

        List<String> pathSegments = Uri.parse(url)
                .getPathSegments();
        // 没有path无法命中asset缓存
        if (null == pathSegments) {
            return null;
        }

        StringBuilder pathStringBuilder = new StringBuilder();
        pathStringBuilder.append(mFilePath).append(File.separator);
        int size = pathSegments.size();
        if (size > 1) {
            pathStringBuilder.append(pathSegments.get(size - 2)).append(File.separator);
        }
        pathStringBuilder.append(pathSegments.get(size - 1));

        AssetManager assetManager = AppContext.getInstance()
                .getResources()
                .getAssets();
        try {
            InputStream inputStream = assetManager.open(pathStringBuilder.toString());
            CacheEntry cacheEntry = new CacheEntry(0, inputStream);
            LogUtils.i(TAG, "hit");
            return cacheEntry;
        } catch (IOException e) {
        }
        return null;
    }

    @Override
    public boolean removeCache(String url) {
        // do nothing
        return true;
    }

}
