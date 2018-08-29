package cn.evergrande.it.hdremoteproxylib.statemachine.states;

import android.os.Message;
import android.util.Log;

import cn.evergrande.it.hdremoteproxylib.evaluate.NetworkEvaluator;
import cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine;
import cn.evergrande.it.logger.BHLog;

import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.EVENT_CMD_CONNECT;
import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.EVENT_CMD_DISCONNECT;
import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.EVENT_CMD_DISCONNECT_SUCCESS;
import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.EVENT_CMD_LOGIN;
import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.EVENT_CMD_LOGIN_OUT;
import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.MAX_RETRY_CONNECT_TIMES;
import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.TCP_STATE_CONNECTED;
import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.TCP_STATE_DISCONNECTING;
import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.TCP_STATE_LOGINING;

/**
 * Created by tam on 18-6-14.
 */
public class ConnectedState extends BaseState {

    public final static String TAG = "ConnectedState";

    public ConnectedState(TCPStateMachine stateMachine) {
        mState = TCP_STATE_CONNECTED;
        mTCPStateMachine = stateMachine;
    }

    @Override
    public void enter() {
        super.enter();

        mTCPStateMachine.setRetryConnectTimes(MAX_RETRY_CONNECT_TIMES);

        if (mTCPStateMachine.isNeedDisconnect()) {
            BHLog.i(TAG, String.format("enter: Remote(%s) Need disconnecting", mTCPStateMachine.getRemote()));

            mTCPStateMachine.setNeedDisconnect(false);
            mTCPStateMachine.switchState(TCP_STATE_DISCONNECTING);

            return;
        }

        if (mTCPStateMachine.isNeedLogin() && mTCPStateMachine.getRetryLoginTimes() > 0) {
            BHLog.i(TAG, String.format("enter: Remote(%s) Need logining", mTCPStateMachine.getRemote()));

            mTCPStateMachine.setNeedLogin(false);
            mTCPStateMachine.switchState(TCP_STATE_LOGINING);
        }
    }

    @Override
    public boolean handleMessage(Message message) {
        BHLog.i(TAG, String.format("handleMessage: Remote(%s) msg.what=%s, curState=%s", mTCPStateMachine.getRemote(), getCmdLabel(message.what), getStateLabel(mState)));

        boolean handled = false;
        switch (message.what) {
            case EVENT_CMD_DISCONNECT:
                 mTCPStateMachine.switchState(TCP_STATE_DISCONNECTING);
                 handled = true;
                 break;
            case EVENT_CMD_CONNECT: // 已经连接，过滤掉该操作
            case EVENT_CMD_LOGIN_OUT: // 尚未登录，过滤掉该操作
                 handled = true;
                 break;
            case EVENT_CMD_LOGIN:
                 mTCPStateMachine.switchState(TCP_STATE_LOGINING);
                 handled = true;
                 break;
        }

        return handled ? handled : super.handleMessage(message);
    }
}