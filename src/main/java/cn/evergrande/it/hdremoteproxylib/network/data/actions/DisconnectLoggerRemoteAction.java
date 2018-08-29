/**
 *****************************************************************************
 * Copyright (C) 2018-2023 HD Corporation. All rights reserved
 * File        : DisconnectLoggerRemoteAction.java
 *
 * Description : DisconnectLoggerRemoteAction,
 *
 * Creation    : 2018-06-11
 * Author      : tanguilong@evergrande.cn
 * History     : 2018-06-11, Creation
 *****************************************************************************
 */
package cn.evergrande.it.hdremoteproxylib.network.data.actions;

import cn.evergrande.it.hdremoteproxylib.AbstractRemote;
import cn.evergrande.it.hdremoteproxylib.LoggerRemote;
import cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine;
import cn.evergrande.it.logger.BHLog;

public class DisconnectLoggerRemoteAction extends BaseAction {

    public final static String TAG = "DisconnectLoggerRemoteAction";

    public DisconnectLoggerRemoteAction(int actionId, int actionSubId, Object[] args) {
        super(actionId, actionSubId, args);
    }

    @Override
    public void run() {
        if (mActionArgs == null || mActionArgs.length <= 0) {
            BHLog.i(TAG, "mActionArgs is null or empty");
            return;
        }

        if (!(mActionArgs[0] instanceof LoggerRemote)) {
            BHLog.i(TAG, "mActionArgs[0] is not LoggerRemote");
            return;
        }

        boolean force = false;
        if (mActionArgs.length >= 2) {
            try {
                force = Boolean.parseBoolean((String) mActionArgs[2]);
            } catch (Throwable throwable) {
                BHLog.i(TAG, throwable.getMessage(), throwable);
            }
        }

        final LoggerRemote loggerRemote = (LoggerRemote) mActionArgs[0];
        TCPStateMachine stateMachine = loggerRemote.getStateMachine();
        int remoteState = stateMachine.getState().getState();
        // TODO: 完善此处逻辑
        switch (remoteState) {
            case TCPStateMachine.TCP_STATE_DISCONNECTED:
                  dealRemoteDisconnectedState(loggerRemote);
                  break;
            case TCPStateMachine.TCP_STATE_DISCONNECTING:
                  dealRemoteConnectedState(loggerRemote);
                  break;
            case TCPStateMachine.TCP_STATE_CONNECTING:
            case TCPStateMachine.TCP_STATE_CONNECTED:
            default:
                  dealRemoteOtherState(loggerRemote, force);
        }
    }

    private void dealRemoteDisconnectedState(AbstractRemote loggerRemote) {
        BHLog.i(TAG, String.format("Remote(%s) already disconnect", loggerRemote));
    }

    private void dealRemoteConnectedState(AbstractRemote loggerRemote) {
        BHLog.i(TAG, String.format("disconnect Remote(%s)", loggerRemote));
        loggerRemote.disconnect();
    }

    private void dealRemoteOtherState(final AbstractRemote loggerRemote, boolean force) {
        BHLog.i(TAG, String.format("dealRemoteConnectingState: register IEstablishChannelStateMonitor on Remote(%s)", loggerRemote));

        TCPStateMachine stateMachine = loggerRemote.getStateMachine();
        stateMachine.disconnect(); // 记录断开连接请求，待状态机切换到稳态后再判断是否进行断开连接操作
    }
}
