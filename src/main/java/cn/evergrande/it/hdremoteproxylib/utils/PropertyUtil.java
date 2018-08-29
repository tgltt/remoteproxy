/**
 *****************************************************************************
 * Copyright (C) 2018-2023 HD Corporation. All rights reserved
 * File        : PropertyUtil.java
 *
 * Description : PropertyUtil,
 *
 * Creation    : 2018-06-13
 * Author      : tanguilong@evergrande.cn
 * History     : 2018-06-13, Creation
 *****************************************************************************
 */
package cn.evergrande.it.hdremoteproxylib.utils;

import android.content.Context;
import android.text.TextUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import cn.evergrande.it.logger.BHLog;

public class PropertyUtil {

    public final static String TAG = "PropertyUtil";

    /**
     * 从内置/外置sdcard加载属性文件
     * @param file 属性文件的路径，可以是相对路径也可以是绝对路径
     * @return 属性文件中的属性集合
     */
    public static Properties loadConfigFromSdcard(String file) {
        if (TextUtils.isEmpty(file)) {
            BHLog.i(TAG, "loadNetworkConfig: file is null or empty");
            return null;
        }

        return loadConfigFromSdcard(new File(file));
    }

    /**
     * 从内置/外置sdcard加载属性文件
     * @param file 属性文件的file对象
     * @return 属性文件中的属性集合
     */
    public static Properties loadConfigFromSdcard(File file) {
        if (file == null || !file.exists()) {
            BHLog.i(TAG, String.format("loadNetworkConfigFromSdcard: file(%s) is null or not exists", file == null ? "null" : file.getAbsoluteFile()));
            return null;
        }

        Properties props = new Properties();

        try {
            FileInputStream fis = new FileInputStream(file);
            props.load(fis);
        } catch (Throwable throwable) {
            BHLog.i(TAG, throwable.getMessage(), throwable);
        }

        return props;
    }

    /**
     * 从Asset加载属性文件
     * @param ctx
     * @param file 属性文件的相对asset文件夹的路径
     * @return 属性文件中的属性集合
     */
    public static Properties loadConfigFromAsset(Context ctx, String file) {
        if (ctx == null) {
            BHLog.i(TAG, "loadNetworkConfigFromAsset: ctx == null");
            return null;
        }

        if (TextUtils.isEmpty(file)) {
            BHLog.i(TAG, "loadNetworkConfigFromAsset: file is null or empty");
            return null;
        }


        Properties props = new Properties();
        try {
            InputStream is = ctx.getAssets().open(file);
            props.load(is);
        } catch (Exception ex) {
            BHLog.i(TAG, ex.getMessage(), ex);
        }

        return props;
    }
}
