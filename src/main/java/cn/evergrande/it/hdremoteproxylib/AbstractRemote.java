/**
 *****************************************************************************
 * Copyright (C) 2018-2023 HD Corporation. All rights reserved
 * File        : AbstractRemote.java
 *
 * Description : AbstractRemote,
 *
 * Creation    : 2018-05-10
 * Author      : tanguilong@evergrande.cn
 * History     : 2018-05-10, Creation
 *****************************************************************************
 */
package cn.evergrande.it.hdremoteproxylib;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.google.gson.Gson;
import cn.evergrande.it.hdnetworklib.api.biz.ApiSendReqCallback;
import cn.evergrande.it.hdnetworklib.network.common.biz.ProtocolUtil;
import cn.evergrande.it.hdnetworklib.api.model.protocal.ErrorCode;
import cn.evergrande.it.hdnetworklib.network.common.model.NetworkMsg;
import cn.evergrande.it.hdnetworklib.network.common.model.NetworkMsg.MSG_TYPE;
import cn.evergrande.it.hdnetworklib.network.common.model.protocal.CommonContentReq;
import cn.evergrande.it.hdnetworklib.network.common.model.protocal.CommonContentRes;
import cn.evergrande.it.hdnetworklib.network.tcp.TcpCallback;
import cn.evergrande.it.hdnetworklib.network.tcp.TcpClient;
import cn.evergrande.it.hdremoteproxylib.RemoteProxy.Server;
import cn.evergrande.it.hdremoteproxylib.RemoteProxy.ServerStatus;
import cn.evergrande.it.hdremoteproxylib.evaluate.NetworkEvaluator;
import cn.evergrande.it.hdremoteproxylib.exceptions.AddMdpHeaderException;
import cn.evergrande.it.hdremoteproxylib.exceptions.CanNotObtainRemoteException;
import cn.evergrande.it.hdremoteproxylib.exceptions.GetMessageTypeException;
import cn.evergrande.it.hdremoteproxylib.interfaces.IJsonResponseParser;
import cn.evergrande.it.hdremoteproxylib.interfaces.ILoginOrOutController;
import cn.evergrande.it.hdremoteproxylib.network.policy.AbstractRouteStrategy;
import cn.evergrande.it.hdremoteproxylib.network.request.RequestParams;
import cn.evergrande.it.hdremoteproxylib.interfaces.ITCPController;
import cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine;
import cn.evergrande.it.hdtoolkits.threadmanage.ThreadManager;
import cn.evergrande.it.logger.BHLog;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static cn.evergrande.it.hdnetworklib.api.model.protocal.ErrorCode.ERROR_CODE_91005;
import static cn.evergrande.it.hdremoteproxylib.network.NetworkConfigLoader.PROPERTY_KEY_ENABLE_ROUTER_CHANNEL_ENCRYPT;

public abstract class AbstractRemote {

    public final static String TAG = "AbstractRemote";

    /**
     * Remote连接模式: <br/>
     * MODE_NON_BLOCK: 非阻塞连接模式 <br/>
     * MODE_BLOCK: 阻塞连接模式
     */
    public final static int MODE_NON_BLOCK = 0;
    public final static int MODE_BLOCK = 1;

    protected Context mContext;

    protected String mIpAddress;
    protected String mPort;
    protected int mRemoteType;
    protected String mRemoteName;
    /**
     * 记录网络及登陆状态
     */
    protected ServerStatus mServerStatus;

    /**
     * TCP响应Json串解析器
     */
    private static IJsonResponseParser sJsonResponseParser;
    /**
     * Remote状态监听器
     */
    private List<IConnectionMonitor> mConnectionMonitors = new ArrayList<IConnectionMonitor>();
    /**
     * 建立连接Channel状态监听器
     */
    private List<IEstablishChannelStateMonitor> mChannelMonitors = new ArrayList<IEstablishChannelStateMonitor>();

    private List<IRemoteStateMonitor> mRemoteStateMonitors = new ArrayList<IRemoteStateMonitor>();

    private static Gson sGson = new Gson();
    /**
     * Remote状态机
     */
    protected TCPStateMachine mStateMachine;

    public TCPStateMachine getStateMachine() {
        return mStateMachine;
    }

