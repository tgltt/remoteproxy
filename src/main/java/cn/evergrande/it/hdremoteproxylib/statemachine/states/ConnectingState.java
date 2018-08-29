package cn.evergrande.it.hdremoteproxylib.statemachine.states;

import android.os.Message;
import android.util.Log;

import cn.evergrande.it.hdremoteproxylib.AbstractRemote;
import cn.evergrande.it.hdremoteproxylib.RemoteConnector;
import cn.evergrande.it.hdremoteproxylib.RemoteProxy;
import cn.evergrande.it.hdremoteproxylib.evaluate.NetworkEvaluator;
import cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine;
import cn.evergrande.it.logger.BHLog;

import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.EVENT_CMD_DISCONNECT_SUCCESS;
import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.TCP_STATE_INIT;

/**
 * Created by tam on 18-6-14.
 */

public class ConnectingState extends BaseState {

    public static final String TAG = "ConnectingState";

    public ConnectingState(TCPStateMachine stateMachine) {
        mState = TCPStateMachine.TCP_STATE_CONNECTING;
        mTCPStateMachine = stateMachine;
    }

    @Override
    public void enter() {
        super.enter();

        AbstractRemote remote = mTCPStateMachine.getRemote();
        if (remote == null) {
            throw new IllegalArgumentException("remote == null");
        }

        if (mTCPStateMachine.isNeedDisconnect()) {
            BHLog.i(TAG, String.format("enter: Remote(%s) need disconnecting, now switch to [Disconnecting] state", remote));

            mTCPStateMachine.setNeedDisconnect(false);
            mTCPStateMachine.switchState(TCPStateMachine.TCP_STATE_DISCONNECTING);

            return;
        }

        int retryConnectTimes = mTCPStateMachine.getRetryConnectTimes();
        if (retryConnectTimes <= 0) {
            BHLog.i(TAG, String.format("enter: Remote(%s) retryConnectTimes(%d) <= 0", remote, retryConnectTimes));

            dealConnectFailure();
            return;
        }

        BHLog.i(TAG, String.format("enter: Remote(%s) start connecting ...", remote));
        // 启动连接动作
        int remoteType = remote.getRemoteType();
        int reconnectMode = RemoteConnector.remoteType2ReconnectMode(remoteType);
        // 连接Remote
        RemoteConnector remoteConnector = RemoteConnector.getInstance();
        remoteConnector.reconnect(reconnectMode, false);
    }

    @Override
    public boolean handleMessage(Message msg){
        BHLog.i(TAG, String.format("handleMessage: Remote(%s) msg.what=%s, curState=%s", mTCPStateMachine.getRemote(), getCmdLabel(msg.what), getStateLabel(mState)));

        boolean handled = false;
        switch (msg.what) {
            case TCPStateMachine.EVENT_CMD_CONNECT_SUCCESS:
            case TCPStateMachine.EVENT_CMD_RETRY_CONNECT_SUCCESS:
                 dealConnectSuccess();
                 handled = true;
                 break;
            case TCPStateMachine.EVENT_CMD_CONNECT_FAILED:
            case TCPStateMachine.EVENT_CMD_RETRY_CONNECT_FAILED:
                 dealConnectFailure();
                 handled = true;
                 break;
            case TCPStateMachine.EVENT_CMD_CONNECT:
                 mTCPStateMachine.setNeedDisconnect(false);
                 handled = true;
                 break;
            case TCPStateMachine.EVENT_CMD_LOGIN_OUT:
                 mTCPStateMachine.setNeedLogin(false);
                 handled = true;
                 break;
        }
        // 尚未处理本次消息, 交由父类去处理该消息
        return handled ? handled : super.handleMessage(msg);
    }

    private void dealConnectSuccess() {
        BHLog.i(TAG, String.format("dealConnectSuccess: Remote(%s) switching to [Connected] state", mTCPStateMachine.getRemote()));

        mTCPStateMachine.switchState(TCPStateMachine.TCP_STATE_CONNECTED);
    }

    private void dealConnectFailure() {
        BHLog.i(TAG, String.format("dealConnectFailure: Remote(%s) switching to [Disconnected] state", mTCPStateMachine.getRemote()));

        if (mTCPStateMachine.isNeedLogin()) {
            mTCPStateMachine.setNeedLogin(false);
        }

        mTCPStateMachine.switchState(TCPStateMachine.TCP_STATE_DISCONNECTED);
    }
}
