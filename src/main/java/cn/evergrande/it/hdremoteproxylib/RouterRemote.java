/**
 *****************************************************************************
 * Copyright (C) 2018-2023 HD Corporation. All rights reserved
 * File        : RouterRemote.java
 *
 * Description : RouterRemote,
 *
 * Creation    : 2018-05-10
 * Author      : tanguilong@evergrande.cn
 * History     : 2018-05-10, Creation
 *****************************************************************************
 */
package cn.evergrande.it.hdremoteproxylib;

import android.content.Context;

import java.util.Random;

import cn.evergrande.it.hdnetworklib.api.model.protocal.ErrorCode;
import cn.evergrande.it.hdnetworklib.network.tcp.TcpCallback;
import cn.evergrande.it.hdnetworklib.network.tcp.TcpClient;
import cn.evergrande.it.hdnetworklib.network.tcp.TcpConnection;
import cn.evergrande.it.hdremoteproxylib.RemoteProxy.Server;
import cn.evergrande.it.hdremoteproxylib.RemoteProxy.ServerStatus;
import cn.evergrande.it.hdremoteproxylib.evaluate.NetworkEvaluator;
import cn.evergrande.it.hdremoteproxylib.interfaces.ICheckInCallback;
import cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine;
import cn.evergrande.it.logger.BHLog;

public class RouterRemote extends BaseRemote {

    public final static String TAG = "RouterRemote";

    //IOT 路由器的公钥
    public final static String IOT_PUB_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC4ABf7zvSIRXMo+sNywo1PlN4J" +
                                             "GZlkDFs+BtBStxbWJ6s1Ts+6dig4hNdq7V4hmgnPtg4dA5xalqUS0Ae5dD+Q9PPB" +
                                             "XjQacrwKSCM5smBToPU1NkTXJJgG+Ats+vLbqdaapUoztQPGawwMYgUD9v0wfGYg" +
                                             "c2T1N+Z19sYGlSFDyQIDAQAB";

    /**
     * AES加密中需要的向量参数，需要和服务器端约定好
     */
    public static final String AES_IV = "1234567890123456";
    /**
     * 生成对称加密密钥种子, 一个application生命周期使用一个种子，根据种子生成AES key
     */
    public static volatile String AES_KEY = "";

    public static void refreshAesKey() {
        String base = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        Random random = new Random();
        StringBuffer sb = new StringBuffer();

        int baseLen = base.length();
        for (int i = 0; i < 32; i++) {
            int number = random.nextInt(baseLen);
            sb.append(base.charAt(number));
        }

        AES_KEY = sb.toString();
    }

    /**
     * 当前路由器ID
     */
    private static volatile long sCurRouterId;

    private volatile boolean mConnectingFlag;

    private RouterRemote() {
        mRemoteName = TAG;
    }

    static class SingletonHolder {
        public static RouterRemote sRouterRemote = new RouterRemote();
    }

    static RouterRemote getInstance() {
        return SingletonHolder.sRouterRemote;
    }

    public void init(Context context, ServerStatus serverStatus) {
        super.init(context, serverStatus);

        mContext = context;
        mServerStatus = serverStatus;

        setRemoteType(RemoteProxy.REMOTE_ROUTE);
    }

    /**
     * 连接路由器服务器.
     * @param args 参数数组长度必须大于等于1, 且第一个参数类型必须为CheckInCallback;
     *             第一个参数：接入服务结果回调;
     *             第二个参数：路由器IP地址；
     *             第三个参数：路由器端口
     */
    public void connect(Object[] args) {
        int curState = mStateMachine.getState().getState();
        if (curState == TCPStateMachine.TCP_STATE_CONNECTED) {
            BHLog.i(TAG, "connect: routerRemote is already connected");
            notifyChannelChanged(IEstablishChannelStateMonitor.CHANNEL_STAGE_SUCCESS);

            return;
        }

        if (!checkConnectParams(args)) {
            BHLog.i(TAG, "connect: checkConnectParams return false");
            notifyChannelChanged(IEstablishChannelStateMonitor.CHANNEL_STAGE_FAIL);

            return;
        }

        super.connect(args);

        connectToRouter((ICheckInCallback) args[0], (String) args[1]/*IP地址*/, (Integer) args[2]/*端口*/);
    }

    /**
     * 检查@see connect参数数组, 参数要求见@see connect
     * @return true, 参数数组符合要求,
     *         false, 参数数组不符合要求
     */
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

