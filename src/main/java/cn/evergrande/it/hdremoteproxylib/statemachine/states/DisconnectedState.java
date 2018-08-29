package cn.evergrande.it.hdremoteproxylib.statemachine.states;

import android.os.Message;

import cn.evergrande.it.hdremoteproxylib.evaluate.NetworkEvaluator;
import cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine;
import cn.evergrande.it.logger.BHLog;

/**
 * Created by tam on 18-6-15.
 */

public class DisconnectedState extends BaseState {

    public final static String TAG = "DisconnectedState";

    public DisconnectedState(TCPStateMachine stateMachine) {
        mState = TCPStateMachine.TCP_STATE_DISCONNECTED;
        mTCPStateMachine = stateMachine;
    }

    public boolean handleMessage(Message message) {
        switch (message.what){
            case TCPStateMachine.EVENT_CMD_CONNECT:
            case TCPStateMachine.EVENT_CMD_LOGIN:
                  dealConnectEvent();
                  break;
        }

        return true;
    }

    private void dealConnectEvent() {
        if(mTCPStateMachine.getRetryConnectTimes() > 0){
            mTCPStateMachine.switchState(TCPStateMachine.TCP_STATE_CONNECTING);
        }
    }

    @Override
    public void enter() {
        super.enter();

        mTCPStateMachine.setRetryConnectTimes(TCPStateMachine.MAX_RETRY_CONNECT_TIMES);

        if (mTCPStateMachine.isNeedConnect()) {
            BHLog.i(TAG, String.format("enter: Remote(%s) need connecting", mTCPStateMachine.getRemote()));

            mTCPStateMachine.setNeedConnect(false);
            mTCPStateMachine.switchState(TCPStateMachine.TCP_STATE_CONNECTING);

            return;
        }

        if (mTCPStateMachine.isNeedLogin() && mTCPStateMachine.getRetryLoginTimes() > 0) {
            BHLog.i(TAG, String.format("enter: Remote(%s) Need logining", mTCPStateMachine.getRemote()));

            mTCPStateMachine.setNeedConnect(true);
            mTCPStateMachine.switchState(TCPStateMachine.TCP_STATE_CONNECTING);
        }
    }
}