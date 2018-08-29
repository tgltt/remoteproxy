/**
 *****************************************************************************
 * Copyright (C) 2018-2023 HD Corporation. All rights reserved
 * File        : DisconnectCloudRemoteAction.java
 *
 * Description : DisconnectCloudRemoteAction,
 *
 * Creation    : 2018-05-29
 * Author      : tanguilong@evergrande.cn
 * History     : 2018-05-29, Creation
 *****************************************************************************
 */
package cn.evergrande.it.hdremoteproxylib.network.data.actions;

import cn.evergrande.it.hdremoteproxylib.AbstractRemote;
import cn.evergrande.it.hdremoteproxylib.CloudRemote;
import cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine;
import cn.evergrande.it.hdremoteproxylib.statemachine.states.AbstractState;
import cn.evergrande.it.logger.BHLog;

public class DisconnectCloudRemoteAction extends BaseAction {

    public final static String TAG = "DisconnectCloudRemoteAction";

    public DisconnectCloudRemoteAction(int actionId, int actionSubId, Object[] args) {
        super(actionId, actionSubId, args);
    }

    @Override
    public void run() {
        if (mActionArgs == null || mActionArgs.length <= 0) {
            BHLog.i(TAG, "run: mActionArgs is null or empty");
            return;
        }

        if (!(mActionArgs[0] instanceof CloudRemote)) {
            BHLog.i(TAG, "run: mActionArgs[0] is not CloudRemote");
            return;
        }

        final CloudRemote cloudRemote = (CloudRemote) mActionArgs[0];
        TCPStateMachine stateMachine = cloudRemote.getStateMachine();

        AbstractState state = stateMachine.getState();
        int remoteState = state.getState();
        // TODO: 完善此处逻辑
        switch (remoteState) {
            case TCPStateMachine.TCP_STATE_DISCONNECTED:
                  dealRemoteDisconnectedState(cloudRemote);
                  break;
            case TCPStateMachine.TCP_STATE_DISCONNECTING:
                  dealRemoteConnectedState(cloudRemote);
                  break;
            case TCPStateMachine.TCP_STATE_CONNECTING:
            case TCPStateMachine.TCP_STATE_CONNECTED:
            default:
                  dealRemoteOtherState(cloudRemote);
        }
    }

    private void dealRemoteDisconnectedState(AbstractRemote cloudRemote) {
        BHLog.i(TAG, String.format("dealRemoteDisconnectedState: Remote(%s) already disconnect", cloudRemote));
        // 仍试图转移状态机状态
        TCPStateMachine tcpStateMachine = cloudRemote.getStateMachine();
        tcpStateMachine.switchState(TCPStateMachine.TCP_STATE_DISCONNECTED);
    }

    private void dealRemoteConnectedState(AbstractRemote cloudRemote) {
        BHLog.i(TAG, String.format("dealRemoteConnectedState: disconnect Remote(%s)", cloudRemote));

        cloudRemote.disconnect();
    }

    private void dealRemoteOtherState(AbstractRemote cloudRemote) {
        BHLog.i(TAG, String.format("dealRemoteConnectingState: Send disconnect signal to Remote(%s) state machine", cloudRemote));

        TCPStateMachine stateMachine = cloudRemote.getStateMachine();
        stateMachine.disconnect(); // 记录断开连接请求，待状态机切换到稳态后再判断是否进行断开连接操作
    }
}
