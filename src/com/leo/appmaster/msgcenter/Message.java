package com.leo.appmaster.msgcenter;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 消息中心数据结构
 * Created by Jasper on 2015/9/10.
 */
public class Message {
    // 更新日志
    public static final String CATEGORY_UPDATE = "001";

    // 活动时间
    public String time;
    // 类型名称
    public String categoryName;
    // 类型表示
    public String categoryCode;
    // 描述信息
    public String description;
    // 图片地址
    public String imageUrl;
    // 页面跳转地址
    public String jumpUrl;
    // 活动下线时间
    public String offlineTime;
    // 活动标题
    public String title;
    // 资源包地址
    public String resUrl;
    // 活动id
    public int msgId;

    // 未读标志
    public boolean unread = true;

    public boolean isCategoryUpdate() {
        return CATEGORY_UPDATE.equals(categoryCode);
    }

    @Override
    public String toString() {
        return "msgId: " + msgId + " | categoryCode: " + categoryCode + " | title: " + title +
                " | categoryName: " + categoryName;
    }

    /**
     * 是否已经下线
     * @return
     */
    public boolean isOffline() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            Date date = format.parse(time);
            long ts = date.getTime();
            return System.currentTimeMillis() > ts;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return false;
    }
}
