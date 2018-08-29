/**
 *****************************************************************************
 * Copyright (C) 2018-2023 HD Corporation. All rights reserved
 * File        : ScreenOnOffReceiver.java
 *
 * Description : ScreenOnOffReceiver,
 *
 * Creation    : 2018-07-04
 * Author      : tanguilong@evergrande.cn
 * History     : 2018-07-04, Creation
 *****************************************************************************
 */
package cn.evergrande.it.hdremoteproxylib.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import cn.evergrande.it.hdremoteproxylib.HdNetworkMonitorService;
import cn.evergrande.it.hdremoteproxylib.evaluate.NetworkEvaluator;
import cn.evergrande.it.hdremoteproxylib.utils.ScreenObserver;
import cn.evergrande.it.logger.BHLog;


public class ScreenOnOffReceiver extends BroadcastReceiver {

    public final static String TAG = "ScreenOnOffReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        BHLog.i(TAG, String.format("onReceive: Screen state has changed, action=%s", action));

        HdNetworkMonitorService hdNetworkMonitorService = HdNetworkMonitorService.getInstance();
        if (Intent.ACTION_SCREEN_ON.equals(action)) { // 开屏
            hdNetworkMonitorService.notifyScreenStateChanged(ScreenObserver.SCREEN_EVENT_ON);
        } else if (Intent.ACTION_SCREEN_OFF.equals(action)) { // 锁屏
            hdNetworkMonitorService.notifyScreenStateChanged(ScreenObserver.SCREEN_EVENT_OFF);
        } else if (Intent.ACTION_USER_PRESENT.equals(action)) { // 解锁
            hdNetworkMonitorService.notifyScreenStateChanged(ScreenObserver.SCREEN_EVENT_USER_PRESENT);
        }
    }

}
