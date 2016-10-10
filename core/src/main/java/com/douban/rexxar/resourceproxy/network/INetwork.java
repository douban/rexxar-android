package com.douban.rexxar.resourceproxy.network;

import java.io.IOException;

import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by luanqian on 15/10/29.
 */
public interface INetwork {

    /**
     * handle api request, should be sync
     *
     * @param request
     * @return
     */
    Response handle(Request request) throws IOException;
}
