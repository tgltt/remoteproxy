package cn.evergrande.it.hdremoteproxylib.statemachine.states;

import android.os.Message;

import cn.evergrande.it.hdremoteproxylib.evaluate.NetworkEvaluator;
import cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine;
import cn.evergrande.it.logger.BHLog;

import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.EVENT_CMD_CONNECT;
import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.EVENT_CMD_LOGIN;
import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.EVENT_CMD_LOGIN_FAILED;
import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.EVENT_CMD_LOGIN_SUCCESS;

/**
 * Created by tam on 18-6-14.
 */

public class LoginingState extends BaseState {

    public final static String TAG = "LoginingState";

    public LoginingState(TCPStateMachine stateMachine) {
        mState = TCPStateMachine.TCP_STATE_LOGINING;
        mTCPStateMachine = stateMachine;
    }

    @Override
    public void enter() {
        super.enter();
        // 启动登录操作
        mTCPStateMachine.notifyLoginOrOutControllers(true);
    }

    @Override
    public boolean handleMessage(Message msg){
        BHLog.i(TAG, String.format("handleMessage: Remote(%s) msg.what=%s, curState=%s", mTCPStateMachine.getRemote(), getCmdLabel(msg.what), getStateLabel(mState)));

        boolean handled = false;
        switch (msg.what) {
            case EVENT_CMD_LOGIN_SUCCESS:
                 mTCPStateMachine.switchState(TCPStateMachine.TCP_STATE_LOGINED);
                 handled = true;
                 break;
            case EVENT_CMD_LOGIN_FAILED:
                 mTCPStateMachine.switchState(TCPStateMachine.TCP_STATE_CONNECTED);
                 handled = true;
                 break;
            case EVENT_CMD_LOGIN:
                 mTCPStateMachine.setNeedLoginOut(false);
                 handled = true;
                 break;
            case EVENT_CMD_CONNECT:
                 mTCPStateMachine.setNeedDisconnect(false);
                 handled = true;
                 break;
        }
        // 尚未处理本次消息, 交由父类去处理该消息
        return handled ? handled : super.handleMessage(msg);
    }
}
