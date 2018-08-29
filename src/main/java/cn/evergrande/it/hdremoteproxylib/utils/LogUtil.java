/**
 *****************************************************************************
 * Copyright (C) 2018-2023 HD Corporation. All rights reserved
 * File        : BHLog.java
 *
 * Description : LogUtil,
 *
 * Creation    : 2018-05-12
 * Author      : tanguilong@evergrande.cn
 * History     : 2018-05-12, Creation
 *****************************************************************************
 */
package cn.evergrande.it.hdremoteproxylib.utils;

import android.util.Log;

import cn.evergrande.it.logger.BHLog;
import cn.evergrande.it.logger.printer.FilePrinter;

public class LogUtil {

    private static String sPrefix = "[RemoteProxy] ";

    public static void v(String msg) {
        v(null, msg);
    }

    public static void v(String tag, String msg) {
        v(tag, msg, true);
    }

    public static void v(String tag, String msg, boolean printToRemote) {
        BHLog.v(tag, sPrefix + msg, printToRemote);
    }

    public static void v(String msg, Throwable throwable) {
        v(null, msg, throwable);
    }

    public static void v(String tag, String msg, Throwable throwable) {
        BHLog.v(tag, sPrefix + msg, throwable);
    }

    public static void v(String tag, String msg, boolean printThreadInfo, boolean printProcessInfo, boolean printStackTrace) {
        BHLog.v(tag, sPrefix + msg, printThreadInfo, printProcessInfo, printStackTrace);
    }

    public static void i(String msg) {
        i(null, msg);
    }

    public static void i(String tag, String msg) {
        i(tag, msg, true);
    }

    public static void i(String tag, String msg, boolean printToRemote) {
        BHLog.i(tag, sPrefix + msg, printToRemote);
    }

    public static void i(String msg, Throwable throwable) {
        i(null, msg, throwable);
    }

    public static void i(String tag, String msg, Throwable throwable) {
        BHLog.i(tag, sPrefix + msg, throwable);
    }

    public static void i(String tag, String msg, boolean printThreadInfo, boolean printProcessInfo, boolean printStackTrace) {
        BHLog.i(tag, sPrefix + msg, printThreadInfo, printProcessInfo, printStackTrace);
    }

    public static void d(String msg) {
        d(null, msg);
    }

    public static void d(String tag, String msg) {
        BHLog.d(tag, sPrefix + msg);
    }

    public static void d(String tag, String msg, boolean printToRemote) {
        BHLog.d(tag, sPrefix + msg, printToRemote);
    }

    public static void d(String msg, Throwable throwable) {
        d(null, msg, throwable);
    }

    public static void d(String tag, String msg, Throwable throwable) {
        d(tag, msg, throwable, true);
    }

    public static void d(String tag, String msg, Throwable throwable, boolean printToRemote) {
        BHLog.d(tag, sPrefix + msg, throwable, printToRemote);
    }

    public static void d(String tag, String msg, boolean printThreadInfo, boolean printProcessInfo, boolean printStackTrace) {
        BHLog.d(tag, sPrefix + msg, printThreadInfo, printProcessInfo, printStackTrace);
    }

    public static void w(String msg) {
        w(null, msg);
    }

    public static void w(String tag, String msg) {
        BHLog.w(tag, sPrefix + msg);
    }

    public static void w(String tag, String msg, boolean printToRemote) {
        BHLog.i(tag, sPrefix + msg, printToRemote);
    }

    public static void w(String msg, Throwable throwable) {
        w(null, msg, throwable);
    }

    public static void w(String tag, String msg, Throwable throwable) {
        BHLog.w(tag, sPrefix + msg, throwable);
    }

    public static void w(String tag, String msg, Throwable throwable, boolean printToRemote) {
        BHLog.w(tag, sPrefix + msg, throwable, printToRemote);
    }


    public static void w(String tag, String msg, boolean printThreadInfo, boolean printProcessInfo, boolean printStackTrace) {
        BHLog.w(tag, sPrefix + msg, printThreadInfo, printProcessInfo, printStackTrace);
    }

    public static void e(String msg) {
        e(null, msg);
    }

    public static void e(String tag, String msg) {
        e(tag, msg, true);
    }

    public static void e(String tag, String msg, boolean printToRemote) {
        BHLog.e(tag, sPrefix + msg, printToRemote);
    }

    public static void e(String msg, Throwable throwable) {
        e(null, msg, throwable, true);
    }

    public static void e(String msg, Throwable throwable, boolean printToRemote) {
        BHLog.e(sPrefix + msg, throwable, printToRemote);
    }

    public static void e(String tag, String msg, Throwable throwable) {
        BHLog.e(tag, sPrefix + msg, throwable);
    }

    public static void e(String tag, String msg, Throwable throwable, boolean printToRemote) {
        BHLog.e(tag, sPrefix + msg, throwable, printToRemote);
    }

    /**
     * 将缓存的日志立即写入到文件中，主要解决app被杀掉时日志丢失问题
     */
    public static void flushCacheLogToFile() {
        BHLog.flushCacheLogToFile();
    }
}
