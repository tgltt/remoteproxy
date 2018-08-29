package cn.evergrande.it.hdremoteproxylib.statemachine;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import cn.evergrande.it.hdremoteproxylib.AbstractRemote;
import cn.evergrande.it.hdremoteproxylib.RemoteConnector;
import cn.evergrande.it.hdremoteproxylib.RemoteProxy;
import cn.evergrande.it.hdremoteproxylib.evaluate.NetworkEvaluator;
import cn.evergrande.it.hdremoteproxylib.interfaces.ILoginOrOutController;
import cn.evergrande.it.hdremoteproxylib.interfaces.ILoginResult;
import cn.evergrande.it.hdremoteproxylib.interfaces.ILogoutResult;
import cn.evergrande.it.hdremoteproxylib.statemachine.states.ConnectedState;
import cn.evergrande.it.hdremoteproxylib.statemachine.states.ConstructedState;
import cn.evergrande.it.hdremoteproxylib.statemachine.states.DestroyedState;
import cn.evergrande.it.hdremoteproxylib.statemachine.states.DisconnectedState;
import cn.evergrande.it.hdremoteproxylib.statemachine.states.InitState;
import cn.evergrande.it.hdremoteproxylib.statemachine.states.InvalidState;
import cn.evergrande.it.hdremoteproxylib.statemachine.states.LoginedState;
import cn.evergrande.it.hdremoteproxylib.statemachine.states.LoginingState;
import cn.evergrande.it.hdremoteproxylib.statemachine.states.LogoutState;
import cn.evergrande.it.hdremoteproxylib.statemachine.states.LogoutingState;
import cn.evergrande.it.hdremoteproxylib.interfaces.ITCPController;
import cn.evergrande.it.hdremoteproxylib.statemachine.states.AbstractState;
import cn.evergrande.it.hdremoteproxylib.statemachine.states.ConnectingState;
import cn.evergrande.it.hdremoteproxylib.statemachine.states.DisconnectingState;
import cn.evergrande.it.logger.BHLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by hefeng on 2018/6/12.
 *
 *   网络状态监控模块，只调activate()和deactivate()接口
 *   心跳管理模块，只调disconnect()接口
 */
public class TCPStateMachine implements ITCPController {
    private static final String TAG = "TCPStateMachine";

    // TCP网络模块的状态
    public static final int TCP_STATE_INVALID       = -1; // 无效状态
    public static final int TCP_STATE_CONSTRUCTED   = 0;  // 稳态，状态机被new后（执行构造函数）的状态
    public static final int TCP_STATE_INIT          = 1;  // 稳态，初始态，也是未连接态
    public static final int TCP_STATE_CONNECTING    = 2;  // 暂态，连接中态
    public static final int TCP_STATE_CONNECTED     = 3;  // 稳态，连接态（未登录）
    public static final int TCP_STATE_LOGINING      = 4;  // 暂态，登录中态
    public static final int TCP_STATE_LOGINED       = 5;  // 稳态，登录态
    public static final int TCP_STATE_DISCONNECTING = 6;  // 暂态，断开连接中态
    public static final int TCP_STATE_DISCONNECTED  = 7;  // 稳态，断开连接的状态
    public static final int TCP_STATE_LOGOUTING     = 8;  // 暂态，登出中态
    public static final int TCP_STATE_LOGOUT        = 9;  // 稳态，登出状态
    public static final int TCP_STATE_DESTROYED     = 10; // 稳态，状态机被销毁后的状态

    public static final int MAX_RETRY_CONNECT_TIMES = 1;
    public static final int MAX_RETRY_LOGIN_TIMES = 3;

    // 状态机操作指令
    private static final int EVENT_BASE = 0;
    public static final int EVENT_ENTER = EVENT_BASE;

    private static final int EVENT_CMD_BASE = EVENT_BASE + 100;

    public static final int EVENT_CMD_CONNECT    = EVENT_CMD_BASE + 1;
    public static final int EVENT_CMD_DISCONNECT = EVENT_CMD_BASE + 2;
    public static final int EVENT_CMD_LOGIN      = EVENT_CMD_BASE + 3;
    public static final int EVENT_CMD_LOGIN_OUT  = EVENT_CMD_BASE + 4;

