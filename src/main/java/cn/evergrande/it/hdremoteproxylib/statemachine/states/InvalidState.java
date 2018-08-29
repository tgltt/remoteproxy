package cn.evergrande.it.hdremoteproxylib.statemachine.states;

import cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine;

import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.TCP_STATE_INVALID;

/**
 * Created by tam on 18-6-20.
 */

public class InvalidState extends BaseState {

    public final static String TAG = "InvalidState";

    public InvalidState(TCPStateMachine stateMachine) {
        mState = TCP_STATE_INVALID;
        mTCPStateMachine = stateMachine;
    }

}