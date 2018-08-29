/**
 *****************************************************************************
 * Copyright (C) 2018-2023 HD Corporation. All rights reserved
 * File        : StorageManager.java
 *
 * Description : StorageManager,
 *
 * Creation    : 2018-06-13
 * Author      : tanguilong@evergrande.cn
 * History     : 2018-06-13, Creation
 *****************************************************************************
 */
package cn.evergrande.it.hdremoteproxylib.utils;

import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import cn.evergrande.it.logger.BHLog;

public class StorageManager {

    public final static String TAG = "StorageManager";

    private SdcardManager mSdcardManager;

    private static class SingleInstanceHolder {
        public static StorageManager sStorageManager = new StorageManager();
    }

    private StorageManager() {
        mSdcardManager = new SdcardManager();
    }

    public static StorageManager getInstance() {
        return SingleInstanceHolder.sStorageManager;
    }

    /**
     * 获取内置Sdcard路径
     */
    public String getInnerSDCardPath() {
        return mSdcardManager.getInnerSDCardPath();
    }

    /**
     * 获取外置Sdcard路径
     */
    public List<String> getExtSDCardPath() {
        return mSdcardManager.getExtSDCardPath();
    }

    private class SdcardManager {

        public final static String TAG = "SdcardManager";

        /**
         * 获取内置SD卡路径
         */
        public String getInnerSDCardPath() {
            return Environment.getExternalStorageDirectory().getPath();
        }

        /**
         * 获取外置SD卡路径
         */
        public List<String> getExtSDCardPath() {
            List<String> sdcardList = new ArrayList<String>();
            try {
                Runtime rt = Runtime.getRuntime();

                Process proc = rt.exec("mount");

                InputStream is = proc.getInputStream();
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);

                String line;
                while ((line = br.readLine()) != null) {
                    if (!line.contains("extSdCard")) {
                        continue;
                    }

                    String[] arr = line.split(" ");
                    String path = arr[1];

                    File file = new File(path);
                    if (file.isDirectory()) {
                        sdcardList.add(path);
                    }
                }

                isr.close();
            } catch (Exception e) {
                BHLog.i(TAG, e.getMessage(), e);
            }

            return sdcardList;
        }
    }
}