    /**
     * 初始化该Remote
     * @param context 必须为Application
     * @param networkStatus 记录网络及登陆状态
     */
    public abstract void init(Context context, ServerStatus networkStatus);

    /**
     * 连接服务端
     * @param args, 参数数组，具体参数个数及类型由具体子类实现决定
     */
    public abstract void connect(Object[] args);

    /**
     * 检查@see connect参数数组
     * @params args, 具体参数个数及类型由具体子类实现决定, @see connect参数数组.
     * @return true, 参数数组符合要求,
     *         false, 参数数组不符合要求
     */
    protected abstract boolean checkConnectParams(Object[] args);

    /**
     * 获取Remote的名称
     */
    public String getRemoteName() {
        return mRemoteName;
    }

    /**
     * 设置Remote的地址以及端口
     * @param ipAddress IP地址
     * @param port 端口
     */
    public void setRemoteAddressPort(String ipAddress, String port) {
        mIpAddress = ipAddress;
        mPort = port;
    }

    /**
     * 获取Remote的IP地址
     * @return Remote的IP地址
     */
    public String getIpAddress() {
        return mIpAddress;
    }

    /**
     * 获取Remote的端口
     * @return Remote的端口
     */
    public String getPort() {
        return mPort;
    }

    /**
     * 判断给定IP及端口是否与本Remote相同
     * @param ipAddress IP地址
     * @param port 端口
     * @return false - 给定IP及端口与本Remote不相同
     *         true - 给定IP及端口与本Remote相同
     */
    public boolean isTheSameRemote(String ipAddress, String port) {
        if (TextUtils.isEmpty(mIpAddress)) {
            return false;
        }

        if (TextUtils.isEmpty(mPort)) {
            return false;
        }

        return mIpAddress.equals(ipAddress) && mPort.equals(port);
    }

    /**
     * 获取Remote的类型，取值REMOTE_CLOUD、REMOTE_ROUTE、REMOTE_LOGGER
     * @see RemoteProxy
     * @return Remote的类型
     */
    public int getRemoteType() {
        return mRemoteType;
    }

    /**
     * 设置Remote的类型，取值REMOTE_CLOUD、REMOTE_ROUTE、REMOTE_LOGGER
     * @param remoteType Remote的类型
     */
    protected void setRemoteType(int remoteType) {
        mRemoteType = remoteType;
    }

    /**
     * 注册Remote连接状态监听器 @see IConnectionMonitor
     * @param monitor Remote状态监听器
     */
    public void registerConnectionMonitor(IConnectionMonitor monitor) {
        mConnectionMonitors.add(monitor);
    }

    /**
     * 注销Remote连接状态监听器
     * @param monitor 待注销的Remote状态监听器
     */
    public void unregisterConnectionMonitor(IConnectionMonitor monitor) {
        mConnectionMonitors.remove(monitor);
    }

    /**
     * 注册Remote长连接状态监听器 @see IConnectionMonitor
     * @param monitor Remote状态监听器
     */
    public void registerChannelMonitor(IEstablishChannelStateMonitor monitor) {
        mChannelMonitors.add(monitor);
    }

    /**
     * 注销Remote长连接状态监听器
     * @param monitor 待注销的Remote状态监听器
     */
    public void unregisterChannelMonitor(IEstablishChannelStateMonitor monitor) {
        mChannelMonitors.remove(monitor);
    }

    /**
     * 注册Remote状态监听器
     * @param monitor
     */
    public void registerRemoteStateMonitor(IRemoteStateMonitor monitor) {
        mRemoteStateMonitors.add(monitor);
    }

    /**
     * 注册Remote状态监听器
     * @param monitor Remote状态监听器
     */
    public void unregisterRemoteStateMonitor(IRemoteStateMonitor monitor) {
        mRemoteStateMonitors.remove(monitor);
    }

    /**
     * 注册Remote登陆/注销控制器
     * @param controller 登陆/注销控制器
     */
    public void loadLoginOrLogoutController(ILoginOrOutController controller) {
        mStateMachine.loadLoginOrLogoutController(controller);
    }

    /**
     * 注销Remote登陆/注销控制器
     */
    public void unloadLoginOrLogoutController() {
        mStateMachine.unloadLoginOrLogoutController();
    }