    /**
     * 获取当前路由ID
     * @return 当前路由ID
     */
    public static long getCurrentRouterId() {
        return sCurRouterId;
    }

    /**
     * 设置当前路由ID
     * @param routerId 当前路由ID
     */
    public static void setCurRouterId(long routerId) {
        sCurRouterId = routerId;
    }

    /**
     * 连接路由器
     */
    public void connectToRouter(final ICheckInCallback callback, final String routerIp, final int routerPort) {
        if (mConnectingFlag) {
            BHLog.i(TAG, "connectToRouter: Already connecting...");
            return;
        }

        mConnectingFlag = true;
        BHLog.i(TAG, "connectToRouter: starting...");

        notifyChannelChanged(IEstablishChannelStateMonitor.CHANNEL_STAGE_START);
        // 记录路由器的地址
        setRemoteAddressPort(routerIp, String.valueOf(routerPort));

        final Server routerServer = mServerStatus.getRouterServer();
        routerServer.setHostIpAddress(routerIp);
        routerServer.setPort(routerPort);
        // 创建路由器长链接
        TcpClient.getInstance().createTcpConnection(routerIp, routerPort, true, new TcpCallback() {
            @Override
            public void onSuccess(Object data) {
                BHLog.i(TAG, String.format("connectToRouter:onSuccess: connect router success! router_host_ip=%s", routerIp));

                mConnectingFlag = false;

                if (callback != null) {
                    ICheckInCallback.CheckinResult checkinResult = new ICheckInCallback.CheckinResult();
                    checkinResult.setBizServerAddress(routerIp);
                    checkinResult.setBizServerPort(String.valueOf(routerPort));

                    routerServer.setCheckInStatus(true);

                    callback.onSuccess(checkinResult);
                }

                notifyChannelChanged(IEstablishChannelStateMonitor.CHANNEL_STAGE_SUCCESS);
            }

            @Override
            public void onError(int errorCode) {
                BHLog.i(TAG, String.format("connectToRouter:onError: connect router fail!!!  router_host_ip=%s", routerIp));

                mConnectingFlag = false;

                if (callback != null) {
                    callback.onError(ErrorCode.ERROR_CODE_91001);
                }

                routerServer.setCheckInStatus(false);

                notifyChannelChanged(IEstablishChannelStateMonitor.CHANNEL_STAGE_FAIL);
            }
        });
    }

    @Override
    public void sendHeartbeat() {
        super.sendHeartbeat();

        BHLog.i(TAG, "sendHeartbeat: starting...");

        final TcpClient tcpClient = TcpClient.getInstance();

        String netAddress = String.format("%s:%s", mIpAddress, mPort);
        //发送心跳
        tcpClient.sendHeartbeatMsg(netAddress);
        //心跳回包校验
        TcpConnection tcpConnection = tcpClient.getTcpConnection(netAddress);
        if (tcpConnection == null) {
            BHLog.i(TAG, "sendHeartbeat: tcpConnection is null, stop sendHeartbeat");
            return;
        }

        int tempHeartbeatFlag = tcpConnection.getHeartbeatFlag() - 1;

        BHLog.i(TAG, String.format("sendHeartbeat: %s, heartbeatFlag = %s", netAddress, tempHeartbeatFlag));

        if (tempHeartbeatFlag <= 0) {
            BHLog.i(TAG, "sendHeartbeat: tempHeartbeatFlag <= 0, sendHeartbeat stop and state machine disconnect");
            mStateMachine.disconnect();
        } else {
            BHLog.i(TAG, String.format("sendHeartbeat: sendHeartbeat to %s", netAddress));
            tcpClient.getTcpConnection(netAddress).setHeartbeatFlag(tempHeartbeatFlag);
        }
    }

    @Override
    public void disconnect() {
        super.disconnect();

        Server server = mServerStatus.getRouterServer();
        // 重置状态
        server.setOnline(false);
        server.setLogin(false);
        server.setCheckInStatus(false);
    }

    @Override
    public RemoteStatus getRemoteStatus() {
        Server routerServer = mServerStatus.getRouterServer();

        RemoteStatus mRemoteStatus = new RemoteStatus();

        mRemoteStatus.setConnected(routerServer.isOnline());
        mRemoteStatus.setLogined(routerServer.isLogin());
        mRemoteStatus.setStateMachine(mStateMachine);

        return mRemoteStatus;
    }

}