/**
 *****************************************************************************
 * Copyright (C) 2018-2023 HD Corporation. All rights reserved
 * File        : LoggerRemote.java
 *
 * Description : LoggerRemote,
 *
 * Creation    : 2018-05-14
 * Author      : tanguilong@evergrande.cn
 * History     : 2018-05-14, Creation
 *****************************************************************************
 */
package cn.evergrande.it.hdremoteproxylib;

import android.content.Context;

import cn.evergrande.it.hdnetworklib.api.model.protocal.ErrorCode;
import cn.evergrande.it.hdnetworklib.network.tcp.TcpCallback;
import cn.evergrande.it.hdnetworklib.network.tcp.TcpClient;
import cn.evergrande.it.hdnetworklib.network.tcp.TcpConnection;
import cn.evergrande.it.hdremoteproxylib.RemoteProxy.Server;
import cn.evergrande.it.hdremoteproxylib.RemoteProxy.ServerStatus;
import cn.evergrande.it.hdremoteproxylib.interfaces.ICheckInCallback;
import cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine;
import cn.evergrande.it.logger.BHLog;

/**
 * LoggerRemote是个特殊的Remote，只实现心跳检测，其他的操作不做实现
 */
public class LoggerRemote extends BaseRemote {

    public final static String TAG = "LoggerRemote";

    static class SingletonHolder {
        public static LoggerRemote sLoggerRemote = new LoggerRemote();
    }

    private LoggerRemote() {

    }

    static LoggerRemote getInstance() {
        return LoggerRemote.SingletonHolder.sLoggerRemote;
    }

    @Override
    public void init(Context context, ServerStatus serverStatus) {
        mContext = context;
        mServerStatus = serverStatus;
        setRemoteType(RemoteProxy.REMOTE_LOGGER);
    }

    @Override
    public void connect(Object[] args) {
        int curState = mStateMachine.getState().getState();
        if (curState == TCPStateMachine.TCP_STATE_CONNECTED) {
            BHLog.i(TAG, "connect: loggerRemote is already connected");
            return;
        }

        if (!checkConnectParams(args)) {
            BHLog.i(TAG, "connect: checkConnectParams return false");
            return;
        }

        super.connect(args);

        connectToLoggerServer((ICheckInCallback) args[0]/*处理结果回调*/, (String) args[1]/*IP地址*/, (int) args[2]/*端口*/);
    }

    /**
     * 连接路由器
     */
    public void connectToLoggerServer(final ICheckInCallback callback, final String loggerIp, final int loggerPort) {
        BHLog.i(TAG, "connectToRouter: starting...");

        notifyChannelChanged(IEstablishChannelStateMonitor.CHANNEL_STAGE_START);
        // 记录路由器的地址
        setRemoteAddressPort(loggerIp, String.valueOf(loggerPort));

        final Server loggerServer = mServerStatus.getRouterServer();
        loggerServer.setHostIpAddress(loggerIp);
        loggerServer.setPort(loggerPort);
        // 创建路由器长链接
        TcpClient.getInstance().createTcpConnection(loggerIp, loggerPort, true, new TcpCallback() {
            @Override
            public void onSuccess(Object data) {
                BHLog.i(TAG, String.format("connectToLoggerServer:onSuccess: connect logger success! logger_host_ip=%s", loggerIp));
                if (callback != null) {
                    ICheckInCallback.CheckinResult checkinResult = new ICheckInCallback.CheckinResult();
                    checkinResult.setBizServerAddress(loggerIp);
                    checkinResult.setBizServerPort(String.valueOf(loggerPort));

                    loggerServer.setCheckInStatus(true);

                    callback.onSuccess(checkinResult);
                }

                notifyChannelChanged(IEstablishChannelStateMonitor.CHANNEL_STAGE_SUCCESS);
            }

            @Override
            public void onError(int errorCode) {
                BHLog.i(TAG, String.format("connectToLoggerServer:onError: connect logger fail!!!  logger_host_ip=", loggerIp));
                if (callback != null) {
                    callback.onError(ErrorCode.ERROR_CODE_91001);
                }

                loggerServer.setCheckInStatus(false);

                notifyChannelChanged(IEstablishChannelStateMonitor.CHANNEL_STAGE_FAIL);
            }
        });
    }

    /**
     * 检查@see connect参数数组, 参数要求见@see connect
     * @return true, 参数数组符合要求,
     *         false, 参数数组不符合要求
     */
    @Override
    protected boolean checkConnectParams(Object[] args) {
        if (args == null || args.length < 3) {
            return false;
        }

        if (!(args[0] instanceof ICheckInCallback)) {
            BHLog.i(TAG, String.format("checkConnectParams: args[0](%s) is not ICheckInCallback", args[0]));
            return false;
        }

        if (!(args[1] instanceof String)) {
            BHLog.i(TAG, String.format("checkConnectParams: args[1](%s) is not String", args[1]));
            return false;
        }

        if (!(args[2] instanceof Integer)) {
            BHLog.i(TAG, String.format("checkConnectParams: args[2](%s) is not Integer", args[2]));
            return false;
        }

        return true;
    }

    @Override
    public void disconnect() {

    }

    @Override
    public void sendHeartbeat() {
        super.sendHeartbeat();

        BHLog.i(TAG, " sendHeartbeat start");

        Server logServer = mServerStatus.getLogServer();
        if (logServer.isOnline()) {
            BHLog.i(TAG, "loggerChannel is not online, stop sendHeartbeat and state machine disconnect");
            mStateMachine.disconnect();
            return;
        }

        final TcpClient tcpClient = TcpClient.getInstance();

        String netAddress = String.format("%s:%s", logServer.getHostIpAddress(), logServer.getPort());
        //发送心跳
        tcpClient.sendHeartbeatMsg(netAddress);
        //心跳回包校验
        TcpConnection tcpConnection = tcpClient.getTcpConnection(netAddress);
        if (tcpConnection == null) {
            BHLog.i(TAG, "tcpConnection is null, stop sendHeartbeat and state machine disconnect");
            mStateMachine.disconnect();
            return;
        }

        int tempHeartbeatFlag = tcpConnection.getHeartbeatFlag() - 1;

        BHLog.i(TAG, String.format("%s, heartbeatFlag = %s", netAddress, tempHeartbeatFlag));

        if (tempHeartbeatFlag <= 0) {
            BHLog.i(TAG, "tempHeartbeatFlag <= 0, sendHeartbeat stop and state machine disconnect");
            mStateMachine.disconnect();
        } else {
            BHLog.i(TAG, String.format("sendHeartbeat to %s", netAddress));
            tcpClient.getTcpConnection(netAddress).setHeartbeatFlag(tempHeartbeatFlag);
        }

        BHLog.i(TAG, " sendHeartbeat end");
    }

    @Override
    public RemoteStatus getRemoteStatus() {
        RemoteStatus mRemoteStatus = new RemoteStatus();

        Server logServer = mServerStatus.getLogServer();

        mRemoteStatus.setConnected(logServer.isOnline());
        mRemoteStatus.setLogined(false);
        mRemoteStatus.setStateMachine(mStateMachine);

        return mRemoteStatus;
    }

}