/**
 *****************************************************************************
 * Copyright (C) 2018-2023 HD Corporation. All rights reserved
 * File        : ActionHandler.java
 *
 * Description : ActionHandler,
 *
 * Creation    : 2018-05-29
 * Author      : tanguilong@evergrande.cn
 * History     : 2018-05-29, Creation
 *****************************************************************************
 */
package cn.evergrande.it.hdremoteproxylib;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import cn.evergrande.it.hdremoteproxylib.network.data.actions.ConnectCloudRemoteAction;
import cn.evergrande.it.hdremoteproxylib.network.data.actions.ConnectLoggerRemoteAction;
import cn.evergrande.it.hdremoteproxylib.network.data.actions.ConnectRouteRemoteAction;
import cn.evergrande.it.hdremoteproxylib.network.data.actions.DisconnectCloudRemoteAction;
import cn.evergrande.it.hdremoteproxylib.network.data.actions.DisconnectLoggerRemoteAction;
import cn.evergrande.it.hdremoteproxylib.network.data.actions.DisconnectRouteRemoteAction;
import cn.evergrande.it.hdremoteproxylib.network.data.actions.ReconnectCloudRemoteAction;
import cn.evergrande.it.hdremoteproxylib.network.data.actions.ReconnectRouteRemoteAction;
import cn.evergrande.it.hdremoteproxylib.network.data.actions.ScreenOnAction;
import cn.evergrande.it.hdremoteproxylib.network.data.actions.UserPresentAction;
import cn.evergrande.it.logger.BHLog;

public class ActionHandler extends Handler {

    public final static String TAG = "ActionHandler";

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case ActionManager.ACTION_CONNECT_CLOUD: // 处理连接云端动作
                 doConnectCloud(msg);
                 break;
            case ActionManager.ACTION_CONNECT_ROUTE: // 处理连接路由器动作
                 doConnectRoute(msg);
                 break;
            case ActionManager.ACTION_CONNECT_LOGGER: // 处理连接日志服务器动作
                 doConnectLogger(msg);
                 break;
            case ActionManager.ACTION_DISCONNECT_CLOUD: // 处理断开云端连接动作
                 doDisconnectCloud(msg);
                 break;
            case ActionManager.ACTION_DISCONNECT_ROUTE: // 处理断开路由器连接动作
                 doDisconnectRoute(msg);
                 break;
            case ActionManager.ACTION_DISCONNECT_LOGGER: // 处理断开日志服务器动作
                 doDisconnectLogger(msg);
                 break;
            case ActionManager.ACTION_RECONNECT_CLOUD: // 处理重连接云端动作
                 doReconnectCloud(msg);
                 break;
            case ActionManager.ACTION_RECONNECT_ROUTE: // 处理重连接路由器动作
                 doReconnectRoute(msg);
                 break;
            case ActionManager.ACTION_SCREEN_ON: // 屏幕点亮事件
                 doScreenOn(msg);
                 break;
            case ActionManager.ACTION_SCREEN_OFF: // 屏幕灭屏事件
                 doScreenOff(msg);
                 break;
            case ActionManager.ACTION_USER_PRESENT: // 用户解锁事件
                 doUserPresent(msg);
                 break;
            case ActionManager.ACTION_IDLE: // 空闲动作
            default:
        }
    }

    public ActionHandler(Looper looper) {
        super(looper);
    }

    private void doConnectCloud(Message msg) {
        if (msg.obj == null) {
            BHLog.i(TAG, "doConnectCloud: msg.obj is null");
            return;
        }

        ((ConnectCloudRemoteAction) msg.obj).run();
    }

    private void doConnectRoute(Message msg) {
        if (msg.obj == null) {
            BHLog.i(TAG, "doConnectRoute: msg.obj is null");
            return;
        }

        ((ConnectRouteRemoteAction) msg.obj).run();
    }

    private void doConnectLogger(Message msg) {
        if (msg.obj == null) {
            BHLog.i(TAG, "doConnectLogger: msg.obj is null");
            return;
        }

        ((ConnectLoggerRemoteAction) msg.obj).run();
    }

    private void doDisconnectCloud(Message msg) {
        if (msg.obj == null) {
            BHLog.i(TAG, "doDisconnectCloud: msg.obj is null");
            return;
        }

        ((DisconnectCloudRemoteAction) msg.obj).run();
    }

    private void doDisconnectRoute(Message msg) {
        if (msg.obj == null) {
            BHLog.i(TAG, "doDisconnectRoute: msg.obj is null");
            return;
        }

        ((DisconnectRouteRemoteAction) msg.obj).run();
    }

    private void doDisconnectLogger(Message msg) {
        if (msg.obj == null) {
            BHLog.i(TAG, "doDisconnectLogger: msg.obj is null");
            return;
        }

        ((DisconnectLoggerRemoteAction) msg.obj).run();
    }

    private void doReconnectCloud(Message msg) {
        if (!(msg.obj instanceof CloudRemote)) {
            BHLog.i(TAG, "doReconnectCloud: msg.obj is null");
            return;
        }

        ((ReconnectCloudRemoteAction) msg.obj).run();
    }

    private void doReconnectRoute(Message msg) {
        if (msg.obj == null) {
            BHLog.i(TAG, "doReconnectRoute: msg.obj is null");
            return;
        }

        ((ReconnectRouteRemoteAction) msg.obj).run();
    }

    private void doScreenOn(Message msg) {
        if (msg.obj == null) {
            BHLog.i(TAG, "doScreenOn: msg.obj is null");
            return;
        }

        ((ScreenOnAction) msg.obj).run();
    }

    private void doScreenOff(Message msg) {
        if (msg.obj == null) {
            BHLog.i(TAG, "doScreenOff: msg.obj is null");
            return;
        }
    }

    private void doUserPresent(Message msg) {
        if (msg.obj == null) {
            BHLog.i(TAG, "doUserPresent: msg.obj is null");
            return;
        }

        ((UserPresentAction) msg.obj).run();
    }
}
