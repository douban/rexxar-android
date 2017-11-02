package com.douban.rexxar.utils;

import java.security.MessageDigest;

/**
 * Created by luanqian on 2017/11/2.
 */

public class MD5Utils {

    private static MessageDigest md5 = null;

    static {
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public static String getMd5(String str) {
        return getMd5(str.getBytes());
    }

    public static String getMd5(byte[] bytes) {
        try {
            byte[] bs = md5.digest(bytes);
            StringBuilder sb = new StringBuilder(40);
            for (byte x : bs) {
                if ((x & 0xff) >> 4 == 0) {
                    sb.append("0")
                            .append(Integer.toHexString(x & 0xff));
                } else {
                    sb.append(Integer.toHexString(x & 0xff));
                }
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return new String(bytes);
        }
    }
}
