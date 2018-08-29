/**
 *****************************************************************************
 * Copyright (C) 2018-2023 HD Corporation. All rights reserved
 * File        : DisconnectRouteRemoteAction.java
 *
 * Description : DisconnectRouteRemoteAction,
 *
 * Creation    : 2018-05-29
 * Author      : tanguilong@evergrande.cn
 * History     : 2018-05-29, Creation
 *****************************************************************************
 */
package cn.evergrande.it.hdremoteproxylib.network.data.actions;

import cn.evergrande.it.hdremoteproxylib.AbstractRemote;
import cn.evergrande.it.hdremoteproxylib.RouterRemote;
import cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine;
import cn.evergrande.it.hdremoteproxylib.statemachine.states.AbstractState;
import cn.evergrande.it.logger.BHLog;

public class DisconnectRouteRemoteAction extends BaseAction {

    public final static String TAG = "DisconnectRouteRemoteAction";

    public DisconnectRouteRemoteAction(int actionId, int actionSubId, Object[] args) {
        super(actionId, actionSubId, args);
    }

    @Override
    public void run() {
        if (mActionArgs == null || mActionArgs.length <= 0) {
            BHLog.i(TAG, "mActionArgs is null or empty");
            return;
        }

        if (!(mActionArgs[0] instanceof RouterRemote)) {
            BHLog.i(TAG, "mActionArgs[0] is not RouterRemote");
            return;
        }

        final RouterRemote routeRemote = (RouterRemote) mActionArgs[0];

        TCPStateMachine stateMachine = routeRemote.getStateMachine();
        AbstractState state = stateMachine.getState();

        int remoteState = state.getState();
        // TODO: 完善此处逻辑
        switch (remoteState) {
            case TCPStateMachine.TCP_STATE_DISCONNECTED:
                  dealRemoteDisconnectedState(routeRemote);
                  break;
            case TCPStateMachine.TCP_STATE_DISCONNECTING:
                  dealRemoteConnectedState(routeRemote);
                  break;
            case TCPStateMachine.TCP_STATE_CONNECTING:
            case TCPStateMachine.TCP_STATE_CONNECTED:
            default:
                  dealRemoteOtherState(routeRemote);
        }
    }

    private void dealRemoteDisconnectedState(AbstractRemote routerRemote) {
        BHLog.i(TAG, String.format("Remote(%s) already disconnect", routerRemote));
        // 仍试图转移状态机状态
        TCPStateMachine tcpStateMachine = routerRemote.getStateMachine();
        tcpStateMachine.switchState(TCPStateMachine.TCP_STATE_DISCONNECTED);
    }

    private void dealRemoteConnectedState(AbstractRemote routerRemote) {
        BHLog.i(TAG, String.format("disconnect Remote(%s)", routerRemote));
        routerRemote.disconnect();
    }

    private void dealRemoteOtherState(AbstractRemote routerRemote) {
        BHLog.i(TAG, String.format("dealRemoteConnectingState: Send disconnect signal to Remote(%s) state machine", routerRemote));

        TCPStateMachine stateMachine = routerRemote.getStateMachine();
        stateMachine.disconnect(); // 记录断开连接请求，待状态机切换到稳态后再判断是否进行断开连接操作
    }
}
