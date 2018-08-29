/**
 *****************************************************************************
 * Copyright (C) 2018-2023 HD Corporation. All rights reserved
 * File        : ConnectLoggerRemoteAction.java
 *
 * Description : ConnectLoggerRemoteAction,
 *
 * Creation    : 2018-06-11
 * Author      : tanguilong@evergrande.cn
 * History     : 2018-06-11, Creation
 *****************************************************************************
 */
package cn.evergrande.it.hdremoteproxylib.network.data.actions;

import cn.evergrande.it.hdnetworklib.api.model.protocal.ErrorCode;
import cn.evergrande.it.hdnetworklib.network.tcp.TcpCallback;
import cn.evergrande.it.hdnetworklib.network.tcp.TcpClient;
import cn.evergrande.it.hdremoteproxylib.HdNetworkMonitorService;
import cn.evergrande.it.hdremoteproxylib.LoggerRemote;
import cn.evergrande.it.hdremoteproxylib.interfaces.ICheckInCallback;
import cn.evergrande.it.logger.BHLog;

public class ConnectLoggerRemoteAction extends RetryConnectRemoteAction {

    public final static String TAG = "ConnectLoggerRemoteAction";

    public ConnectLoggerRemoteAction(int actionId, int actionSubId, Object[] args) {
        super(actionId, actionSubId, args);
    }

    @Override
    public void run() {
        if (!checkAndParseParams(mActionArgs)) {
            BHLog.i(TAG, "run: checkAndParseParams failed");
            return;
        }

        final LoggerRemote loggerRemote = (LoggerRemote) mActionArgs[0];
        final String loggerIp = (String) mActionArgs[3];
        final Integer loggerPort = (Integer) mActionArgs[4];
        final TcpCallback callerCallback;

        if (mActionArgs.length > 5 && mActionArgs[5] != null && mActionArgs[5] instanceof TcpCallback) {
            callerCallback = (TcpCallback) mActionArgs[5];
        } else {
            callerCallback = null;
        }

        BHLog.i(TAG, String.format("run: loggerRemote(%s:%s)", loggerIp, loggerPort));
        // 设置Remote的地址及端口
        loggerRemote.setRemoteAddressPort(loggerIp, loggerPort.toString());
        TcpClient.getInstance().updateConnectionParams(TcpClient.MAP_KEY_LOGGER_CONNECTION, loggerIp, loggerPort, "", "", "");

        ICheckInCallback callback = new ICheckInCallback() {
            @Override
            public void onSuccess(CheckinResult checkinResult) {
                if (checkinResult == null) {
                    BHLog.i(TAG, "run: onSuccess: can't get loggerRemote address");

                    if (callerCallback != null) {
                        callerCallback.onError(ErrorCode.ERROR_CODE_90001);
                    }

                    retryConnectRemote(loggerRemote);

                    return;
                }
                // 通知调用方回调连接建立成功消息
                if (callerCallback != null) {
                    callerCallback.onSuccess(checkinResult);
                }

                BHLog.i(TAG, "run:onSuccess: invokeTcpConnectionHeartbeat");
                // 启动心跳
                HdNetworkMonitorService.getInstance().invokeTcpConnectionHeartbeat();
            }

            @Override
            public void onError(int errorCode) {
                BHLog.i(TAG, String.format("run: onError: errorCode=%s", errorCode));

                if (callerCallback != null) {
                    callerCallback.onError(errorCode);
                }

                retryConnectRemote(loggerRemote);
            }
        };

        BHLog.i(TAG, "run: routeRemote connecting..");

        Object[] connectParams = new Object[] {callback, loggerIp, loggerPort};
        loggerRemote.connect(connectParams);
    }

    protected boolean checkAndParseParams(Object[] paramArgs) {
        if (!mValid) {
            BHLog.i(TAG, "checkAndParseParams: Action not valid");
            return false;
        }

        final int expectedCount = 5;
        if (mActionArgs.length < expectedCount) {
            BHLog.i(TAG, String.format("checkAndParseParams: Params not enough(expectd=%d, actual=%d)", expectedCount, mActionArgs.length));
            return false;
        }

        if (!(mActionArgs[0] instanceof LoggerRemote)) {
            BHLog.i(TAG, String.format("checkAndParseParams: mActionArgs[0](%s) is not RouterRemote", mActionArgs[0]));
            return false;
        }

        if (!(mActionArgs[3] instanceof String)) {
            BHLog.i(TAG, String.format("checkAndParseParams: mActionArgs[3](%s) is not String", mActionArgs[3]));
            return false;
        }

        if (!(mActionArgs[4] instanceof Integer)) {
            BHLog.i(TAG, String.format("checkAndParseParams: mActionArgs[4](%s) is not Integer", mActionArgs[4]));
            return false;
        }

        return true;
    }

}
