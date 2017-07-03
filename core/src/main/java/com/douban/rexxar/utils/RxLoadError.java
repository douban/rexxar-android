package com.douban.rexxar.utils;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by luanqian on 2017/7/3.
 */

public class RxLoadError implements Parcelable, Cloneable {

    /**
     * 预定义的一些错误类型
     */
    public static final RxLoadError ROUTE_NOT_FOUND = new RxLoadError(0, "无法找到合适的Route");
    public static final RxLoadError HTML_NO_CACHE = new RxLoadError(1, "找不到html缓存");
    public static final RxLoadError HTML_DOWNLOAD_FAIL = new RxLoadError(2, "资源加载失败");
    public static final RxLoadError HTML_CACHE_INVALID = new RxLoadError(3, "html缓存失效");
    public static final RxLoadError JS_CACHE_INVALID = new RxLoadError(4, "js缓存失效");
    public static final RxLoadError UNKNOWN = new RxLoadError(10, "unknown");

    /**
     * 错误类型
     * 必须
     */
    public int type;

    /**
     * 错误信息, 用于显示到界面上
     * 必须
     */
    public String message;

    /**
     * 额外信息, 用于记录当时场景
     * 可选
     */
    public String extra;

    public RxLoadError(int type, String message) {
        this(type, message, "");
    }

    public RxLoadError(int type, String message, String extra) {
        this.type = type;
        this.message = message;
        this.extra = extra;
    }

    protected RxLoadError(Parcel in) {
        type = in.readInt();
        message = in.readString();
        extra = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public RxLoadError clone() {
        return new RxLoadError(this.type, this.message, this.extra);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(type);
        dest.writeString(message);
        dest.writeString(extra);
    }

    public static final Creator<RxLoadError> CREATOR = new Creator<RxLoadError>() {
        @Override
        public RxLoadError createFromParcel(Parcel in) {
            return new RxLoadError(in);
        }

        @Override
        public RxLoadError[] newArray(int size) {
            return new RxLoadError[size];
        }
    };
}
