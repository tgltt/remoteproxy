/**
 *****************************************************************************
 * Copyright (C) 2018-2023 HD Corporation. All rights reserved
 * File        : ConnectCloudRemoteAction.java
 *
 * Description : ConnectCloudRemoteAction,
 *
 * Creation    : 2018-05-29
 * Author      : tanguilong@evergrande.cn
 * History     : 2018-05-29, Creation
 *****************************************************************************
 */
package cn.evergrande.it.hdremoteproxylib.network.data.actions;

import cn.evergrande.it.hdnetworklib.network.tcp.TcpClient;
import cn.evergrande.it.hdremoteproxylib.AbstractRemote;
import cn.evergrande.it.hdremoteproxylib.CloudRemote;
import cn.evergrande.it.hdremoteproxylib.HdNetworkMonitorService;
import cn.evergrande.it.hdremoteproxylib.RemoteProxy;
import cn.evergrande.it.hdremoteproxylib.evaluate.NetworkEvaluator;
import cn.evergrande.it.hdremoteproxylib.interfaces.ICheckInCallback;
import cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine;
import cn.evergrande.it.logger.BHLog;

public class ConnectCloudRemoteAction extends RetryConnectRemoteAction {

    public final static String TAG = "ConnectCloudRemoteAction";

    public ConnectCloudRemoteAction(int actionId, int actionSubId, Object[] args) {
        super(actionId, actionSubId, args);
    }

    @Override
    public void run() {
        if (!checkAndParseParams(mActionArgs)) {
            BHLog.i(TAG, "run: checkAndParseParams failed");

            notifyCheckinStage(RemoteProxy.REMOTE_CLOUD, mActionArgs, AbstractRemote.IEstablishChannelStateMonitor.CHANNEL_STAGE_FAIL);
            return;
        }

        final CloudRemote cloudRemote = (CloudRemote) mActionArgs[0];

        ICheckInCallback callback = new ICheckInCallback() {
            @Override
            public void onSuccess(ICheckInCallback.CheckinResult checkinResult) {
                if (checkinResult == null) {
                    BHLog.i(TAG, "run: onSuccess: can't get cloudRemote address");

                    retryConnectRemote(cloudRemote);
                    return;
                }

                String ipAddress = checkinResult.getBizServerAddress();
                int port = -1;
                try {
                    port = Integer.parseInt(checkinResult.getBizServerPort());
                } catch (Throwable throwable) {
                    BHLog.i(TAG, throwable.getMessage(), throwable);
                }

                TcpClient.getInstance().updateConnectionParams(TcpClient.MAP_KEY_CLOUD_CONNECTION, ipAddress, port, CloudRemote.IOT_PUB_KEY, CloudRemote.AES_KEY, CloudRemote.AES_IV);

                BHLog.i(TAG, String.format("run: cloudRemote(%s:%s) connected successfully", ipAddress, port));

                cloudRemote.setRemoteAddressPort(ipAddress, "" + port);

                BHLog.i(TAG, "run:onSuccess: invokeTcpConnectionHeartbeat");
                HdNetworkMonitorService.getInstance().invokeTcpConnectionHeartbeat();

                TCPStateMachine stateMachine = cloudRemote.getStateMachine();
                stateMachine.sendEvent(TCPStateMachine.EVENT_CMD_CONNECT_SUCCESS, null);
                stateMachine.login();
            }

            @Override
            public void onError(int errorCode) {
                BHLog.i(TAG, String.format("run: onError: errorCode=%s", errorCode));
                retryConnectRemote(cloudRemote);
            }
        };

        BHLog.i(TAG, "run: cloudRemote connecting..");

        Object[] connectParams = new Object[] {callback, mActionArgs[3]/*接入模式*/, mActionArgs[4]/*Sec服务器地址*/, mActionArgs[5]/*Sec服务器端口*/, mActionArgs[6]/*LB服务器地址*/, mActionArgs[7]/*LB服务器端口*/};
        cloudRemote.connect(connectParams);
    }

    protected boolean checkAndParseParams(Object[] paramArgs) {
        if (!mValid) {
            BHLog.i(TAG, "checkAndParseParams: Action not valid");
            return false;
        }

        final int expectedArgsCount = 8;
        if (mActionArgs.length < expectedArgsCount) {
            BHLog.i(TAG, String.format("checkAndParseParams: Params not enough(expectd=%d, actual=%d)", expectedArgsCount, mActionArgs.length));
            return false;
        }

        if (!(mActionArgs[0] instanceof CloudRemote)) { // CloudRemote
            BHLog.i(TAG, String.format("checkAndParseParams: mActionArgs[0](%s) is not CloudRemote", mActionArgs[0]));
            return false;
        }

        if (!(mActionArgs[3] instanceof Integer)) { // Mode : Block or non Block
            BHLog.i(TAG, String.format("checkAndParseParams: mActionArgs[3](%s) is not Integer", mActionArgs[3]));
            return false;
        }

        if (!(mActionArgs[4] instanceof String)) { // Sec server url
            BHLog.i(TAG, String.format("checkAndParseParams: mActionArgs[4](%s) is not String", mActionArgs[4]));
            return false;
        }

        if (!(mActionArgs[5] instanceof Integer)) { // Sec server port
            BHLog.i(TAG, String.format("checkAndParseParams: mActionArgs[5](%s) is not Integer", mActionArgs[5]));
            return false;
        }

        if (!(mActionArgs[6] instanceof String)) { // LB server url
            BHLog.i(TAG, String.format("checkAndParseParams: mActionArgs[6](%s) is not String", mActionArgs[6]));
            return false;
        }

        if (!(mActionArgs[7] instanceof Integer)) { // LB server port
            BHLog.i(TAG, String.format("checkAndParseParams: mActionArgs[7](%s) is not Integer", mActionArgs[7]));
            return false;
        }

        return true;
    }
}
