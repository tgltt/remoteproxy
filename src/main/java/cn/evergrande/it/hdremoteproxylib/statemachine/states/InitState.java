package cn.evergrande.it.hdremoteproxylib.statemachine.states;

import android.os.Message;

import cn.evergrande.it.hdremoteproxylib.evaluate.NetworkEvaluator;
import cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine;
import cn.evergrande.it.hdtoolkits.threadmanage.ThreadManager;
import cn.evergrande.it.logger.BHLog;

import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.EVENT_CMD_CONNECT;
import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.EVENT_CMD_DISCONNECT;
import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.EVENT_CMD_LOGIN;
import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.EVENT_CMD_LOGIN_OUT;
import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.TCP_STATE_CONNECTING;
import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.TCP_STATE_INIT;

/**
 * Created by tam on 18-6-14.
 */

public class InitState extends BaseState {

    public final static String TAG = "InitState";

    public InitState(TCPStateMachine stateMachine) {
        mState = TCP_STATE_INIT;
        mTCPStateMachine = stateMachine;
    }

    @Override
    public void enter() {
        super.enter();

        ThreadManager.getInstance().postDelayedLogicTask(new Runnable() {
            @Override
            public void run() {
                connect();
            }
        }, 5000);
    }

    private void connect(){
        if (mTCPStateMachine.isEnabled() && mTCPStateMachine.getRetryConnectTimes() > 0) {
            mTCPStateMachine.switchState(TCP_STATE_CONNECTING);
        }
    }

    @Override
    public boolean handleMessage(Message message) {
        BHLog.i(TAG, String.format("handleMessage: Remote(%s) msg.what=%s, curState=%s, state machine enable status: %s", mTCPStateMachine.getRemote(), getCmdLabel(message.what), getStateLabel(mState), mTCPStateMachine.isEnabled()));

        if(!mTCPStateMachine.isEnabled()){
            return true;
        }

        boolean handled = false;
        switch (message.what) {
            case EVENT_CMD_CONNECT:
            case EVENT_CMD_LOGIN:
                 connect();
                 handled = true;
                 break;
            case EVENT_CMD_DISCONNECT: // 忽略操作
            case EVENT_CMD_LOGIN_OUT:
                 handled = true;
                 break;
        }

        return handled ? handled : super.handleMessage(message);
    }
}
