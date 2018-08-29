package cn.evergrande.it.hdremoteproxylib.statemachine.states;

import android.os.Message;

import cn.evergrande.it.hdremoteproxylib.AbstractRemote;
import cn.evergrande.it.hdremoteproxylib.RemoteConnector;
import cn.evergrande.it.hdremoteproxylib.RemoteProxy;
import cn.evergrande.it.hdremoteproxylib.evaluate.NetworkEvaluator;
import cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine;
import cn.evergrande.it.logger.BHLog;

import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.EVENT_CMD_DISCONNECT;
import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.EVENT_CMD_DISCONNECT_FAILED;
import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.EVENT_CMD_DISCONNECT_SUCCESS;
import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.EVENT_CMD_LOGIN;
import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.EVENT_CMD_LOGIN_OUT;
import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.TCP_STATE_INVALID;

/**
 * Created by tam on 18-6-14.
 */

public class DisconnectingState extends BaseState {

    public static final String TAG = "DisconnectingState";

    public DisconnectingState(TCPStateMachine stateMachine) {
        mState = TCPStateMachine.TCP_STATE_DISCONNECTING;
        mTCPStateMachine = stateMachine;
    }

    @Override
    public void enter() {
        super.enter();
        // 启动断开连接的操作
        AbstractRemote remote = mTCPStateMachine.getRemote();
        if (remote == null) {
            throw new IllegalArgumentException("remote == null");
        }

        int remoteType = remote.getRemoteType();
        RemoteConnector.getInstance().disconnect(remoteType);
    }

    @Override
    public boolean handleMessage(Message msg){
        BHLog.i(TAG, String.format("handleMessage: Remote(%s) msg.what=%s, curState=%s", mTCPStateMachine.getRemote(), getCmdLabel(msg.what), getStateLabel(mState)));

        boolean handled = false;
        switch (msg.what) {
            case EVENT_CMD_DISCONNECT_SUCCESS:
                 mTCPStateMachine.switchState(TCPStateMachine.TCP_STATE_DISCONNECTED);
                 handled = true;
                 break;
            case EVENT_CMD_DISCONNECT_FAILED:
                 AbstractState oldState = mTCPStateMachine.getOldState();
                 if (oldState != null) {
                     mTCPStateMachine.switchState(oldState.getState());
                 } else { // 没有旧状态, 该情况在Disconnecting状态时, 不应该发生,故在此状况发生时,将状态机状态置为Invalid
                     mTCPStateMachine.switchState(TCP_STATE_INVALID);
                 }

                 handled = true;
                 break;
            case EVENT_CMD_DISCONNECT:
                 mTCPStateMachine.setNeedConnect(false);
                 handled = true;
                 break;
            case EVENT_CMD_LOGIN_OUT:
                 mTCPStateMachine.setNeedLogin(false);
                 handled = true;
                 break;

        }
        // 尚未处理本次消息, 交由父类去处理该消息
        return handled ? handled : super.handleMessage(msg);
    }

}
