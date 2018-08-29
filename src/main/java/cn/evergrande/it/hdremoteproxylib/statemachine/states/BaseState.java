package cn.evergrande.it.hdremoteproxylib.statemachine.states;

import android.os.Message;
import android.text.TextUtils;

import cn.evergrande.it.hdremoteproxylib.AbstractRemote;
import cn.evergrande.it.hdremoteproxylib.evaluate.NetworkEvaluator;
import cn.evergrande.it.logger.BHLog;

import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.EVENT_CMD_CONNECT;
import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.EVENT_CMD_CONNECT_FAILED;
import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.EVENT_CMD_CONNECT_SUCCESS;
import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.EVENT_CMD_DISCONNECT;
import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.EVENT_CMD_DISCONNECT_FAILED;
import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.EVENT_CMD_DISCONNECT_SUCCESS;
import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.EVENT_CMD_LOGIN;
import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.EVENT_CMD_LOGIN_FAILED;
import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.EVENT_CMD_LOGIN_OUT;
import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.EVENT_CMD_LOGIN_SUCCESS;
import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.EVENT_CMD_LOGOUT_FAILED;
import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.EVENT_CMD_LOGOUT_SUCCESS;
import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.EVENT_CMD_RETRY_CONNECT_FAILED;
import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.EVENT_CMD_RETRY_CONNECT_SUCCESS;
import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.TCP_STATE_CONNECTED;
import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.TCP_STATE_CONNECTING;
import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.TCP_STATE_CONSTRUCTED;
import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.TCP_STATE_DESTROYED;
import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.TCP_STATE_DISCONNECTED;
import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.TCP_STATE_DISCONNECTING;
import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.TCP_STATE_INIT;
import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.TCP_STATE_INVALID;
import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.TCP_STATE_LOGINED;
import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.TCP_STATE_LOGINING;
import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.TCP_STATE_LOGOUT;
import static cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine.TCP_STATE_LOGOUTING;

/**
 * Created by tam on 18-6-15.
 */
public class BaseState extends AbstractState {

    public final static String TAG = "BaseState";

    @Override
    public void exit() {
        String remoteName = "Remote";

        AbstractRemote remote = mTCPStateMachine.getRemote();
        if (remote != null) {
            remoteName = remote.getRemoteName();
        }

        if (TextUtils.isEmpty(remoteName)) {
            remoteName = "Remote";
        }

        BHLog.i(TAG, String.format("exit: Remote(%s) state(%d, %s)", remoteName, mState, getStateLabel(mState)));
        NetworkEvaluator.getInstance().stat("", String.format("StateMachine: %s exit (%d, %s)", remoteName, mState, getStateLabel(mState)), true);
    }

    @Override
    public void enter() {
        String remoteName = "Remote";

        AbstractRemote remote = mTCPStateMachine.getRemote();
        if (remote != null) {
            remoteName = remote.getRemoteName();
        }

        if (TextUtils.isEmpty(remoteName)) {
            remoteName = "Remote";
        }

        BHLog.i(TAG, String.format("enter: Remote(%s) state(%d, %s)", remoteName, mState, getStateLabel(mState)));
        NetworkEvaluator.getInstance().stat("", String.format("StateMachine: %s enter (%d, %s)", remoteName, mState, getStateLabel(mState)), true);
    }

    protected String getStateLabel(int state) {
        switch (state) {
            case TCP_STATE_CONSTRUCTED:
                 return "Constructed";
            case TCP_STATE_INIT:
                 return "Init";
            case TCP_STATE_CONNECTING:
                 return "Connecting";
            case TCP_STATE_CONNECTED:
                 return "Connected";
            case TCP_STATE_LOGINING:
                 return "Logining";
            case TCP_STATE_LOGINED:
                 return "Logined";
            case TCP_STATE_DISCONNECTING:
                 return "Disconnecting";
            case TCP_STATE_DISCONNECTED:
                 return "Disconnected";
            case TCP_STATE_LOGOUTING:
                 return "Logouting";
            case TCP_STATE_DESTROYED:
                 return "Destroyed";
            case TCP_STATE_LOGOUT:
                 return "Logout";
            case TCP_STATE_INVALID:
                 return "Invalid";
        }

        return "Unknown";
    }



    protected String getCmdLabel(int cmd) {
        switch (cmd) {
            case EVENT_CMD_CONNECT:
                 return "Cmd Connect";
            case EVENT_CMD_DISCONNECT:
                 return "Cmd Disconnect";
            case EVENT_CMD_LOGIN:
                 return "Cmd Login";
            case EVENT_CMD_LOGIN_OUT:
                 return "Cmd LoginOut";
            case EVENT_CMD_CONNECT_SUCCESS:
                 return "Cmd ConnectSuccess";
            case EVENT_CMD_CONNECT_FAILED:
                 return "Cmd ConnectFailed";
            case EVENT_CMD_RETRY_CONNECT_SUCCESS:
                 return "Cmd RetryConnectSuccess";
            case EVENT_CMD_RETRY_CONNECT_FAILED:
                 return "Cmd RetryConnectFailed";
            case EVENT_CMD_DISCONNECT_SUCCESS:
                 return "Cmd DisconnectSuccess";
            case EVENT_CMD_DISCONNECT_FAILED:
                 return "Cmd DisconnectFailed";
            case EVENT_CMD_LOGIN_SUCCESS:
                 return "Cmd LoginSuccess";
            case EVENT_CMD_LOGIN_FAILED:
                 return "Cmd LoginFailed";
            case EVENT_CMD_LOGOUT_SUCCESS:
                 return "Cmd LogoutSuccess";
            case EVENT_CMD_LOGOUT_FAILED:
                 return "Cmd LogoutFailed";
        }

        return "Unknown command";
    }

    public boolean handleMessage(Message message) {
        BHLog.i(TAG, String.format("handleMessage: Remote(%s) msg.what=%s, curState=%s", mTCPStateMachine.getRemote(), getCmdLabel(message.what), getStateLabel(mState)));
        return super.handleMessage(message);
    }
}
