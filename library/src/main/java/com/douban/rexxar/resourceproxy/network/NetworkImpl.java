package com.douban.rexxar.resourceproxy.network;

import com.douban.rexxar.Rexxar;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by luanqian on 15/10/29.
 */
public class NetworkImpl implements INetwork {

    public static final String TAG = NetworkImpl.class.getSimpleName();

    OkHttpClient mOkHttpClient;

    public NetworkImpl() {
        mOkHttpClient = Rexxar.getOkHttpClient();
    }

    @Override
    public Response handle(Request request) throws IOException {
        try {
            Response response = RexxarContainerAPIHelper.handle(request);
            if (null == response) {
                response = mOkHttpClient.newCall(request).execute();
            }
            return response;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

}