    public static final int EVENT_CMD_CONNECT_SUCCESS = EVENT_CMD_BASE + 5;
    public static final int EVENT_CMD_CONNECT_FAILED  = EVENT_CMD_BASE + 6;
    public static final int EVENT_CMD_RETRY_CONNECT_SUCCESS = EVENT_CMD_BASE + 7;
    public static final int EVENT_CMD_RETRY_CONNECT_FAILED  = EVENT_CMD_BASE + 8;
    public static final int EVENT_CMD_DISCONNECT_SUCCESS = EVENT_CMD_BASE + 9;
    public static final int EVENT_CMD_DISCONNECT_FAILED  = EVENT_CMD_BASE + 10;
    public static final int EVENT_CMD_LOGIN_SUCCESS = EVENT_CMD_BASE + 11;
    public static final int EVENT_CMD_LOGIN_FAILED  = EVENT_CMD_BASE + 12;
    public static final int EVENT_CMD_LOGOUT_SUCCESS = EVENT_CMD_BASE + 13;
    public static final int EVENT_CMD_LOGOUT_FAILED  = EVENT_CMD_BASE + 14;

    private static final String STATE_MACHINE_THREAD = "state_machine_thread";

    private Map<Integer, AbstractState> mStates;

    private AbstractState mCurState;
    private AbstractState mOldState;

    private AbstractRemote mRemote;  // 状态机管理的TCP网络对象


    protected volatile boolean mNeedConnect; // 记录是否要连接服务端
    protected volatile boolean mNeedLogin;   // 记录是否要登陆服务端
    protected volatile boolean mNeedLoginOut; // 记录是否要登出服务端
    protected volatile boolean mNeedDisconnect; // 记录是否要断开与服务端的连接
    protected volatile boolean mEnabled; // 打开/关闭状态机

    protected volatile int mRetryConnectTimes;  // 连接重试次数
    protected volatile int mRetryLoginTimes;  // 登录重试次数

    private Handler mHandler;

    private ILoginOrOutController mLoginOrOutController;

    public TCPStateMachine() {
        initStates();

        mCurState = new InvalidState(this);
        switchState(TCP_STATE_CONSTRUCTED);
    }

    public void load(AbstractRemote remote){
        mRemote = remote;
        init();
    }

    private void init(){
        mRetryConnectTimes = MAX_RETRY_CONNECT_TIMES;
        mRetryLoginTimes = MAX_RETRY_LOGIN_TIMES;

        mEnabled = true;

        HandlerThread smThread = new HandlerThread(STATE_MACHINE_THREAD);
        smThread.start();

        mHandler = new Handler(smThread.getLooper()){
            @Override
            public void handleMessage(Message msg) {
                if(msg == null){
                    return;
                }

                if(mCurState != null){
                    mCurState.handleMessage(msg);
                }
            }
        };

        switchState(TCP_STATE_INIT);
    }

    private void initStates() {
        mStates = new HashMap<Integer, AbstractState>();

        mStates.put(TCP_STATE_CONSTRUCTED, new ConstructedState(this));
        mStates.put(TCP_STATE_INIT, new InitState(this));
        mStates.put(TCP_STATE_CONNECTING, new ConnectingState(this));
        mStates.put(TCP_STATE_CONNECTED, new ConnectedState(this));
        mStates.put(TCP_STATE_LOGINING, new LoginingState(this));
        mStates.put(TCP_STATE_LOGINED, new LoginedState(this));
        mStates.put(TCP_STATE_DISCONNECTING, new DisconnectingState(this));
        mStates.put(TCP_STATE_DISCONNECTED, new DisconnectedState(this));
        mStates.put(TCP_STATE_LOGOUTING, new LogoutingState(this));
        mStates.put(TCP_STATE_LOGOUT, new LogoutState(this));
        mStates.put(TCP_STATE_DESTROYED, new DestroyedState(this));
    }

