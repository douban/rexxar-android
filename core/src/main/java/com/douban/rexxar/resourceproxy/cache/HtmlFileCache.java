package com.douban.rexxar.resourceproxy.cache;

import android.content.Context;
import android.text.TextUtils;

import com.douban.rexxar.Constants;
import com.douban.rexxar.utils.AppContext;
import com.douban.rexxar.utils.LogUtils;
import com.douban.rexxar.utils.Utils;
import com.douban.rexxar.utils.io.IOUtils;

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

    public HtmlFileCache() {
    }

    @Override
    public CacheEntry findCache(String url) {
        if (TextUtils.isEmpty(url)) {
            return null;
        }
        File file = file(url);
        if (file.exists() && file.canRead()) {
            try {
                FileInputStream fileInputStream = new FileInputStream(file);
                byte[] bytes = IOUtils.toByteArray(fileInputStream);
                CacheEntry cacheEntry = new CacheEntry(file.length(), new ByteArrayInputStream(bytes));
                fileInputStream.close();
                LogUtils.i(TAG, "hit");
                return cacheEntry;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public boolean removeCache(String url) {
        LogUtils.i(TAG, "remove cache  : url " + url);
        File file = file(url);
        return file.exists() && file.delete();
    }

    /**
     * 保存文件缓存
     *
     * @param url         html的url
     * @param bytes html数据
     */
    public boolean saveCache(String url, byte[] bytes) {
        if (TextUtils.isEmpty(url) || null == bytes || bytes.length == 0) {
            return false;
        }
        File fileDir = fileDir();
        if (!fileDir.exists()) {
            if (!fileDir.mkdirs()) {
                return false;
            }
        }
        // 如果存在，则先删掉之前的缓存
        removeCache(url);
        File saveFile = null;
        try {
            saveFile = file(url);
            OutputStream outputStream = new FileOutputStream(saveFile);
            outputStream.write(bytes);
            outputStream.flush();
            outputStream.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            if (null != saveFile && saveFile.exists()) {
                saveFile.delete();
            }
        }
        return false;
    }

    /**
     * 清除html缓存
     *
     * @return whether clear cache successfully
     */
    public boolean clear() {
        File htmlDir = fileDir();
        if (!htmlDir.exists()) {
            return true;
        }
        File[] htmlFiles = htmlDir.listFiles();
        if (null == htmlFiles) {
            return true;
        }
        boolean processed = true;
        for (File file : htmlFiles) {
            if (!file.delete()) {
                processed = false;
            }
        }
        return processed;
    }

    /**
     * html存储目录
     *
     * @return html存储目录
     */
    public static File fileDir() {
        return new File(AppContext.getInstance().getDir(Constants.CACHE_HOME_DIR,
                Context.MODE_PRIVATE), Constants.DEFAULT_DISK_HTML_FILE_PATH);
    }

    /**
     * 单个html存储文件路径
     *
     * @param url html路径
     * @return html对应的存储文件
     */
    public static File file(String url) {
        String fileName = Utils.hash(url) + Constants.EXTENSION_HTML;
        return new File(fileDir(), fileName);
    }
}
