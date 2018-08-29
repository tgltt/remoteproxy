/**
 *****************************************************************************
 * Copyright (C) 2018-2023 HD Corporation. All rights reserved
 * File        : NetworkEvaluator.java
 *
 * Description : NetworkEvaluator,
 *
 * Creation    : 2018-07-09
 * Author      : tanguilong@evergrande.cn
 * History     : 2018-07-09, Creation
 *****************************************************************************
 */
package cn.evergrande.it.hdremoteproxylib.evaluate;


import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;

import com.file.zip.ZipUtil;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import cn.evergrande.it.hdremoteproxylib.RemoteProxy;
import cn.evergrande.it.hdremoteproxylib.network.NetworkConfigLoader;
import cn.evergrande.it.hdtoolkits.threadmanage.ThreadManager;
import cn.evergrande.it.hdtoolkits.utils.ZipUtils;
import cn.evergrande.it.logger.BHLog;
import cn.evergrande.it.logger.common.LogConstants;

public class NetworkEvaluator {

    public final static String TAG = "NetworkEvaluator";

    /**
     * 写文件方式：覆写原文件
     */
    public final static int WRITE_MODE_OVERRIDE = 0;
    /**
     * 写文件方式：追加写入原文件
     */
    public final static int WRITE_MODE_APPEND = 1;

    private final static String DEFAULT_NETWORK_EVALUATOR_FILE_NAME = "network_evaluate.txt";

    private final static SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
    private final static SimpleDateFormat sDateFormatTimestamp = new SimpleDateFormat("yyyyMMddhhmm");
    /**
     * 评估文件容量上限，单位：Byte
     */
    private final static long EVALUATOR_FILE_SIZE_VALVE = 10 * 1024 * 1024;
    /**
     * 时间间隔阀值，单位：ms
     * 评估文件打开时间到当前时间间隔，如果超过该间隔阀值，则检查评估文件大小是否超出限制
     */
    private final static long EVALUATOR_FILE_TIME_VALVE =  2 * 60 * 60 * 1000;

    private Context mContext;

    private File mNetworkEvaluateFile;
    private BufferedWriter mWriter;
    /**
     * 评估文件打开时间
     */
    private long mEvaluateFileOpenTime;
    /**
     * 评估文件写入模式，取值：WRITE_MODE_OVERRIDE、WRITE_MODE_APPEND
     */
    private int mWriteMode;

    private HandlerThread mHandlerThread;
    private Handler mHandler;

    private static class SingleHolder {
        public static NetworkEvaluator sNetworkEvaluator = new NetworkEvaluator();
    }

    private NetworkEvaluator() {
        mHandlerThread = new HandlerThread(TAG);
        mHandlerThread.start();

        mHandler = new Handler(mHandlerThread.getLooper());
    }

    public static NetworkEvaluator getInstance() {
        return SingleHolder.sNetworkEvaluator;
    }

    public void init(Context context) {
        mContext = context;

        loadNetworkEvaluatorConfig();
        initWriter(mWriteMode);
    }

