/**
 *****************************************************************************
 * Copyright (C) 2018-2023 HD Corporation. All rights reserved
 * File        : NetworkStatusHandler.java
 *
 * Description : NetworkStatusHandler,
 *
 * Creation    : 2018-05-25
 * Author      : tanguilong@evergrande.cn
 * History     : 2018-05-25, Creation
 *****************************************************************************
 */
package cn.evergrande.it.hdremoteproxylib;

import java.util.List;

public class NetworkStatusHandler {

    public final static String TAG = "NetworkStatusHandler";

    /**
     * 网络状态变化处理器
     * @param connStatus 当前remote连接状态, 取值REMOTE_STATUS_UNKNOWN、REMOTE_STATUS_CONNECTED、REMOTE_STATUS_CONNECTING、<br/>
     *                   REMOTE_STATUS_DISCONNECTED, see IConnectionMonitor
     * @param netType 当前连接的网络类型, 取值NET_TYPE_UNKNOWN、NET_TYPE_MOBILE、NET_TYPE_WIFI、NET_TYPE_OTHER,
     *                see IConnectionMonitor
     * @param remoteType remote长连接, 取值REMOTE_CLOUD、REMOTE_ROUTE、REMOTE_LOGGER、REMOTE_ALL,
     *                   see RemoteProxy
     */
    public static void dealConnectionStatusChanged(int connStatus, int netType, int remoteType) {
        if (remoteType == RemoteProxy.REMOTE_CLOUD) {
            doRemoteCloudStatusChanged(connStatus,  netType);
        } else if (remoteType == RemoteProxy.REMOTE_ROUTE) {
            doRemoteRouteStatusChanged(connStatus, netType);
        } else if (remoteType == RemoteProxy.REMOTE_ALL) {
            doRemoteAllStatusChanged(connStatus, netType);
        }
    }

    private static void doRemoteCloudStatusChanged(int connStatus, int netType) {
        AbstractRemote remote = RemoteProxy.getInstance().getRemote(RemoteProxy.REMOTE_CLOUD);
        if (remote == null) {
            return;
        }
        // 通知上层关注网络变化的模块
        notifyConnectionMonitorObservers(remote, connStatus, netType);
    }

    private static void doRemoteRouteStatusChanged(int connStatus, int netType) {
        AbstractRemote remote = RemoteProxy.getInstance().getRemote(RemoteProxy.REMOTE_ROUTE);
        if (remote == null) {
            return;
        }
        // 通知上层关注网络变化的模块
        notifyConnectionMonitorObservers(remote, connStatus, netType);
    }

    private static void notifyConnectionMonitorObservers(AbstractRemote remote, int connStatus, int netType) {
        final List<AbstractRemote.IConnectionMonitor> connectionMonitors = remote.getConnectionMonitors();
        if (connectionMonitors == null || connectionMonitors.isEmpty()) {
            return;
        }

        for (AbstractRemote.IConnectionMonitor connectionMonitor : connectionMonitors) {
            connectionMonitor.onNetworkChanged(connStatus, netType, remote);
        }
    }

    private static void doRemoteAllStatusChanged(int connStatus, int netType) {
        doRemoteCloudStatusChanged(connStatus, netType);
        doRemoteRouteStatusChanged(connStatus, netType);
    }

}
