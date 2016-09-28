package com.douban.rexxar.resourceproxy.network;

import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by luanqian on 16/9/26.
 */
public interface RexxarContainerAPI {

    /**
     * url for the api
     *
     * @return
     */
    String getPath();

    /**
     * response for the api
     *
     * @param request
     * @return
     */
    Response call(Request request);
}
