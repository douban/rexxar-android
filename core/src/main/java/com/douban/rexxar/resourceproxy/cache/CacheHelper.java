package com.douban.rexxar.resourceproxy.cache;

import android.net.Uri;
import android.text.TextUtils;

import com.douban.rexxar.Constants;
import com.douban.rexxar.route.RouteManager;
import com.douban.rexxar.utils.LogUtils;
import com.douban.rexxar.utils.MD5Utils;
import com.douban.rexxar.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * Created by luanqian on 15/11/2.
 */
public class CacheHelper {

    public static final String TAG = CacheHelper.class.getSimpleName();

    private static CacheHelper sInstance;

    private CacheHelper() {
        if (null == mInternalCache) {
            mInternalCache = new InternalCache();
        }
        if (null == mInternalHtmlCache) {
            mInternalHtmlCache = new HtmlFileCache();
        }
    }

    public static CacheHelper getInstance() {
        if (null == sInstance) {
            synchronized (CacheHelper.class) {
                if (null == sInstance) {
                    sInstance = new CacheHelper();
                }
            }
        }
        return sInstance;
    }

    /**
     * internal cache
     */
    private InternalCache mInternalCache;
    private HtmlFileCache mInternalHtmlCache;
    /**
     * register cache
     */
    private List<ICache> mCaches = new ArrayList<>();
    /**
     * 是否使用缓存
     */
    private boolean mCacheEnabled = true;

    /**
     * Register additional readable cache
     *
     * @param cache
     */
    public void registerCache(ICache cache) {
        if (null != cache) {
            mCaches.add(cache);
        }
    }

    /**
     * 查找缓存
     *
     * @param url
     * @return
     */
    public CacheEntry findCache(String url) {
        if (TextUtils.isEmpty(url)) {
            return null;
        }
        if (!checkUrl(url)) {
            return null;
        }
        // 如果是html文件，则查找html缓存
        if (url.contains(Constants.EXTENSION_HTML)) {
            return findHtmlCache(url);
        }
        CacheEntry result = null;
        // 遍历内部缓存
        result = mInternalCache.findCache(url);
        if (null != result) {
            return result;
        }
        // 遍历外部缓存
        for (ICache cache : mCaches) {
            result = cache.findCache(url);
            if (null != result && result.isValid()) {
                return result;
            }
        }
        return result;
    }

    /**
     * 查找html缓存
     *
     * @param url
     * @return
     */
    public CacheEntry findHtmlCache(String url) {
        if (TextUtils.isEmpty(url)) {
            return null;
        }
        // html地址需要去掉参数
        url = Uri.parse(url).buildUpon().clearQuery().build().toString();
        if (!checkUrl(url)) {
            return null;
        }
        CacheEntry result = null;
        // 遍历外部缓存
        for (ICache cache : mCaches) {
            result = cache.findCache(url);
            if (null != result && result.isValid()) {
                return result;
            }
        }
        // 遍历内部缓存
        result = mInternalHtmlCache.findCache(url);
        return result;
    }

    /**
     * 是否缓存了
     *
     * @param url html地址
     * @return
     */
    public boolean hasHtmlCached(String url) {
        CacheEntry cacheEntry = findHtmlCache(url);
        if (null != cacheEntry) {
            cacheEntry.close();
            return true;
        }
        return false;
    }

    /**
     * Just save to internalCache
     *
     * @param url
     * @param bytes
     */
    public void saveCache(String url, byte[] bytes) {
        if (TextUtils.isEmpty(url) || null == bytes || bytes.length == 0) {
            return;
        }
        if (!checkUrl(url)) {
            return;
        }
        mInternalCache.putCache(url, bytes);
    }

    /**
     * Just save html file
     *
     * @param url
     * @param bytes
     */
    public boolean saveHtmlCache(String url, byte[] bytes) {
        if (TextUtils.isEmpty(url) || null == bytes || bytes.length == 0) {
            return false;
        }
        if (!checkUrl(url)) {
            return true;
        }
        if (!checkHtmlFile(url, bytes)) {
            LogUtils.i(TAG, "html file check fail : url: " + url + ", bytes md5: " + MD5Utils.getMd5(bytes));
            return false;
        }
        return mInternalHtmlCache.saveCache(url, bytes);
    }

    // 建议html文件的命名规则是：%filename%-%hash code%.html
    private boolean checkHtmlFile(String url, byte[] bytes) {
        String fileName = Uri.parse(url).getLastPathSegment();
        // 不是以html为结尾的，则不进行校验
        if (!fileName.endsWith(Constants.EXTENSION_HTML)) {
            return true;
        }
        try {
            String hashCode = fileName.split("\\.")[0].split("-")[1];
            // 提取到hash code
            if (!TextUtils.isEmpty(hashCode)) {
                return MD5Utils.getMd5(bytes).startsWith(hashCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
        return true;
    }

    /**
     * Clear caches
     */
    public void clearCache() {
        // clear file caches
        mInternalCache.clear();
        mInternalCache = new InternalCache();
        // clear html files
        mInternalHtmlCache.clear();
    }

    /**
     * 删除单个资源缓存
     *
     * @param url 资源地址
     */
    public void removeInternalCache(String url) {
        mInternalCache.removeCache(url);
    }

    /**
     * 删除单个html缓存
     *
     * @param url html地址
     */
    public void removeHtmlCache(String url) {
        mInternalHtmlCache.removeCache(url);
    }

    /**
     * 是否能够缓存
     *
     * @param url
     * @return
     */
    public boolean checkUrl(String url) {
        if (TextUtils.isEmpty(url)) {
            LogUtils.i(TAG, "can not cache, url = " + url);
            return false;
        }

        // 获取文件名
        String fileName;
        if (!url.contains(File.separator)) {
            fileName = url;
        } else {
            fileName = Uri.parse(url)
                    .getLastPathSegment();
            if (TextUtils.isEmpty(fileName)) {
                fileName = Uri.parse(url)
                        .getHost();
            }
        }
        // 如果文件名为空，则不能缓存
        if (TextUtils.isEmpty(fileName)) {
            LogUtils.i(TAG, "can not cache, fileName is null, url = " + url);
            return false;
        }
        // 如果文件名不为空，且后缀为能够缓存的类型，则可以缓存
        for (String extension : Constants.CACHE_FILE_EXTENSION) {
            if (fileName.endsWith(extension)) {
                LogUtils.i(TAG, "can cache url = " + url);
                return true;
            }
        }
        // 默认不能缓存
        LogUtils.i(TAG, "can not cache, extension not match, url = " + url);
        return false;
    }

    /**
     * 禁用缓存
     */
    public void enableCache(boolean enableCache) {
        mCacheEnabled = enableCache;
    }

    public boolean cacheEnabled() {
        return mCacheEnabled;
    }

    /**
     * 根据url获取缓存的key
     *
     * @param url
     * @return
     */
    public String urlToKey(String url) {
        // url为空,返回null
        if (TextUtils.isEmpty(url)) {
            return null;
        }
        // uri有文件后缀则会缓存
        try {
            // 如果只有文件名
            if (!url.contains(File.separator)) {
                return url;
            }
            String key = Utils.hash(url);
            LogUtils.i(TAG, "url : " + url + " ; key : " + key);
            return key;
        } catch (Exception e) {
            LogUtils.e(TAG, e.getMessage());
        }

        return null;
    }
}
