package com.douban.rexxar.resourceproxy.cache;

/**
 * 缓存池接口
 *
 * Created by luanqian on 15/11/2.
 */
interface ICache {

    /**
     * 根据url返回相应的CacheEntry
     *
     * @param url 资源的url
     * @return 与改url匹配的缓存数据
     */
    CacheEntry findCache(String url);

    /**
     * 移除单个缓存
     *
     * @param url 资源的url
     * @return 是否移除成功
     */
    boolean removeCache(String url);

}