    /**
     * 记录评估信息
     * @param TAG 模块TAG
     * @param message 评估信息
     * @param immediateSave false - 不立即保存本次评估信息
     *                      true - 立即保存本次评估信息
     */
    public void stat(final String TAG, final String message, final boolean immediateSave) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                doStat(TAG, message, immediateSave);
            }
        });
    }

    private synchronized void doStat(String TAG, String message, boolean immediateSave) {
        if (mWriter == null) {
            BHLog.i(TAG, "stat: mWriter is null");
            return;
        }
        // 格式化输出信息
        final String outMsg = String.format("%s %s: %s\r\n",
                                            sDateFormat.format(System.currentTimeMillis()),
                                            TAG,
                                            message);
        // 输出评估信息
        try {
            mWriter.write(outMsg, 0, outMsg.length());
        } catch (Throwable throwable) {
            BHLog.i(TAG, throwable.getMessage(), throwable);
        }

        int mode = WRITE_MODE_APPEND;

        long period = System.currentTimeMillis() - mEvaluateFileOpenTime;
        if (period >= EVALUATOR_FILE_TIME_VALVE) { // 到达本次检查时间
            File evaluateFile = new File(mNetworkEvaluateFile.getAbsolutePath());
            if (evaluateFile.length() >= EVALUATOR_FILE_SIZE_VALVE) { // 评估文件大小超出阀值
                mode = WRITE_MODE_OVERRIDE; // 后续评估信息将覆写当前文件
                immediateSave = true; // 立即回写文件
            }
        }

        if (immediateSave) { // 立即保存本次评估信息
            saveAndCloseWriter(mode == WRITE_MODE_OVERRIDE);
            initWriter(mode);
        }
    }

    public void release() {
        saveAndCloseWriter(false);
    }

    private void initWriter(int mode) {
        try {
            OutputStream os = new FileOutputStream(mNetworkEvaluateFile, mode == WRITE_MODE_APPEND);
            mWriter = new BufferedWriter(new OutputStreamWriter(os));

            mEvaluateFileOpenTime = System.currentTimeMillis();
        } catch (Throwable throwable) {
            BHLog.i(TAG, throwable.getMessage(), throwable);
        }
    }

    private void saveAndCloseWriter(boolean archive) {
        if (mWriter == null) {
            BHLog.i(TAG, "saveAndCloseWriter: mWriter == null");
            return;
        }

        try {
            mWriter.flush();
            mWriter.close();

            if (archive) { // 归档评估文件
                archiveEvaluateFile();
            }
        } catch (Throwable throwable) {
            Log.i(TAG, throwable.getMessage(), throwable);
        }
        // 释放文件句柄
        mWriter = null;
    }

    private void archiveEvaluateFile() throws IOException {
        final String networkEvaluateFileName = mNetworkEvaluateFile.getName();
        String timestamp = sDateFormatTimestamp.format(new Date());
        String outputFile = String.format("%s_%s.zip", networkEvaluateFileName, timestamp);

        File evaluateDir = mNetworkEvaluateFile.getParentFile();
        final String[] archiveFiles = evaluateDir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith(networkEvaluateFileName) && name.endsWith(".zip");
            }
        });

        if (archiveFiles != null && archiveFiles.length >= 3) { // 找出创建时间最早的评估文件并删除
            List<String> listArchiveFile = Arrays.asList(archiveFiles);

            Collections.sort(listArchiveFile, new Comparator<String>() {
                @Override
                public int compare(String file1, String file2) {
                    return file1.compareTo(file2);
                }
            });

            File eliminateFile = new File(listArchiveFile.get(0));
            boolean delResult = eliminateFile.delete();

            BHLog.i(TAG, String.format("Deleting %s, delete result: %s", eliminateFile.getAbsolutePath(), delResult));
        }

        File archiveFile = new File(evaluateDir, outputFile);
        ZipUtils.toZip(mNetworkEvaluateFile.getAbsolutePath(), new FileOutputStream(archiveFile), true);
    }

    private void loadNetworkEvaluatorConfig() {
        // 优先获取配置文件的配置属性，如果获取失败，再读取缺省配置属性
        Properties props = RemoteProxy.getInstance().getNetworkProperties();
        if (props == null || props.isEmpty()) {
            BHLog.i(TAG, "loadNetworkEvaluatorConfig: properties is null or empty");
            loadDefaultNetworkEvaluatorConfig();

            return;
        }
        // 获取网络模块评估文件路径
        String evaluateFilePath = props.getProperty(NetworkConfigLoader.PROPERTY_KEY_EVALUATE_FILE_PATH);
        String evaluateFileName = props.getProperty(NetworkConfigLoader.PROPERTY_KEY_EVALUATE_FILE_NAME);

        if (TextUtils.isEmpty(evaluateFilePath)) { // 如果评估文件路径配置项无效，则采用缺省评估文件保存路径
            evaluateFilePath = LogConstants.DEFAULT_LOG_DIR;
        }

        if (TextUtils.isEmpty(evaluateFileName)) { // 如果评估文件名称配置项无效，则采用缺省评估文件名称
            evaluateFileName = DEFAULT_NETWORK_EVALUATOR_FILE_NAME;
        }

        // 构建网络评估文件句柄
        mNetworkEvaluateFile = new File(evaluateFilePath, evaluateFileName);
        // 设置评估文件写入方式：覆盖写入或追加写入
        String writeModePropValue = props.getProperty(NetworkConfigLoader.PROPERTY_KEY_EVALUATE_FILE_WRITE_MODE);
        try {
            mWriteMode = Integer.parseInt(writeModePropValue);
        } catch (NumberFormatException nfex) {
            BHLog.i(TAG, nfex.getMessage(), nfex);
            mWriteMode = WRITE_MODE_APPEND; // 以追加方式写入
        }

        long fileSize = mNetworkEvaluateFile.length();
        if (fileSize >= EVALUATOR_FILE_SIZE_VALVE) {
            mWriteMode = WRITE_MODE_OVERRIDE;
        }

        BHLog.i(TAG, String.format("loadNetworkEvaluatorConfig: using %s%s%s to init network evaluate file", evaluateFilePath, File.separator, evaluateFileName));
    }

    private void loadDefaultNetworkEvaluatorConfig() {
        BHLog.i(TAG, "loadDefaultNetworkEvaluatorConfig: using default properties to init network evaluate file");
        // 构建网络评估文件句柄
        mNetworkEvaluateFile = new File(LogConstants.DEFAULT_LOG_DIR, DEFAULT_NETWORK_EVALUATOR_FILE_NAME);
    }

}
