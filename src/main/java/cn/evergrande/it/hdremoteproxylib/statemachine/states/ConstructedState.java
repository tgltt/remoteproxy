package cn.evergrande.it.hdremoteproxylib.statemachine.states;

import cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine;

/**
 * Created by tam on 18-6-15.
 */

public class ConstructedState extends BaseState {

    public final static String TAG = "ConstructedState";

    public ConstructedState(TCPStateMachine stateMachine) {
        mState = TCPStateMachine.TCP_STATE_CONSTRUCTED;
        mTCPStateMachine = stateMachine;

        mTCPStateMachine.resetStatus();
    }

}