    public void switchState(int state){
        int curState = mCurState.getState();
        if (curState == state) {
            BHLog.i(TAG, String.format("switchState: Remote(%s) curState(%d) == state(%d)", mRemote, curState, state));
            return;
        }

        AbstractState newState = mStates.get(state);
        if (newState == null) {
            throw new IllegalStateException(String.format("new state(%d) is illegal", state));
        }

        mOldState = mCurState;
        mCurState = newState;
        // 旧有状态退出
        mOldState.exit();
        // 新状态进入
        mCurState.enter();
    }

    public void resetStatus() {
        mEnabled = true;

        mNeedConnect = false;
        mNeedDisconnect = false;
        mNeedLogin = false;
        mNeedLoginOut = false;

        mRetryConnectTimes = MAX_RETRY_CONNECT_TIMES;
        mRetryLoginTimes = MAX_RETRY_LOGIN_TIMES;
    }

    public AbstractRemote getRemote() {
        return mRemote;
    }

    @Override
    public void connect() {
        BHLog.i(TAG, String.format("connect: To connect %s", mRemote));

        mRetryConnectTimes = MAX_RETRY_CONNECT_TIMES;
        mRetryLoginTimes = MAX_RETRY_LOGIN_TIMES;

        mHandler.sendEmptyMessage(EVENT_CMD_CONNECT);
    }

    @Override
    public void disconnect() {
        BHLog.i(TAG, String.format("disconnect: To disconnect %s", mRemote));

        mRetryConnectTimes = MAX_RETRY_CONNECT_TIMES;
        mRetryLoginTimes = MAX_RETRY_LOGIN_TIMES;

        mHandler.sendEmptyMessage(EVENT_CMD_DISCONNECT);
    }

    @Override
    public void login() {
        BHLog.i(TAG, String.format("connect: To login %s", mRemote));

        mRetryConnectTimes = MAX_RETRY_CONNECT_TIMES;
        mRetryLoginTimes = MAX_RETRY_LOGIN_TIMES;

        mHandler.sendEmptyMessage(EVENT_CMD_LOGIN);
    }

    @Override
    public void loginOut() {
        BHLog.i(TAG, String.format("connect: To loginOut %s", mRemote));

        mRetryLoginTimes = 0;
        mHandler.sendEmptyMessage(EVENT_CMD_LOGIN_OUT);
    }

    @Override
    public void activate() {
        BHLog.i(TAG, String.format("connect: To activate %s", mRemote));

        mEnabled = true;

        mRetryConnectTimes = MAX_RETRY_CONNECT_TIMES;
        mRetryLoginTimes = MAX_RETRY_LOGIN_TIMES;

        mHandler.sendEmptyMessage(EVENT_CMD_CONNECT);
    }

    @Override
    public void deactivate() {
        BHLog.i(TAG, String.format("connect: To deactivate %s", mRemote));

        mEnabled = false;

        mHandler.sendEmptyMessage(EVENT_CMD_DISCONNECT);
    }

    @Override
    public void sendEvent(int event, Object[] args) {
        if (args == null || args.length <= 0) {
            mHandler.sendEmptyMessage(event);
            return;
        }

        Message msg = mHandler.obtainMessage();
        msg.what = event;
        msg.obj = args;

        mHandler.sendMessage(msg);
    }

    public void loadLoginOrLogoutController(ILoginOrOutController controller) {
        mLoginOrOutController = controller;
    }

    public void unloadLoginOrLogoutController() {
        mLoginOrOutController = null;
    }

    public void notifyLoginOrOutControllers(boolean whetherLogin) {
        if (mLoginOrOutController == null) {
            BHLog.i(TAG, "notifyTcpControllers: No controller needs being notified");
            return;
        }

        if (whetherLogin) { // 通知登陆操作
            LoginResult loginResult = new LoginResult(mLoginOrOutController, mRemote, mRetryLoginTimes);
            mLoginOrOutController.login(mRemote.getRemoteType(), loginResult);
        } else { // 通知登出操作
            LogoutResult logoutResult = new LogoutResult(mRemote);
            mLoginOrOutController.loginOut(mRemote.getRemoteType(), logoutResult);
        }
    }

