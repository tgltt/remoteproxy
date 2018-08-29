package cn.evergrande.it.hdremoteproxylib.statemachine.states;

import cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine;

import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.TCP_STATE_DESTROYED;

/**
 * Created by tam on 18-6-15.
 */

public class DestroyedState extends BaseState {

    public final static String TAG = "DestroyedState";

    public DestroyedState(TCPStateMachine stateMachine) {
        mState = TCP_STATE_DESTROYED;
        mTCPStateMachine = stateMachine;
    }

}