    List<IConnectionMonitor> getConnectionMonitors() {
        return mConnectionMonitors;
    }

    List<IEstablishChannelStateMonitor> getChannelMonitors() {
        return mChannelMonitors;
    }

    void notifyRemoteStatusChanged(final int socketStatus) {
        if (mConnectionMonitors == null || mConnectionMonitors.isEmpty()) {
            BHLog.i(TAG, "notifyRemoteStatusChanged: mConnectionMonitors is null or empty ");
            return;
        }
        // 通知Socket通道变化给上层
        ThreadManager.getInstance().postUITask(new Runnable() {
            @Override
            public void run() {
                final List<IConnectionMonitor> connectionMonitors = mConnectionMonitors;
                for (IConnectionMonitor connectionMonitor : connectionMonitors) {
                    connectionMonitor.onSocketStatusChanged(socketStatus, AbstractRemote.this);
                }
            }
        });
    }

    public void notifyChannelChanged(final int channelStatus) {
        if (mChannelMonitors == null || mChannelMonitors.isEmpty()) {
            BHLog.i(TAG, "notifyChannelChanged: mChannelMonitors is null or empty ");
            return;
        }
        // 通知Channel通道变化给上层
        ThreadManager.getInstance().postUITask(new Runnable() {
            @Override
            public void run() {
                final List<IEstablishChannelStateMonitor> channelMonitors = mChannelMonitors;
                for (IEstablishChannelStateMonitor channelMonitor : channelMonitors) {
                    channelMonitor.onChannelStageChanged(channelStatus, AbstractRemote.this);
                }
            }
        });
    }

    /**
     * 释放Remote所持资源
     */
    public void release() {
        if (mConnectionMonitors != null && !mConnectionMonitors.isEmpty()) {
            mConnectionMonitors.clear();
        }

        if (mChannelMonitors != null && !mChannelMonitors.isEmpty()) {
            mChannelMonitors.clear();
        }
    }

    /**
     * 获取长连接状态
     * @return 长连接状态
     */
    public abstract RemoteStatus getRemoteStatus();

    /**
     * 断开与服务端的连接
     */
    public void disconnect() {
        String netAddress = String.format("%s:%s", mIpAddress, mPort);
        TcpClient.getInstance().closeTcpConnection(netAddress, false);

        BHLog.i(TAG, String.format("Disconnect %s", netAddress));

        mStateMachine.sendEvent(TCPStateMachine.EVENT_CMD_DISCONNECT_SUCCESS, null);
    }

    /**
     * 心跳发送
     */
    public void sendHeartbeat() {
        int networkState = RemoteProxy.getInstance().getCurNetworkEnv();
        boolean curNetworkEnabled = networkState == RemoteProxy.NETWORK_WIFI || networkState == RemoteProxy.NETWORK_MOBILE;

        tryReconnectIfNeeded(curNetworkEnabled, getRemoteType());
    }

    protected boolean isOnline() {
        int curState = mStateMachine.getState().getState();
        if (curState == TCPStateMachine.TCP_STATE_CONNECTED
                || curState == TCPStateMachine.TCP_STATE_LOGINING
                || curState == TCPStateMachine.TCP_STATE_LOGINED
                || curState == TCPStateMachine.TCP_STATE_LOGOUTING
                || curState == TCPStateMachine.TCP_STATE_LOGOUT) { // 已连接路由器, 无需尝试重连
            return true;
        }

        return false;
    }

    private void tryReconnectIfNeeded(boolean curNetworkEnabled, int remoteType) {
        if (!curNetworkEnabled) { // 网络已断开, 无需尝试重连
            BHLog.i(TAG, "tryReconnectIfNeeded: network disconnect");
            return;
        }

        Server server = null;
        if (remoteType == RemoteProxy.REMOTE_CLOUD) {
            server = mServerStatus.getCloudServer();
        } else if (remoteType == RemoteProxy.REMOTE_ROUTE) {
            server = mServerStatus.getRouterServer();
        }

        String destIp = server == null ? "" : server.getHostIpAddress();
        int destPort = server == null ? -1 : server.getPort();

        if (isOnline()) { // 已连接路由器, 无需尝试重连
            BHLog.i(TAG, String.format("tryReconnectIfNeeded: %s channel(%s:%d) already online", this, destIp, destPort));
            return;
        }

        BHLog.i(TAG, String.format("tryReconnectIfNeeded: try reconnecting Remote(type=%d)", remoteType));
        // 尝试重连
        mStateMachine.connect();
    }