    public AbstractState getState() {
        return mCurState;
    }

    public boolean isNeedConnect() {
        return mNeedConnect;
    }

    public void setNeedConnect(boolean needConnect) {
        mNeedConnect = needConnect;
    }

    public boolean isNeedLogin() {
        return mNeedLogin;
    }

    public void setNeedLogin(boolean needLogin) {
        mNeedLogin = needLogin;
    }

    public boolean isNeedLoginOut() {
        return mNeedLoginOut;
    }

    public void setNeedLoginOut(boolean needLoginOut) {
        mNeedLoginOut = needLoginOut;
    }

    public boolean isNeedDisconnect() {
        return mNeedDisconnect;
    }

    public void setNeedDisconnect(boolean needDisconnect) {
        mNeedDisconnect = needDisconnect;
    }

    public boolean isEnabled() {
        return mEnabled;
    }

    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
    }

    public int getRetryConnectTimes() {
        return mRetryConnectTimes;
    }

    public void setRetryConnectTimes(int retryConnectTimes) {
        mRetryConnectTimes = retryConnectTimes;
    }

    public int getRetryLoginTimes() {
        return mRetryLoginTimes;
    }

    public void setRetryLoginTimes(int retryLoginTimes) {
        mRetryLoginTimes = retryLoginTimes;
    }

    public AbstractState getOldState() {
        return mOldState;
    }

    public class LoginResult implements ILoginResult {

        public final static String TAG = "LoginResult";

        private int mMaxRetryLoginTimes;
        private int mCurRetryLoginTimes;
        private ILoginOrOutController mController;
        private AbstractRemote mRemote;

        public LoginResult(ILoginOrOutController controller, AbstractRemote remote, int maxRetryLoginTimes) {
            mController = controller;
            mRemote = remote;
            mMaxRetryLoginTimes = maxRetryLoginTimes;
            mCurRetryLoginTimes = 0;
        }

        @Override
        public void loginSuccess() {
            BHLog.i(TAG, "loginSuccess");

            TCPStateMachine stateMachine = mRemote.getStateMachine();
            stateMachine.sendEvent(EVENT_CMD_LOGIN_SUCCESS, null);
        }

        @Override
        public void loginFailed() {
            BHLog.i(TAG, "loginFailed");

            if (mCurRetryLoginTimes >= mMaxRetryLoginTimes) {
                BHLog.i(TAG, String.format("loginFailed: Stop retrying login, mCurRetryLoginTimes(%d) >= mMaxRetryLoginTimes(%d)", mCurRetryLoginTimes, mMaxRetryLoginTimes));

                TCPStateMachine stateMachine = mRemote.getStateMachine();
                stateMachine.sendEvent(EVENT_CMD_LOGIN_FAILED, null);;

                return;
            }

            BHLog.i(TAG, String.format("loginFailed: Retry logining, mCurRetryLoginTimes(%d) < mMaxRetryLoginTimes(%d)", mCurRetryLoginTimes, mMaxRetryLoginTimes));

            mCurRetryLoginTimes++;
            mController.login(mRemote.getRemoteType(), this);
        }

    }

    public class LogoutResult implements ILogoutResult {

        public final static String TAG = "LoginResult";

        private AbstractRemote mRemote;

        public LogoutResult(AbstractRemote remote) {
            mRemote = remote;
        }

        @Override
        public void logoutSuccess() {
            BHLog.i(TAG, "logoutSuccess");

            TCPStateMachine stateMachine = mRemote.getStateMachine();
            stateMachine.sendEvent(EVENT_CMD_LOGOUT_SUCCESS, null);

            int remoteType = mRemote.getRemoteType();
            if (remoteType == RemoteProxy.REMOTE_ROUTE) {
                stateMachine.disconnect();
                stateMachine.connect();
            }
        }

        @Override
        public void logoutFailed() {
            BHLog.i(TAG, "logoutFailed");

            TCPStateMachine stateMachine = mRemote.getStateMachine();
            stateMachine.sendEvent(EVENT_CMD_LOGOUT_FAILED, null);
        }
    }

}
