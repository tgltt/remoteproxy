package cn.evergrande.it.hdremoteproxylib.statemachine.states;

import android.os.Message;

import cn.evergrande.it.hdremoteproxylib.evaluate.NetworkEvaluator;
import cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine;
import cn.evergrande.it.logger.BHLog;

import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.EVENT_CMD_CONNECT;
import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.EVENT_CMD_DISCONNECT;
import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.EVENT_CMD_DISCONNECT_SUCCESS;
import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.EVENT_CMD_LOGIN;
import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.EVENT_CMD_LOGIN_OUT;
import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.TCP_STATE_DISCONNECTING;
import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.TCP_STATE_LOGINED;
import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.TCP_STATE_LOGOUT;
import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.TCP_STATE_LOGOUTING;

/**
 * Created by tam on 18-6-14.
 */

public class LoginedState extends BaseState {

    public final static String TAG = "LoginedState";

    public LoginedState(TCPStateMachine stateMachine) {
        mState = TCP_STATE_LOGINED;
        mTCPStateMachine = stateMachine;
    }

    @Override
    public void enter() {
        super.enter();

        if (mTCPStateMachine.isNeedDisconnect()) {
            BHLog.i(TAG, "enter: Need disconnecting");

            mTCPStateMachine.setNeedDisconnect(false);
            mTCPStateMachine.switchState(TCP_STATE_DISCONNECTING);

            return;
        }

        if (mTCPStateMachine.isNeedLoginOut()) {
            BHLog.i(TAG, "Need login out ing");

            mTCPStateMachine.setNeedLoginOut(false);
            mTCPStateMachine.switchState(TCP_STATE_LOGOUTING);
            
            return;
        }
    }

    @Override
    public boolean handleMessage(Message message) {
        BHLog.i(TAG, String.format("handleMessage: Remote(%s) msg.what=%s, curState=%s", mTCPStateMachine.getRemote(), getCmdLabel(message.what), getStateLabel(mState)));

        boolean handled = false;
        switch (message.what) {
            case EVENT_CMD_CONNECT: // 登录态下，忽略连接操作
            case EVENT_CMD_LOGIN: // 登录态下，忽略登录操作
                 handled = true;
                 break;
            case EVENT_CMD_DISCONNECT:
                 mTCPStateMachine.switchState(TCP_STATE_DISCONNECTING);
                 handled = true;
                 break;
            case EVENT_CMD_DISCONNECT_SUCCESS:
                 mTCPStateMachine.switchState(TCPStateMachine.TCP_STATE_DISCONNECTED);
                 handled = true;
                 break;
            case EVENT_CMD_LOGIN_OUT:
                 mTCPStateMachine.switchState(TCP_STATE_LOGOUTING);
                 handled = true;
                 break;
        }

        return handled ? handled : super.handleMessage(message);
    }
}
