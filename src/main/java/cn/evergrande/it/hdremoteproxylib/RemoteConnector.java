/**
 *****************************************************************************
 * Copyright (C) 2018-2023 HD Corporation. All rights reserved
 * File        : RemoteConnector.java
 *
 * Description : RemoteConnector,
 *
 * Creation    : 2018-07-06
 * Author      : tanguilong@evergrande.cn
 * History     : 2018-07-06, Creation
 *****************************************************************************
 */
package cn.evergrande.it.hdremoteproxylib;

import android.content.Context;
import android.text.TextUtils;

import java.util.Map;

import cn.evergrande.it.hdnetworklib.network.tcp.TcpCallback;
import cn.evergrande.it.hdnetworklib.network.tcp.TcpClient;
import cn.evergrande.it.hdremoteproxylib.evaluate.NetworkEvaluator;
import cn.evergrande.it.hdremoteproxylib.network.data.actions.ConnectCloudRemoteAction;
import cn.evergrande.it.hdremoteproxylib.network.data.actions.ConnectLoggerRemoteAction;
import cn.evergrande.it.hdremoteproxylib.network.data.actions.ConnectRouteRemoteAction;
import cn.evergrande.it.hdremoteproxylib.network.data.actions.DisconnectCloudRemoteAction;
import cn.evergrande.it.hdremoteproxylib.network.data.actions.DisconnectLoggerRemoteAction;
import cn.evergrande.it.hdremoteproxylib.network.data.actions.DisconnectRouteRemoteAction;
import cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine;
import cn.evergrande.it.hdremoteproxylib.RemoteProxy.ServerStatus;
import cn.evergrande.it.logger.BHLog;

import static cn.evergrande.it.hdremoteproxylib.RemoteProxy.REMOTE_ALL;
import static cn.evergrande.it.hdremoteproxylib.RemoteProxy.REMOTE_CLOUD;
import static cn.evergrande.it.hdremoteproxylib.RemoteProxy.REMOTE_LOGGER;
import static cn.evergrande.it.hdremoteproxylib.RemoteProxy.REMOTE_ROUTE;

public class RemoteConnector {

    public final static String TAG = "RemoteConnector";

    /*网络重连类型*/
    /**
     * 全部重连
     */
    public final static int RECONNECT_MODE_ALL = 0;
    /**
     * 重连云端
     */
    public final static int RECONNECT_MODE_CLOUD = 1;
    /**
     * 重连路由器
     */
    public final static int RECONNECT_MODE_ROUTE = 2;
    /**
     * 重连日志服务器
     */
    public final static int RECONNECT_MODE_LOGGER = 3;

    private Context mContext;

    private Map<Integer, AbstractRemote> mRemotes;
    private ServerStatus mServerStatus;

    private volatile boolean hasInit;

    static class SingleHodler {
        static RemoteConnector sRemoteConnector = new RemoteConnector();
    }

    private RemoteConnector() {

    }

    public static RemoteConnector getInstance() {
        return SingleHodler.sRemoteConnector;
    }

    public void init(Context context, Map<Integer, AbstractRemote> remotes, ServerStatus serverStatus) {
        mContext = context;
        mRemotes = remotes;
        mServerStatus = serverStatus;

        hasInit = true;
    }

    public boolean hasInit() {
        return hasInit;
    }

    /**
     * 重连接远端服务器，真正连接服务器的接口
     * @param mode RECONNECT_MODE_CLOUD：重连接云端，
     *              RECONNECT_MODE_ROUTE：重连接路由器，
     *              RECONNECT_MODE_LOGGER：重连接日志服务器，
     *              RECONNECT_MODE_ALL：连接所有服务器，包括但不限于云端、路由器、日志服务器等
     * @param closeOldConnectionIfExists true - 尝试先关闭原有长连接
     *                                       false - 尝试先关闭原有长连接，直接建立新的长连接
     */
    public void reconnect(int mode, boolean closeOldConnectionIfExists) {
        switch (mode) {
            case RECONNECT_MODE_CLOUD:
                 doReconnectCloud(closeOldConnectionIfExists);
                 break;
            case RECONNECT_MODE_ROUTE:
                 doReconnectRoute(closeOldConnectionIfExists);
                 break;
            case RECONNECT_MODE_LOGGER:
                 doReconnectLogger(closeOldConnectionIfExists);
                 break;
            case RECONNECT_MODE_ALL:
            default:
                 doReconnectAll(closeOldConnectionIfExists);
        }
    }