    /**
     * 调用底层Tcp的接口发送数据
     *
     * @param requestParams, request请求参数， 包括target、req、timeout等
     * @param callback, TCP请求结果callback
     */
    public void sendReq(RequestParams requestParams, ApiSendReqCallback callback) {
        // 发送前预处理Tcp请求
        TcpRequestPrepareResult tcpRequestPrepareResult = prepareTcpRequest(requestParams);
        if (tcpRequestPrepareResult == null) {
            if (callback != null) {
                callback.onError(ErrorCode.ERROR_CODE_91006);
            }

            return;
        }
        // 发送Tcp请求
        doSendReq(requestParams, callback, tcpRequestPrepareResult);
    }

    private TcpRequestPrepareResult prepareTcpRequest(RequestParams requestParams) {
        TcpRequestPrepareResult tcpRequestPrepareResult;
        try {
            tcpRequestPrepareResult = dealCommonBizHeader(requestParams);
        } catch (GetMessageTypeException gmtex) {
            BHLog.e(TAG, gmtex.getMessage(), gmtex);
            return null;
        } catch (AddMdpHeaderException amhex) {
            BHLog.e(TAG, amhex.getMessage(), amhex);
            return null;
        } catch (CanNotObtainRemoteException cnrex) {
            BHLog.e(TAG, cnrex.getMessage(), cnrex);
            return null;
        }

        return tcpRequestPrepareResult;
    }

    private void doSendReq(final RequestParams requestParams, final ApiSendReqCallback callback, TcpRequestPrepareResult tcpRequestPrepareResult) {
        final String reqMethod = requestParams.getReqMethod();
        final boolean isImmediate = requestParams.isImmediate();

        final CommonContentReq commonContentReq = tcpRequestPrepareResult.getRequestCommonHeader();
        final NetworkMsg networkMsg = new NetworkMsg(commonContentReq.getReq_id(), commonContentReq.getMethod(), tcpRequestPrepareResult.getMsgType(), tcpRequestPrepareResult.getRequestData());

        int timeout = requestParams.getTimeout();
        if (timeout == RequestParams.DEFAULT_NETWORKMSG_TIMEOUT) { // timeout取值DEFAULT_NETWORKMSG_TIMEOUT， 则取策略返回的超时时间为准
            timeout = tcpRequestPrepareResult.getTimeout();
        }

        TcpClient tcpClient = TcpClient.getInstance();
        tcpClient.sendMsg(commonContentReq.getReq_id(), networkMsg, timeout, isImmediate, new TcpCallback() {
            @Override
            public void onSuccess(final Object data) {

                if (sJsonResponseParser == null) {
                    throw new NullPointerException("sJsonResponseParser is null.");
                }

                final CommonContentRes response = sJsonResponseParser.decodeJsonToResponse(reqMethod, requestParams.getNodeId(), (String) data, requestParams.getResultBeanClass());

                if (callback != null) {
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (callback == null) {
                                BHLog.i(TAG, "doSendReq:onSuccess:run: callback == null");
                                return;
                            }

                            if (response != null) {
                                callback.onSuccess(response);
                            } else {
                                callback.onError(ERROR_CODE_91005);
                            }
                        }
                    });
                }
            }

