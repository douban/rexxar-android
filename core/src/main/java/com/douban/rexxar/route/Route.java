package com.douban.rexxar.route;

import android.text.TextUtils;

import com.douban.rexxar.utils.GsonHelper;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Route负责通过uri找到对应的html页面，一条Route包含一个uri的正则匹配规则和一个html地址。
 *
 * {@link #match(String)} 负责匹配uri;
 * {@link #getHtmlFile()} 返回html地址
 *
 *
 * Created by luanqian on 15/12/23.
 */
public class Route implements Serializable{

    private static final long serialVersionUID = 2l;

    @SerializedName("deploy_time")
    public String deployTime;
    @SerializedName("remote_file")
    public String remoteFile;
    @SerializedName("uri")
    public String uriRegex;

    public Route() {
    }

    /**
     * 匹配传入的uri，如果能匹配上则说明可以用这个html来显示
     *
     * @param url 匹配的uri
     * @return true: 能匹配上  false: 不能匹配上
     */
    public boolean match(String url) {
        try {
            Pattern pattern = Pattern.compile(uriRegex);
            Matcher matcher = pattern.matcher(url);
            return matcher.matches();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 返回html地址
     *
     * @return html的远程地址
     */
    public String getHtmlFile() {
        return remoteFile;
    }

    @Override
    public int hashCode() {
        return uriRegex.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (null == o) {
            return false;
        }
        if (!(o instanceof Route)) {
            return false;
        }
        return TextUtils.equals(this.uriRegex, ((Route) o).uriRegex);
    }

    @Override
    public String toString() {
        return GsonHelper.getInstance().toJson(this);
    }
}