    private void doReconnectCloud(boolean closeOldConnectionIfExists) {
        if (mRemotes == null || mRemotes.isEmpty()) {
            BHLog.i(TAG, "doReconnectCloud: mRemotes is null or empty");
            return;
        }

        final AbstractRemote cloudRemote = mRemotes.get(REMOTE_CLOUD);
        if (cloudRemote == null) {
            BHLog.i(TAG, "doReconnectCloud: cloudRemote is null");
            return;
        }

        BHLog.i(TAG, "doReconnectCloud: cloudRemote is reconnecting");
        // 移除Action队列相关的Action
        ActionManager.getInstance().clearPreviousAction(new int[] {ActionManager.ACTION_CONNECT_CLOUD, ActionManager.ACTION_RECONNECT_CLOUD});
        // 根据标志判断是否先释放之前建立的连接
        if (closeOldConnectionIfExists) {
            ActionManager.getInstance().clearPreviousAction(new int[] {ActionManager.ACTION_DISCONNECT_CLOUD});
            doDisconnectCloud(cloudRemote);
        }
        // 重连云端
        doConnectCloudRemote(cloudRemote);
    }

    private void doReconnectRoute(boolean closeOldConnectionIfExists) {
        if (mRemotes == null || mRemotes.isEmpty()) {
            BHLog.i(TAG, "doReconnectRoute: mRemotes is null or empty");
            return;
        }

        final AbstractRemote routeRemote = mRemotes.get(REMOTE_ROUTE);
        if (routeRemote == null) {
            BHLog.i(TAG, "doReconnectRoute: routeRemote is null");
            return;
        }

        BHLog.i(TAG, "doReconnectRoute: routeRemote is reconnecting");
        // 移除Action队列相关的Action
        ActionManager.getInstance().clearPreviousAction(new int[] {ActionManager.ACTION_CONNECT_ROUTE, ActionManager.ACTION_RECONNECT_ROUTE});
        // 根据标志判断是否先释放之前建立的连接
        if (closeOldConnectionIfExists) {
            ActionManager.getInstance().clearPreviousAction(new int[] {ActionManager.ACTION_DISCONNECT_ROUTE});
            doDisconnectRoute(routeRemote);
        }
        // 重连路由器
        doConnectRouteRemote(routeRemote);
    }

    private void doReconnectLogger(boolean closeOldConnectionIfExists) {
        if (mRemotes == null || mRemotes.isEmpty()) {
            BHLog.i(TAG, "doReconnectLogger: mRemotes is null or empty");
            return;
        }

        final AbstractRemote loggerRemote = mRemotes.get(REMOTE_LOGGER);
        if (loggerRemote == null) {
            BHLog.i(TAG, "doReconnectLogger: loggerRemote is null");
            return;
        }

        BHLog.i(TAG, "doReconnectLogger: loggerRemote is reconnecting");
        // 移除Action队列相关的Action
        ActionManager.getInstance().clearPreviousAction(new int[] {ActionManager.ACTION_DISCONNECT_LOGGER, ActionManager.ACTION_CONNECT_LOGGER, ActionManager.ACTION_RECONNECT_LOGGER});

        doDisconnectLogger(loggerRemote);
        doConnectLoggerRemote(loggerRemote, null);
    }


    private void doReconnectAll(boolean closeOldConnectionIfExists) {
        doReconnectCloud(closeOldConnectionIfExists);
        doReconnectRoute(closeOldConnectionIfExists);
        doReconnectLogger(closeOldConnectionIfExists);
    }

    /**
     * 关闭指定Remote类型的长连接
     * @param remoteType 取值以下常量：REMOTE_CLOUD、REMOTE_ROUTE或REMOTE_LOGGER
     */
    public void disconnect(int remoteType) {
        if (remoteType != REMOTE_ALL && remoteType != REMOTE_CLOUD && remoteType != REMOTE_ROUTE && remoteType != REMOTE_LOGGER) {
            BHLog.i(TAG, String.format("disconnect: remoteType(%d) illegal", remoteType));
            return;
        }

        AbstractRemote remote = mRemotes.get(remoteType);
        if (remoteType == REMOTE_CLOUD) {
            doDisconnectCloud(remote);
        } else if (remoteType == REMOTE_ROUTE) {
            doDisconnectRoute(remote);
        } else if (remoteType == REMOTE_LOGGER) {
            doDisconnectLogger(remote);
        }
    }

