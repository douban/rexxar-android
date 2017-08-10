package com.douban.rexxar.route;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

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
}
