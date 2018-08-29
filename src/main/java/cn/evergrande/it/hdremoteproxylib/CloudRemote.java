/**
 *****************************************************************************
 * Copyright (C) 2018-2023 HD Corporation. All rights reserved
 * File        : CloudRemote.java
 *
 * Description : CloudRemote,
 *
 * Creation    : 2018-05-10
 * Author      : tanguilong@evergrande.cn
 * History     : 2018-05-10, Creation
 *****************************************************************************
 */
package cn.evergrande.it.hdremoteproxylib;

import android.content.Context;

import cn.evergrande.it.hdnetworklib.network.common.biz.ProtocolUtil;
import cn.evergrande.it.hdnetworklib.api.model.protocal.ErrorCode;
import cn.evergrande.it.hdnetworklib.network.tcp.TcpClient;
import cn.evergrande.it.hdnetworklib.network.tcp.TcpConnection;
import cn.evergrande.it.hdremoteproxylib.RemoteProxy.Server;
import cn.evergrande.it.hdremoteproxylib.RemoteProxy.ServerStatus;
import cn.evergrande.it.hdremoteproxylib.evaluate.NetworkEvaluator;
import cn.evergrande.it.hdremoteproxylib.interfaces.ICheckInCallback;
import cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine;
import cn.evergrande.it.logger.BHLog;

import java.util.Random;
import java.util.concurrent.Semaphore;

public class CloudRemote extends BaseRemote {