    /**
     * 断开所有连接，包括但不限于云端、路由器、日志服务器等
     */
    public void disconnectAll() {
        if (mRemotes == null || mRemotes.isEmpty()) {
            BHLog.i(TAG, "disconnectAll: mRemotes is null or empty");
            return;
        }

        AbstractRemote cloudRemote = mRemotes.get(REMOTE_CLOUD);
        AbstractRemote routeRemote = mRemotes.get(REMOTE_ROUTE);
        AbstractRemote loggerRemote = mRemotes.get(REMOTE_LOGGER);

        doDisconnectCloud(cloudRemote);
        doDisconnectRoute(routeRemote);

        if (loggerRemote != null) {
            doDisconnectLogger(loggerRemote);
        }
    }

    private void doDisconnectCloud(AbstractRemote remote) { // 断开云端连接
        DisconnectCloudRemoteAction disconnectCloudRemoteAction = new DisconnectCloudRemoteAction(ActionManager.ACTION_DISCONNECT_CLOUD, ActionManager.SUB_ACTION_UNDEFINED, new Object[] {remote, mServerStatus});
        ActionManager.getInstance().postAction(disconnectCloudRemoteAction);
    }

    private void doDisconnectRoute(AbstractRemote remote) { // 断开路由器连接
        DisconnectRouteRemoteAction disconnectRouteRemoteAction = new DisconnectRouteRemoteAction(ActionManager.ACTION_DISCONNECT_ROUTE, ActionManager.SUB_ACTION_UNDEFINED, new Object[] {remote, mServerStatus});
        ActionManager.getInstance().postAction(disconnectRouteRemoteAction);
    }

    private void doDisconnectLogger(AbstractRemote remote) {
        DisconnectLoggerRemoteAction disconnectLoggerRemoteAction = new DisconnectLoggerRemoteAction(ActionManager.ACTION_DISCONNECT_LOGGER, ActionManager.SUB_ACTION_UNDEFINED, new Object[] {remote, mServerStatus});
        ActionManager.getInstance().postAction(disconnectLoggerRemoteAction);
    }

    /**
     * 获取Remote类型对应的重连模式
     * @param remoteType Remote类型，取值以下常量：REMOTE_CLOUD、REMOTE_ROUTE、REMOTE_LOGGER
     * @return Remote类型对应的重连模式，取值以下常量：RECONNECT_MODE_CLOUD、RECONNECT_MODE_ROUTE、RECONNECT_MODE_LOGGER和RECONNECT_MODE_ALL
     */
    public static int remoteType2ReconnectMode(int remoteType) {
        if (remoteType == REMOTE_CLOUD) {
            return RECONNECT_MODE_CLOUD;
        } else if (remoteType == REMOTE_ROUTE) {
            return RECONNECT_MODE_ROUTE;
        } else if (remoteType == REMOTE_LOGGER) {
            return RECONNECT_MODE_LOGGER;
        } else {
            return RECONNECT_MODE_ALL;
        }
    }

    private void doConnectRouteRemote(final AbstractRemote routeRemote) {
        new Thread() {
            @Override
            public void run() {
                RemoteProxy.Server routerServer = mServerStatus.getRouterServer();

                if (!routerServer.hasValidIpAddress()) { // 尚无有效路由网关地址，则尝试重新获取
                    RemoteProxy.getInstance().updateRouterGatewayIp(mContext);
                }

                TCPStateMachine stateMachine = routeRemote.getStateMachine();
                // 配置连接路由器参数
                Object[] args = new Object[] {
                        routeRemote,
                        stateMachine.getRetryConnectTimes(), 60l, // 重连参数: 最大重试次数、重试间隔
                        routerServer.getHostIpAddress(), routerServer.getPort()
                };

                ConnectRouteRemoteAction action = new ConnectRouteRemoteAction(ActionManager.ACTION_CONNECT_ROUTE, ActionManager.SUB_ACTION_UNDEFINED, args);
                ActionManager.getInstance().postAction(action);
            }
        }.start();
    }

