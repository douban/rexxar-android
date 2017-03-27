package com.douban.rexxar.resourceproxy.network;

import android.text.TextUtils;

import com.douban.rexxar.Constants;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Request;
import okhttp3.Response;

/**
 *
 * Created by luanqian on 16/8/16.
 */
public class RexxarContainerAPIHelper {

    public static Response handle(Request request, List<RexxarContainerAPI> containerAPIs) {
        if (null == containerAPIs) {
            return null;
        }
        for (RexxarContainerAPI api : containerAPIs) {
            String requestUrl = request.url().toString();
            int fragment = requestUrl.lastIndexOf('#');
            if (fragment > 0) {
                requestUrl = requestUrl.substring(0, fragment);
            }

            int query = requestUrl.lastIndexOf('?');
            if (query > 0) {
                requestUrl = requestUrl.substring(0, query);
            }
            if (!TextUtils.equals(Constants.CONTAINER_API_BASE + api.getPath(), requestUrl)) {
                continue;
            }
            Response response = api.call(request);
            if (null != response) {
                return response;
            }
        }
        return null;
    }

}
