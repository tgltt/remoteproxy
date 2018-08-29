/**
 *****************************************************************************
 * Copyright (C) 2018-2023 HD Corporation. All rights reserved
 * File        : ScreenObserver.java
 *
 * Description : ScreenObserver,
 *
 * Creation    : 2018-07-05
 * Author      : tanguilong@evergrande.cn
 * History     : 2018-07-05, Creation
 *****************************************************************************
 */
package cn.evergrande.it.hdremoteproxylib.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;

import java.util.ArrayList;
import java.util.List;

import cn.evergrande.it.hdremoteproxylib.receiver.ScreenOnOffReceiver;
import cn.evergrande.it.logger.BHLog;

public class ScreenObserver {

    public final static String TAG = "ScreenObserver";

    /**
     * 屏幕空闲事件
     */
    public final static int SCREEN_EVENT_IDLE = 0;
    /**
     * 屏幕点亮事件
     */
    public final static int SCREEN_EVENT_ON = 1;
    /**
     * 屏幕灭屏事件
     */
    public final static int SCREEN_EVENT_OFF = 2;
    /**
     * 用户解锁事件
     */
    public final static int SCREEN_EVENT_USER_PRESENT = 3;


    private Context mContext;
    private ScreenOnOffReceiver mScreenReceiver;

    private List<ScreenStateListener> mScreenStateListeners;

    public ScreenObserver(Context context) {
        mContext = context;
        mScreenReceiver = new ScreenOnOffReceiver();

        mScreenStateListeners = new ArrayList<ScreenStateListener>();
    }

    public void startObserver() {
        registerScreenStateBroadcastReceiver();
        getScreenState();
    }

    public void shutdownObserver() {
        unregisterScreenStateBroadcastReceiver();

        mScreenStateListeners.clear();
    }

    /**
     * 获取screen状态
     */
    @SuppressLint("NewApi")
    private void getScreenState() {
        if (mContext == null) {
            return;
        }

        PowerManager manager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        if (manager.isScreenOn()) {
            notifyScreenStateListener(SCREEN_EVENT_ON);
        } else {
            notifyScreenStateListener(SCREEN_EVENT_OFF);
        }
    }

    public void registerScreenStateListener(ScreenStateListener listener) {
        if (listener == null) {
            BHLog.i(TAG, "registerScreenStateListener: listener is null, no need to add it");
            return;
        }

        if (mScreenStateListeners.contains(listener)) {
            BHLog.i(TAG, "registerScreenStateListener: already contains lisener(%s)");
            return;
        }

        mScreenStateListeners.add(listener);
    }

    public void unregisterScreenStateListener(ScreenStateListener listener) {
        if (listener == null) {
            BHLog.i(TAG, "unregisterScreenStateListener: listener is null, no need to delete it");
            return;
        }

        if (!mScreenStateListeners.contains(listener)) {
            BHLog.i(TAG, "registerScreenStateListener: not contains lisener(%s)");
            return;
        }

        mScreenStateListeners.remove(listener);
    }

    public void notifyScreenStateListener(int event) {
        switch (event) {
            case SCREEN_EVENT_ON:
                 notifyScreenStateOn();
                 break;
            case SCREEN_EVENT_OFF:
                 notifyScreenStateOff();
                 break;
            case SCREEN_EVENT_USER_PRESENT:
                 notifyScreenStateUserPresent();
                 break;
            case SCREEN_EVENT_IDLE:
            default:
        }
    }

    private void notifyScreenStateOn() {
        final List<ScreenStateListener>  screenStateListeners = mScreenStateListeners;
        if (screenStateListeners == null || screenStateListeners.isEmpty()) {
            BHLog.i(TAG, "notifyScreenStateOn: no liseners");
            return;
        }

        for (ScreenStateListener listener : screenStateListeners) {
            listener.onScreenOn();
        }
    }

    private void notifyScreenStateOff() {
        final List<ScreenStateListener>  screenStateListeners = mScreenStateListeners;
        if (screenStateListeners == null || screenStateListeners.isEmpty()) {
            BHLog.i(TAG, "notifyScreenStateOff: no liseners");
            return;
        }

        for (ScreenStateListener listener : screenStateListeners) {
            listener.onScreenOff();
        }
    }

    private void notifyScreenStateUserPresent() {
        final List<ScreenStateListener>  screenStateListeners = mScreenStateListeners;
        if (screenStateListeners == null || screenStateListeners.isEmpty()) {
            BHLog.i(TAG, "notifyScreenStateUserPresent: no liseners");
            return;
        }

        for (ScreenStateListener listener : screenStateListeners) {
            listener.onUserPresent();
        }
    }

    private void registerScreenStateBroadcastReceiver() {
        if (mContext != null) {
            IntentFilter filter = new IntentFilter();

            filter.addAction(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            filter.addAction(Intent.ACTION_USER_PRESENT);

            mContext.registerReceiver(mScreenReceiver, filter);
        }
    }

    private void unregisterScreenStateBroadcastReceiver() {
        if (mContext != null) {
            mContext.unregisterReceiver(mScreenReceiver);
        }
    }

    /**
     * 返回给调用者屏幕状态信息
     */
    public interface ScreenStateListener {
        public void onScreenOn();

        public void onScreenOff();

        public void onUserPresent();
    }

}