    private void doConnectCloudRemote(final AbstractRemote cloudRemote) {
        new Thread() {
            @Override
            public void run() {
                RemoteProxy.Server secServer = mServerStatus.getSecServer();
                RemoteProxy.Server lbServer = mServerStatus.getLBServer();

                if (!secServer.hasValidIpAddress()) {
                    BHLog.i(TAG, String.format("doConnectCloudRemote: secServer(%s) has no valid ip address(%s:%s), retry getting ip address", secServer.getHostname(), secServer.getHostIpAddress(), secServer.getPort()));
                    secServer.transformHostname2IpAddressSync(secServer.getHostname());
                }
                // 强制刷新底层网络库地址
                TcpClient.getInstance().updateConnectionParams(TcpClient.MAP_KEY_SEC_SERVER_CONNECTION, secServer.getHostIpAddress(), secServer.getPort(), "", "", "");

                if (!lbServer.hasValidIpAddress()) {
                    BHLog.i(TAG, String.format("doConnectCloudRemote: lbServer(%s) has no valid ip address(%s:%s), retry getting ip address", lbServer.getHostname(), lbServer.getHostIpAddress(), lbServer.getPort()));
                    lbServer.transformHostname2IpAddressSync(lbServer.getHostname());
                }
                // 强制刷新底层网络库地址
                TcpClient.getInstance().updateConnectionParams(TcpClient.MAP_KEY_LB_SERVER_CONNECTION, lbServer.getHostIpAddress(), lbServer.getPort(), "", "", "");

                TCPStateMachine stateMachine = cloudRemote.getStateMachine();
                // 配置连接云端参数
                Object[] args = new Object[] {
                        cloudRemote,
                        stateMachine.getRetryConnectTimes(), 60l, // 重连参数: 最大重试次数、重试间隔
                        AbstractRemote.MODE_NON_BLOCK,
                        secServer.getHostIpAddress(), secServer.getPort(),
                        lbServer.getHostIpAddress(), lbServer.getPort()
                };

                ConnectCloudRemoteAction action = new ConnectCloudRemoteAction(ActionManager.ACTION_CONNECT_CLOUD, ActionManager.SUB_ACTION_UNDEFINED, args);
                ActionManager.getInstance().postAction(action);
            }
        }.start();
    }

    void doConnectLoggerRemote(final AbstractRemote loggerRemote, TcpCallback callback) {
        RemoteProxy.Server logServer = mServerStatus.getLogServer();

        if (!logServer.hasValidIpAddress()) { // 尚无有效日志服务器地址
            BHLog.i(TAG, "doConnectLoggerRemote: invalid logServer address");
            return;
        }

        TCPStateMachine stateMachine = loggerRemote.getStateMachine();
        // 配置连接日志服务器参数
        Object[] args = new Object[] {
                loggerRemote,
                stateMachine.getRetryConnectTimes(), 60l, // 重连参数: 最大重试次数、重试间隔
                logServer.getHostIpAddress(), logServer.getPort(),
                callback // 结果处理回调
        };

        ConnectLoggerRemoteAction action = new ConnectLoggerRemoteAction(ActionManager.ACTION_CONNECT_LOGGER, ActionManager.SUB_ACTION_UNDEFINED, args);
        ActionManager.getInstance().postAction(action);
    }

    /**
     * 更新日志Remote
     * @param ip 日志服务器IP地址
     * @param port 日志服务器端口
     */
    public void updateLoggerRemote(AbstractRemote loggerRemote, String ip, int port) {
        if (TextUtils.isEmpty(ip) || port < 0) {
            BHLog.i(TAG, String.format("updateLoggerRemote: params illegal, ip=%s, port=%d", ip, port));
            return;
        }

        RemoteProxy.Server logServer = mServerStatus.getLogServer();
        // 更新日志服务器信息
        logServer.setHostIpAddress(ip);
        logServer.setPort(port);

        boolean needRebuildConnection = !loggerRemote.isTheSameRemote(ip, "" + port);
        if (needRebuildConnection) { // 重连日志服务器
            loggerRemote.setRemoteAddressPort(ip, "" + port);
            doReconnectLogger(true);
        }
    }

    public void release() {
        disconnectAll();
    }
}