            @Override
            public void onError(final int errorCode) {
                BHLog.e(TAG, String.format("[AbstractRemote] request fail, req_id=%s, errorCode=%s", commonContentReq.getReq_id(), errorCode), false);

                if (callback != null) {
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (callback == null) {
                                BHLog.i(TAG, "doSendReq:onError:run: callback == null");
                                return;
                            }

                            callback.onError(errorCode);
                        }
                    });
                }
            }
        });
    }

    private TcpRequestPrepareResult dealCommonBizHeader(RequestParams requestParams)
            throws GetMessageTypeException, AddMdpHeaderException, CanNotObtainRemoteException {
        final CommonContentReq commonContentReq = new CommonContentReq();
        commonContentReq.setMethod(requestParams.getReqMethod());
        commonContentReq.setParams(requestParams.getParamsBean());

        String reqDataStr = sGson.toJson(commonContentReq);

        boolean needEncrypt = false;
        boolean needMdp = false;

        MSG_TYPE msgType = AbstractRouteStrategy.getMsgType(requestParams.getTarget());
        // 根据方法名决定走哪条通道
        // 以及是否需要加密
        int dest = AbstractRouteStrategy.TARGET_INVALID;
        if (msgType == MSG_TYPE.TYPE_REQ_CLOUD || msgType == MSG_TYPE.TYPE_REQ_CLOUD_WITHOUT_RES) {
            needEncrypt = true;
            dest = AbstractRouteStrategy.TARGET_CLOUD;
        } else if (msgType == MSG_TYPE.TYPE_REQ_CLOUD_TRANS) {
            needEncrypt = true;
            needMdp = true;
            dest = AbstractRouteStrategy.TARGET_CLOUD;
        } else if (msgType == MSG_TYPE.TYPE_REQ_ROUTER || msgType == MSG_TYPE.TYPE_REQ_ROUTER_WITHOUT_RES) {
            needEncrypt = true;
            try {
                Properties properties = RemoteProxy.getInstance().getNetworkProperties();
                String propValue = properties.getProperty(PROPERTY_KEY_ENABLE_ROUTER_CHANNEL_ENCRYPT);

                needEncrypt = Boolean.parseBoolean(propValue);
            } catch (Exception ex) {
                BHLog.i(TAG, ex.getMessage());
            }

            dest = AbstractRouteStrategy.TARGET_ROUTER;
        } else if (msgType == MSG_TYPE.TYPE_INVALID) {
            throw new GetMessageTypeException("[AbstractRemote] request fail，can't pick a SocketChannel");
        }

        //加mdp
        if (needMdp) {
            long curRouteId = requestParams.getCurRouterId();
            if (curRouteId == AbstractRouteStrategy.RouteStrategyResult.INVALID_VALUE) {
                throw new AddMdpHeaderException(String.format("curRouteId(%d) invalid", curRouteId));
            }

            reqDataStr = ProtocolUtil.addMdpHeader(NetworkMsg.MDP_MSG_TYPE.TYPE_P2R, curRouteId, reqDataStr);
        }

        String aesKey = "", aesIv = "";
        String destStr = "unknown";
        switch (dest) {
            case AbstractRouteStrategy.TARGET_CLOUD:
                 aesKey = CloudRemote.AES_KEY;
                 aesIv  = CloudRemote.AES_IV;

                 destStr = "cloud";
                 break;
            case AbstractRouteStrategy.TARGET_ROUTER:
                 aesKey = RouterRemote.AES_KEY;
                 aesIv  = RouterRemote.AES_IV;

                 destStr = "router";
                 break;
            default:
        }

        BHLog.i(TAG, String.format("[%s<-app] %s", destStr, reqDataStr));

        reqDataStr = ProtocolUtil.addSaferHeader(aesKey, aesIv, reqDataStr, needEncrypt) + "\n";

        //BHLog.i(TAG, "[AbstractRemote] send req to server, after add safe header:" + reqDataStr);
        // 记录本次处理结果
        TcpRequestPrepareResult tcpRequestDealResult = new TcpRequestPrepareResult();
        tcpRequestDealResult.setRequestData(reqDataStr);
        tcpRequestDealResult.setRequestCommonHeader(commonContentReq);
        tcpRequestDealResult.setTimeout(requestParams.getTimeout());
        tcpRequestDealResult.setMsgType(msgType);

        return tcpRequestDealResult;
    }

    /**
     * 注册响应结果Json串解析器
     * @param jsonResponseParser 响应结果Json串解析器
     */
    static void registerResponseJsonParser(IJsonResponseParser jsonResponseParser) {
        sJsonResponseParser = jsonResponseParser;
    }

    /**
     * 取消注册响应结果Json串解析器
     */
    static void unregisterResponseJsonParser() {
        sJsonResponseParser = null;
    }

    protected static class TcpRequestPrepareResult {
        private CommonContentReq mRequestCommonHeader;
        private String mRequestData;
        private int mTimeout; // 单位：s
        private MSG_TYPE mMsgType;

        public CommonContentReq getRequestCommonHeader() {
            return mRequestCommonHeader;
        }

        public void setRequestCommonHeader(CommonContentReq requestCommonHeader) {
            mRequestCommonHeader = requestCommonHeader;
        }

        public String getRequestData() {
            return mRequestData;
        }

        public void setRequestData(String requestData) {
            mRequestData = requestData;
        }

        public int getTimeout() {
            return mTimeout;
        }

        public void setTimeout(int timeout) {
            mTimeout = timeout;
        }

        public MSG_TYPE getMsgType() {
            return mMsgType;
        }

        public void setMsgType(MSG_TYPE msgType) {
            mMsgType = msgType;
        }
    }

    /**
     * Remote的状态：连接状态、登陆状态
     */
    public static class RemoteStatus {
        /**
         * 连接状态
         */
        private boolean mConnected;
        /**
         * 登陆状态
         */
        private boolean mLogined;
        /**
         * 网络类型, 取值
         */
        private int mNetworkType;
        /**
         * Remote状态机
         */
        private ITCPController mStateMachine;

        public boolean isConnected() {
            return mConnected;
        }

        public void setConnected(boolean connected) {
            mConnected = connected;
        }

        public boolean isLogined() {
            return mLogined;
        }

        public void setLogined(boolean logined) {
            mLogined = logined;
        }

        public int getNetworkType() {
            return mNetworkType;
        }

        public void setNetworkType(int networkType) {
            mNetworkType = networkType;
        }

        public ITCPController getStateMachine() {
            return mStateMachine;
        }

        public void setStateMachine(ITCPController stateMachine) {
            mStateMachine = stateMachine;
        }
    }

    /**
     * Remote状态监听器
     */
    public interface IConnectionMonitor {
        /**
         * Socket通道连接断开
         */
        public final static int SOCKET_CONNECTION_DISCONNECT = 0;
        /**
         * Socket通道已建立连接
         */
        public final static int SOCKET_CONNECTION_CONNECTED = 1;

        /**
         * 网络状态变化时的回调
         * @param connStatus 当前remote连接状态, 取值REMOTE_STATUS_UNKNOWN、REMOTE_STATUS_CONNECTED、REMOTE_STATUS_CONNECTING、REMOTE_STATUS_DISCONNECTED
         * @param netType 当前连接的网络类型, 取值NET_TYPE_UNKNOWN、NET_TYPE_MOBILE、NET_TYPE_WIFI、NET_TYPE_OTHER
         * @param remote remote长连接, 取值CloudRemote、RouteRemote、LoggerRemote的实例，或REMOTE_ALL
         */
        public void onNetworkChanged(int connStatus,int netType, AbstractRemote remote);

        /**
         * 网络状态变化时的回调
         * @param socketStatus socket通道联通状态, 取值SOCKET_CONNECTION_DISCONNECT、SOCKET_CONNECTION_CONNECTED
         * @param remote remote长连接, 取值CloudRemote、RouteRemote、LoggerRemote的实例，或REMOTE_ALL
         */
        public void onSocketStatusChanged(int socketStatus, AbstractRemote remote);
    }

    /**
     * SocketChannel通道建立流程变化 开始 成功 失败
     */
    public interface IEstablishChannelStateMonitor {
        //通道建立流程
        public static final int CHANNEL_STAGE_START = 1;
        public static final int CHANNEL_STAGE_SUCCESS = 2;
        public static final int CHANNEL_STAGE_FAIL = 3;

        /**
         * 建立通道状态监听器
         * @param state 取值CHANNEL_STAGE_START、CHANNEL_STAGE_SUCCESS、CHANNEL_STAGE_FAIL
         * @param remote 取值CloudRemote、RouteRemote
         */
        public void onChannelStageChanged(int state, AbstractRemote remote);
    }

    public interface IRemoteStateMonitor {
        /**
         * Remote状态变化时触发
         * @param remote 状态发生变化的Remote
         * @param oldState Remote状态发生变化前的状态
         * @param newState Remote状态发生变化后的状态
         */
        public void onRemoteStateChanged(AbstractRemote remote, int oldState, int newState);
    }

}