    public final static String TAG = "CloudRemote";
    /**
     * AES加密中需要的向量参数，需要和服务器端约定好
     */
    public static final String AES_IV = "1234567890123456";
    /**
     * 生成对称加密密钥种子, 一个application生命周期使用一个种子，根据种子生成AES key
     */
    public final static String AES_KEY;
    static {
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

    //IOT云端服务器的公钥
    public final static String IOT_PUB_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDU74fcId1n/+v+xENa6l9Z0zW2" +
                                             "qEE+ZVxb0dRNAeDn/h6wIy8BTxFxaDcL+mSm0xtezI52WxRARhXyqAhdKeFOB2Wx" +
                                             "09Szx2UGEpNYxIJyh3vmIF5LEHkLe7Jiyt1rEBzb6lUJfjBWBvsUDDynvhCORVEG" +
                                             "b7nKevKKjiRlsDwLyQIDAQAB";

    //用于接入线程控制的信号量
    private Semaphore checkInSemaphore;

    private CloudRemote() {
        mRemoteName = TAG;
    }

    static class SingletonHolder {
        public static CloudRemote sCloudRemote = new CloudRemote();
    }

    static CloudRemote getInstance() {
        return SingletonHolder.sCloudRemote;
    }

    public void init(Context context, ServerStatus serverStatus) {
        super.init(context, serverStatus);

        mContext = context;
        mServerStatus = serverStatus;
        checkInSemaphore = new Semaphore(1);

        setRemoteType(RemoteProxy.REMOTE_CLOUD);
    }

    /**
     * 连接云端服务器.
     * @param args 参数数组长度必须大于等于6, 且第一个参数类型必须为CheckInCallback, 第二个参数类型必须为Integer;
     *              第一个参数：接入服务结果回调;
     *              第二个参数：连接模式, 取值 MODE_NON_BLOCK为非阻塞，MODE_BLOCK为阻塞.
     */
    public void connect(Object[] args) {
        int curState = mStateMachine.getState().getState();
        if (curState == TCPStateMachine.TCP_STATE_CONNECTED) {
            BHLog.i(TAG, "connect: cloudRemote is already connected");

            notifyChannelChanged(IEstablishChannelStateMonitor.CHANNEL_STAGE_SUCCESS);
            return;
        }

        if (!checkConnectParams(args)) {
            BHLog.i(TAG, "checkConnectParams return false");

            notifyChannelChanged(IEstablishChannelStateMonitor.CHANNEL_STAGE_FAIL);
            return;
        }

        super.connect(args);

        ICheckInCallback callback = (ICheckInCallback) args[0];
        int mode = (Integer) args[1];
        // access Biz server (Cloud server)
        accessSrv(callback, mode == MODE_NON_BLOCK ? true : false, (String) args[2]/*Sec服务器地址*/, (Integer) args[3]/*Sec服务器端口*/, (String) args[4]/*LB服务器地址*/, (Integer) args[5]/*LB服务器端口*/);
    }

    /**
     * 检查@see connect参数数组, 参数要求见@see connect
     * @return true, 参数数组符合要求,
     *         false, 参数数组不符合要求
     */
    protected boolean checkConnectParams(Object[] args) {
        if (args == null || args.length < 6) {
            return false;
        }

        if (args[0] instanceof ICheckInCallback
                && args[1] instanceof Integer    // 接入模式
                && args[2] instanceof String     // 安全服务器地址
                && args[3] instanceof Integer    // 安全服务器端口
                && args[4] instanceof String     // LB服务器地址
                && args[5] instanceof Integer) { // LB服务器端口
            return true;
        }

        return false;
    }

    /**
     * App 接入服务，应用启动时候调用，各个云端业务请求的前提
     * 1.使用公钥加密请求串，建立安全通道(短连接)
     * 2.获取负载较低的服务器（短连接）
     * 3.连接云端业务服务器
     *
     * @param callback
     * @param hasTry 是否为尝试获取锁（true为非阻塞，false则为阻塞）
     * @param secServerUrl Sec服务器地址
     * @param secServerPort Sec服务器端口
     * @param lbServerUrl LB服务器地址
     * @param lbServerPort LB服务器端口
     */
    private void accessSrv(final ICheckInCallback callback, boolean hasTry, final String secServerUrl, final int secServerPort, final String lbServerUrl, final int lbServerPort) {
        BHLog.i(TAG, String.format("accessSrv: sec_server_host_name: %s, lb_server_host_name: %s", secServerUrl,  lbServerUrl));

        if (hasTry) {
            boolean result = checkInSemaphore.tryAcquire();

            if (!result) {
                if (callback != null) {
                    callback.onError(ErrorCode.ERROR_CODE_91002);
                }
                return;
            }
        } else {
            try {
                checkInSemaphore.acquire();
            } catch (InterruptedException e) {
                BHLog.e(TAG, e.getMessage(), e);
            }
        }

        Server cloudServer = mServerStatus.getCloudServer();
        if (!cloudServer.isCheckInStatus()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    CheckInBiz checkInBiz = new CheckInBiz(mContext, callback, CloudRemote.this, mServerStatus);
                    checkInBiz.confirmProcess();
                }
            }).start();
        } else {
            BHLog.i(TAG, "accessSrv: CheckInBiz has already succeed");

            checkInSemaphore.release();

            if (callback != null) {
                ICheckInCallback.CheckinResult checkinResult = new ICheckInCallback.CheckinResult();
                checkinResult.setBizServerAddress(mIpAddress);
                checkinResult.setBizServerPort(mPort);

                callback.onSuccess(checkinResult);
            }
        }
    }

    /**
     * 获取接入线程控制的信号量
     */
    public Semaphore getCheckInSemaphore() {
        return checkInSemaphore;
    }

    @Override
    public void sendHeartbeat() {
        super.sendHeartbeat();

        BHLog.i(TAG, " sendHeartbeat: sendHeartbeat ...");

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

        Server server = mServerStatus.getCloudServer();

        server.setOnline(false);
        server.setLogin(false);
        server.setCheckInStatus(false);
    }

    @Override
    public RemoteStatus getRemoteStatus() {
        Server cloudServer = mServerStatus.getCloudServer();

        RemoteStatus mRemoteStatus = new RemoteStatus();

        mRemoteStatus.setConnected(cloudServer.isOnline());
        mRemoteStatus.setLogined(cloudServer.isLogin());
        mRemoteStatus.setStateMachine(mStateMachine);

        return mRemoteStatus;
    }

}