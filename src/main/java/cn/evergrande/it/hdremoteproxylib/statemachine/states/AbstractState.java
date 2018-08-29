package cn.evergrande.it.hdremoteproxylib.statemachine.states;

import android.os.Handler;
import android.os.Message;

import cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine;

/**
 * Created by tam on 18-6-14.
 */

public abstract class AbstractState {

    public final static String TAG = "BaseState";

    protected Handler mHandler;

    protected int mState = TCPStateMachine.TCP_STATE_INVALID;

    protected TCPStateMachine mTCPStateMachine;

    /**
     * 进入该状态要做的事情
     */
    public abstract void exit();
    /**
     * 离开该状态要做的事情
     */
    public abstract void enter();

    /**
     * 处理状态事件
     */
    public boolean handleMessage(Message message) {
        switch (message.what){
            case TCPStateMachine.EVENT_CMD_CONNECT:
                  return dealConnectEvent();
            case TCPStateMachine.EVENT_CMD_DISCONNECT:
                  return dealDisconnectEvent();
            case TCPStateMachine.EVENT_CMD_LOGIN:
                  return dealLoginEvent();
            case TCPStateMachine.EVENT_CMD_LOGIN_OUT:
                  return dealLogoutEvent();
        }

        return false;
    }

    private boolean dealConnectEvent() {
        mTCPStateMachine.setNeedConnect(true);
        return true;
    }

    private boolean dealDisconnectEvent() {
        mTCPStateMachine.setNeedDisconnect(true);
        return true;
    }

    private boolean dealLoginEvent() {
        mTCPStateMachine.setNeedLogin(true);
        return true;
    }

    private boolean dealLogoutEvent() {
        mTCPStateMachine.setNeedLoginOut(true);
        return true;
    }

    public int getState() {
        return mState;
    }
}
