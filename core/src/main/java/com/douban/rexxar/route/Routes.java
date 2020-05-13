package com.douban.rexxar.route;

import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by luanqian on 15/12/23.
 */
public class Routes {

    public int count;
    @SerializedName("deploy_time")
    public String deployTime;
    // 页面rexxar uri
    public List<Route> items = new ArrayList<>();
    // 局部rexxar uri
    @SerializedName("partial_items")
    public List<Route> partialItems = new ArrayList<>();

    public Routes() {
    }

    /**
     * @return  Routes是否为空
     */
    public boolean isEmpty() {
        return (null == items || items.isEmpty()) && (null == partialItems || partialItems.isEmpty());
    }

    /**
     * 判断deploy_time判断route是不是更新的
     * @param routes
     * @return
     */
    public boolean before(Routes routes) {
        if (null == routes) {
            return false;
        }
        if (TextUtils.isEmpty(routes.deployTime) || TextUtils.isEmpty(deployTime)){
            return true;
        }

        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM YYYY HH:mm:ss 'GTM'",
                Locale.ENGLISH);
        try {
            Date oldRouteDate = format.parse(deployTime);
            Date newRouteDate = format.parse(routes.deployTime);
            return oldRouteDate.before(newRouteDate);
        } catch (ParseException e) {
            e.printStackTrace();
            return true;
        }
    }
}
