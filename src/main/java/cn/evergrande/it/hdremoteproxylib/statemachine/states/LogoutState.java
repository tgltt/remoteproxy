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
import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.TCP_STATE_LOGINING;
import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.TCP_STATE_LOGOUT;

/**
 * Created by tam on 18-6-25.
 */

public class LogoutState extends BaseState {

    public final static String TAG = "LogoutState";

    public LogoutState(TCPStateMachine stateMachine) {
        mState = TCP_STATE_LOGOUT;
        mTCPStateMachine = stateMachine;
    }

    @Override
    public void enter() {
        super.enter();

        if (mTCPStateMachine.isNeedLogin()) {
            BHLog.i(TAG, "Need logining");

            mTCPStateMachine.setNeedLogin(false);
            mTCPStateMachine.switchState(TCP_STATE_LOGINING);

            return;
        }
    }

    @Override
    public boolean handleMessage(Message message) {
        BHLog.i(TAG, String.format("handleMessage: Remote(%s) msg.what=%s, curState=%s", mTCPStateMachine.getRemote(), getCmdLabel(message.what), getStateLabel(mState)));

        boolean handled = false;
        switch (message.what) {
            case EVENT_CMD_LOGIN: // 登录态下，忽略登录操作
                 mTCPStateMachine.switchState(TCP_STATE_LOGINING);
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
            case EVENT_CMD_CONNECT: // 登出态下，忽略连接和登出操作
            case EVENT_CMD_LOGIN_OUT:
        }

        return handled ? handled : super.handleMessage(message);
    }
}