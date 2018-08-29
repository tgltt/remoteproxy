/**
 *****************************************************************************
 * Copyright (C) 2018-2023 HD Corporation. All rights reserved
 * File        : RemoteProxy.java
 *
 * Description : RemoteProxy,
 *
 * Creation    : 2018-05-10
 * Author      : tanguilong@evergrande.cn
 * History     : 2018-05-10, Creation
 *****************************************************************************
 */
package cn.evergrande.it.hdremoteproxylib;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.v4.content.PermissionChecker;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import cn.evergrande.it.hdremoteproxylib.evaluate.NetworkEvaluator;
import cn.evergrande.it.hdremoteproxylib.interfaces.ILoginOrOutController;
import cn.evergrande.it.hdremoteproxylib.exceptions.CanNotObtainRemoteException;
import cn.evergrande.it.hdremoteproxylib.exceptions.RemoteProxyNotInitException;
import cn.evergrande.it.hdremoteproxylib.exceptions.RequestParamsIncorrectException;
import cn.evergrande.it.hdremoteproxylib.interfaces.IJsonResponseParser;
import cn.evergrande.it.hdremoteproxylib.network.NetworkConfigLoader;
import cn.evergrande.it.hdremoteproxylib.network.data.actions.ConnectCloudRemoteAction;
import cn.evergrande.it.hdremoteproxylib.network.data.actions.ConnectLoggerRemoteAction;
import cn.evergrande.it.hdremoteproxylib.network.data.actions.ConnectRouteRemoteAction;
import cn.evergrande.it.hdremoteproxylib.network.data.actions.DisconnectCloudRemoteAction;
import cn.evergrande.it.hdremoteproxylib.network.data.actions.DisconnectLoggerRemoteAction;
import cn.evergrande.it.hdremoteproxylib.network.data.actions.DisconnectRouteRemoteAction;
import cn.evergrande.it.hdremoteproxylib.network.policy.AbstractRouteStrategy;
import cn.evergrande.it.hdremoteproxylib.network.policy.DefaultStrategy;
import cn.evergrande.it.hdremoteproxylib.network.request.RequestParams;
import cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import cn.evergrande.it.hdnetworklib.api.biz.ApiSendJsonCallback;
import cn.evergrande.it.hdnetworklib.api.biz.ApiSendReqCallback;
import cn.evergrande.it.hdnetworklib.api.model.protocal.ErrorCode;
import cn.evergrande.it.hdnetworklib.network.common.biz.ProtocolUtil;
import cn.evergrande.it.hdnetworklib.network.common.model.NetworkMsg;
import cn.evergrande.it.hdnetworklib.network.http2.net.NetworkUtil;
import cn.evergrande.it.hdnetworklib.network.tcp.TcpCallback;
import cn.evergrande.it.hdnetworklib.network.tcp.TcpClient;
import cn.evergrande.it.hdnetworklib.network.tcp.TcpConnectionChangedListener;
import cn.evergrande.it.hdnetworklib.network.tcp.TcpExceptionListener;
import cn.evergrande.it.logger.BHLog;

import static cn.evergrande.it.hdnetworklib.api.model.protocal.ErrorCode.ERROR_CODE_91005;
import static cn.evergrande.it.hdremoteproxylib.RemoteConnector.RECONNECT_MODE_ALL;
import static cn.evergrande.it.hdremoteproxylib.RemoteConnector.RECONNECT_MODE_CLOUD;
import static cn.evergrande.it.hdremoteproxylib.RemoteConnector.RECONNECT_MODE_ROUTE;
import static cn.evergrande.it.hdremoteproxylib.network.NetworkConfigLoader.PROPERTY_KEY_ENABLE_ROUTER_CHANNEL_ENCRYPT;

public final class RemoteProxy {

    public final static String TAG = "RemoteProxy";

    /*环境描述*/
    /**
     * 开发联调环境描述
     */
    public final static String ENV_DESC_DEV = "开发联调环境";
    /**
     * 硬件提测环境描述
     */
    public final static String ENV_DESC_SOFT_TEST = "软件提测环境";
    /**
     * 硬件提测环境描述
     */
    public final static String ENV_DESC_HARD_TEST = "硬件提测环境";
    /**
     * 老板专用环境描述
     */
    public final static String ENV_DESC_FOR_BOSS = "老板专用环境";
    /**
     * 预发布专用环境描述
     */
    public final static String ENV_DESC_FOR_YUFABU = "预发布专用环境";
    /**
     * 沙箱环境描述
     */
    public final static String ENV_DESC_FOR_SANDBOX = "沙箱环境";
    /**
     * 家居体验环境
     */
    public static final String ENV_DESC_FOR_JIAJU = "家居体验环境";

    /*打包常量*/
    /**
     * 发布环境
     */
    public final static String ENV_RELEASE = "Release";
    /**
     * 老板环境
     */
    public final static String ENV_BOSS = "Boss";
    /**
     * 开发环境
     */
    public final static String ENV_DEV = "Dev";
    /**
     * 软件测试环境
     */
    public final static String ENV_SOFT_TEST = "SoftTest";
    /**
     * 硬件测试环境
     */
    public final static String ENV_HARD_TEST = "HardTest";
    /**
     * 预发布环境
     */
    public final static String ENV_YUFABU = "Yufabu";
    /**
     * 沙箱环境
     */
    public final static String ENV_SANDBOX = "Sandbox";
    /**
     * 家居体验环境
     */
    public static final String ENV_JIAJU = "Jiaju";

    /**
     * 网络状态数据传递键值，兼容老版本库设计，socket通道类型
     */
    public final static String SOCKET_CHANNEL_TYPE = "socket_type";

    /*安全服务器域名*/
    /**
     * 开发联调环境Sec服务器域名
     */
    public static final String SEC_SERVER_HOST_NAME_DEV = "dev-security.egtest.cn";
    /**
     * 软件提测环境Sec服务器域名
     */
    public final static String SEC_SERVER_HOST_NAME_SOFT_TEST = "softtest-security.evergrande.me";
    /**
     * 硬件件提测环境Sec服务器域名
     */
    public static final String SEC_SERVER_HOST_NAME_HARD_TEST = "hardtest-security.evergrande.me";
    /**
     * 老板专用环境Sec服务器域名
     */
    public final static String SEC_SERVER_HOST_NAME_FOR_BOSS = "poc-security.evergrande.me";
    /**
     * 预发布专用环境Sec服务器域名
     */
    public final static String SEC_SERVER_HOST_NAME_FOR_YUFABU = "yufabu-security.evergrande.me";
    /**
     * 沙箱专用环境Sec服务器域名
     */
    public final static String SEC_SERVER_HOST_NAME_FOR_SANDBOX = "sandbox-security.evergrande.me";
    /**
     * 家居体验环境
     */
    public static final String SEC_SERVER_HOST_NAME_FOR_JIAJU = "jiaju-show-security.xl.cn";

    /*LB服务器域名*/
    /**
     * 开发联调环境LB服务器域名
     */
    public final static String LB_SERVER_HOST_NAME_DEV = "dev-lb.egtest.cn";
    /**
     * 软件提测环境
     */
    public final static String LB_SERVER_HOST_NAME_SOFT_TEST = "softtest-lb.evergrande.me";
    /**
     * 硬件件提测环境
     */
    public final static String LB_SERVER_HOST_NAME_HARD_TEST = "hardtest-lb.evergrande.me";
    /**
     * 老板专用环境
     */
    public final static String LB_SERVER_HOST_NAME_FOR_BOSS = "poc-lb.evergrande.me";
    /**
     * 预发布专用环境
     */
    public final static String LB_SERVER_HOST_NAME_FOR_YUFABU = "yufabu-lb.evergrande.me";
    /**
     * 沙箱专用环境
     */
    public final static String LB_SERVER_HOST_NAME_FOR_SANDBOX = "sandbox-lb.evergrande.me";
    /**
     * 家居体验环境
     */
    public static final String LB_SERVER_HOST_NAME_FOR_JIAJU = "jiaju-show-lb.xl.cn";

    /*Remote类型*/
    /**
     * 云端Remote
     */
    public final static Integer REMOTE_CLOUD = new Integer(0);
    /**
     * 路由器Remote
     */
    public final static Integer REMOTE_ROUTE = new Integer(1);
    /**
     * 日志服务器Remote
     */
    public final static Integer REMOTE_LOGGER = new Integer(2);
    /**
     * 代表所有服务器Remote
     */
    public final static Integer REMOTE_ALL = new Integer(3);

    /*当前网络环境*/
    /**
     * 网络状态未知
     */
    public final static int NETWORK_UNKNOWN = 0;
    /**
     * 当前网络为wifi，<br/>
     * <b>注意：在wifi和mobile同时连接的情况，只认定为wifi网络</b>
     */
    public final static int NETWORK_WIFI    = 1;
    /**
     * 当前网络为mobile网络
     */
    public final static int NETWORK_MOBILE  = 2;
    /**
     * 其他网络，比如蓝牙等
     */
    public final static int NETWORK_OTHER   = 3;

    /*网络状态*/
    /**
     * 未知状态
     */
    public final static int NETWORK_STATUS_UNKNOWN      = 0;
    /**
     * 已连接网络
     */
    public final static int NETWORK_STATUS_CONNECTED    = 1;
    /**
     * 正在连接网络
     */
    public final static int NETWORK_STATUS_CONNECTING   = 2;
    /**
     * 网络断开
     */
    public final static int NETWORK_STATUS_DISCONNECTED = 3;

    /**
     * 安全服务器端口
     */
    public final static int SEC_SERVER_HOST_PORT = 20086;
    /**
     * LB服务器端口
     */
    public final static int LB_SERVER_HOST_PORT = 10086;

    /**
     * 路由器服务端端口
     */
    public static volatile int DEFAULT_ROUTER_HOST_PORT = 5100;
    /**
     * 安全服务器ip地址
     */
    public static String DEFAULT_SEC_SERVER_IP = "119.23.168.152";//172.18.254.50
    /**
     * LB服务器ip地址
     */
    public static String DEFAULT_LB_SERVER_IP = "119.23.168.152";//172.18.254.50

    /**
     * 是否已初始化
     */
    private boolean hasInit;
    /**
     * 当前网络环境
     */
    private int mCurNetworkEnv = NETWORK_UNKNOWN;

    private Context mContext;
    /**
     * 全局唯一ID (GUID) 对应用实例进行唯一标识
     */
    private String mUniqueID;
    /**
     * TcpConnection状态监听器
     */
    private TcpConnectionChangedListener mTcpConnectionChangedListener;

    private TcpExceptionListener mTcpExceptionListener;


    private Map<Integer, AbstractRemote> mRemotes;

    private HandlerThread mHandlerThread;
    /**
     * 记录网络连接及登陆状态
     */
    private ServerStatus mServerStatus;
    /**
     * Remote连接器
     */
    private RemoteConnector mRemoteConnector;

    private Properties mNetworkProperties;

    private static class SingletonHolder {
        private static RemoteProxy sInstance = new RemoteProxy();
    }

    /**
     * 获取RemoteProxy实例，RemoteProxy在整个App生命周期中，有且仅有一个实例
     * @return RemoteProxy实例
     */
    public static RemoteProxy getInstance() {
        return SingletonHolder.sInstance;
    }

    private RemoteProxy() {
    }

    /**
     * 初始化RemoteProxy
     * @param context 必须传Application Context，否则会抛出IllegalArgumentException
     * @param env 打包或运行环境，如ENV_RELEASE、ENV_BOSS、ENV_DEV等
     * @param jsonResponseParser Tcp响应结果解析器
     */
    public void init(Context context, String env, long curRouterId, IJsonResponseParser jsonResponseParser) {
        if (hasInit)
            return;

        if (!(context instanceof Application)) {
            throw new IllegalArgumentException("context must be Application");
        }

        loadNetworkConfig(context); // 加载网络配置
        loadNetworkEvaluator(context); // 加载网络评估模块

        mContext = context;
        mServerStatus = new ServerStatus(env); // 记录网络连接及登陆状态

        mHandlerThread = new HandlerThread(String.format("HandlerThread[%s]", getClass().getName()));
        mHandlerThread.start();
        // 初始化Action管理器
        ActionManager.getInstance().init();

        initUUID(context);
        initNetworkLib(context);
        initRemotes(curRouterId);

        // 构建并初始化Remote连接器
        mRemoteConnector = RemoteConnector.getInstance();
        mRemoteConnector.init(mContext, mRemotes, mServerStatus);

        AbstractRemote.registerResponseJsonParser(jsonResponseParser);
        // 注册监听底层网络库TcpConnection状态监听器
        mTcpConnectionChangedListener = new TcpConnectionChangedListenerIml();
        TcpClient.getInstance().registerTcpConnectionChangedListener(mTcpConnectionChangedListener);
        // 注册Tcp链接异常监听器
        mTcpExceptionListener = new TcpExceptionListenerImpl(context);
        TcpClient.getInstance().registerTcpExceptionListener(mTcpExceptionListener);

        hasInit = true;
    }

    private void loadNetworkEvaluator(Context context) {
        NetworkEvaluator networkEvaluator = NetworkEvaluator.getInstance();
        networkEvaluator.init(context);
    }

    private void loadNetworkConfig(Context context) {
        mNetworkProperties = NetworkConfigLoader.loadNetworkConfig(context);
        if (mNetworkProperties == null || mNetworkProperties.isEmpty()) {
            BHLog.i(TAG, "loadNetworkConfig: mNetworkProperties is null or empty");
            return;
        }
        // 查找注释配置项
        final Set<Object> setNoteKeys = new HashSet<Object>();
        final Set<Object> setKeys = mNetworkProperties.keySet();
        for (Object objKey : setKeys) {
            if (objKey == null) { // 忽略null键
                continue;
            }

            String keyName = objKey.toString().trim();
            if (TextUtils.isEmpty(keyName)) { // 忽略空串键
                return;
            }

            if (keyName.charAt(0) == '#') {
                setNoteKeys.add(objKey);
            }
        }
        // 去除注释配置项
        for (Object objNoteKey : setNoteKeys) {
            mNetworkProperties.remove(objNoteKey);
        }
    }

    private void initNetworkLib(Context context) {
        // Update router address
        updateRouterGatewayIp(context);
        // Init TcpClient
        TcpClient tcpClient = TcpClient.getInstance();
        tcpClient.init(context);
        // Register Mdp handler
        tcpClient.registerMdpMessageHandler(new MdpMessageParser());
    }

    private void initRemotes(long curRouterId) {
        RouterRemote.setCurRouterId(curRouterId);

        mRemotes = new HashMap<Integer, AbstractRemote>();

        final AbstractRemote cloudRemote = CloudRemote.getInstance();
        final AbstractRemote routeRemote = RouterRemote.getInstance();

        mRemotes.put(REMOTE_CLOUD, cloudRemote);
        mRemotes.put(REMOTE_ROUTE, routeRemote);

        cloudRemote.init(mContext, mServerStatus);
        routeRemote.init(mContext, mServerStatus);

        //doConnectCloudRemote(cloudRemote);
        //doConnectRouteRemote(routeRemote);
    }

    public void updateRouterId(long curRouterId) {
        RouterRemote.setCurRouterId(curRouterId);
    }


    /**
     * 使能Remote
     */
    public void enableRemotes() {
        setRemoteStateMachineStatus(true);
    }

    /**
     * 禁用Remote
     */
    public void disableRemotes() {
        setRemoteStateMachineStatus(false);
    }

    private void setRemoteStateMachineStatus(boolean enable) {
        if (mRemotes == null || mRemotes.isEmpty()) {
            BHLog.i(TAG, "setRemoteStateMachineStatus: mRemotes is null or empty");
            return;
        }

        final Collection<AbstractRemote> remotes =  mRemotes.values();
        for (AbstractRemote remote : remotes) {
            TCPStateMachine stateMachine = remote.getStateMachine();
            if (enable) {
                stateMachine.activate();
            } else {
                stateMachine.deactivate();
            }
        }

    }

    public void disconnectAll() {
        mRemoteConnector.disconnectAll();
    }

    public void disconnect(int remoteType) {
        mRemoteConnector.disconnect(remoteType);
    }

    /**
     * 连接远端服务器，本接口仅用于支持状态机触发机制，真正实现重连功能的接口为reconnect方法
     * @param remoteType REMOTE_CLOUD：连接云端，
     *                   REMOTE_ROUTE：连接路由器，
     *                   REMOTE_LOGGER：连接日志服务器，
     *                   REMOTE_ALL: 连接所有服务器，包括但不限于云端、路由器、日志服务器等
     */
    public void connect(int remoteType) {
        if (remoteType == REMOTE_CLOUD) {
            doConnectCloud();
        } else if (remoteType == REMOTE_ROUTE) {
            doConnectRoute();
        } else if (remoteType == REMOTE_LOGGER) {
            doConnectLogger();
        } else {
            doConnectAll();
        }
    }

    private void doConnectCloud() {
        if (mRemotes == null || mRemotes.isEmpty()) {
            BHLog.i(TAG, "doConnectCloud: mRemotes is null or empty");
            return;
        }

        final AbstractRemote cloudRemote = mRemotes.get(REMOTE_CLOUD);
        if (cloudRemote == null) {
            BHLog.i(TAG, "doConnectCloud: cloudRemote is null");
            return;
        }

        TCPStateMachine stateMachine = cloudRemote.getStateMachine();
        stateMachine.connect();
    }

    private void doConnectRoute() {
        if (mRemotes == null || mRemotes.isEmpty()) {
            BHLog.i(TAG, "doConnectRoute: mRemotes is null or empty");
            return;
        }

        final AbstractRemote routeRemote = mRemotes.get(REMOTE_ROUTE);
        if (routeRemote == null) {
            BHLog.i(TAG, "doConnectRoute: routeRemote is null");
            return;
        }

        TCPStateMachine stateMachine = routeRemote.getStateMachine();
        stateMachine.connect();
    }

    private void doConnectLogger() {
        mRemoteConnector.reconnect(RemoteConnector.RECONNECT_MODE_LOGGER, true);
    }

    private void doConnectAll() {
        doConnectCloud();
        doConnectRoute();
        doConnectLogger();
    }

    /**
     * 创建并连接日志服务器
     * @param args 连接参数
     */
    public void connect2LoggerRemote(Object[] args) {
        AbstractRemote remote = mRemotes.get(REMOTE_LOGGER);

        TcpCallback callback = null;
        if (args != null && args.length > 0 && args[0] instanceof TcpCallback) {
            callback = (TcpCallback) args[0];
        }

        mRemoteConnector.doConnectLoggerRemote(remote, callback);
    }

    /**
     * 更新日志Remote
     * @param ip 日志服务器IP地址
     * @param port 日志服务器端口
     */
    public void updateLoggerRemote(String ip, int port) {
        if (TextUtils.isEmpty(ip) || port < 0) {
            BHLog.i(TAG, String.format("updateLoggerRemote: params illegal, ip=%s, port=%d", ip, port));
            return;
        }

        if (hasInit) {
            BHLog.i(TAG, "updateLoggerRemote: RemoteProxy not init");
            return;
        }

        Server logServer = mServerStatus.getLogServer();
        // 更新日志服务器信息
        logServer.setHostIpAddress(ip);
        logServer.setPort(port);
        // 获取日志Remote, 在必要时进行初始化
        AbstractRemote loggerRemote = mRemotes.get(REMOTE_LOGGER);
        if (loggerRemote == null) {
            loggerRemote = LoggerRemote.getInstance();
            loggerRemote.init(mContext, mServerStatus);

            mRemotes.put(REMOTE_LOGGER, loggerRemote);
        }
        // 调用连接器连接日志服务器
        mRemoteConnector.updateLoggerRemote(loggerRemote, ip, port);
    }

    /**
     * 释放RemoteProxy所持有的资源
     */
    public void destroy() {
        hasInit = false;
        // 释放Remote连接器资源,包括但不限于 关闭所有Remote连接
        mRemoteConnector.release();

        AbstractRemote.unregisterResponseJsonParser();

        if (mRemotes != null && !mRemotes.isEmpty()) { // 释放Remote所持有资源
            Set<Integer> keyIds = mRemotes.keySet();
            for (Integer keyId : keyIds) {
                AbstractRemote remote = mRemotes.get(keyId);
                remote.release();
            }
        }
        // 注销Action管理器
        ActionManager.getInstance().release();
        // 注销TcpConnection连接状态监听器
        if (mTcpConnectionChangedListener != null) {
            TcpClient.getInstance().unregisterTcpConnectionChangedListener(mTcpConnectionChangedListener);
        }
        // 注销Tcp链接异常监听器
        if (mTcpExceptionListener != null) {
            TcpClient.getInstance().unregisterTcpExceptionListener(mTcpExceptionListener);
        }

        mHandlerThread.quit();

        mServerStatus.release();
        TcpClient.getInstance().release();

        NetworkEvaluator.getInstance().release();

        mServerStatus = null;
        mUniqueID = null;
        mContext  = null;
        mHandlerThread = null;
    }

    /**
     * <font color="red">TCP数据发送的<b>唯一接口</b>，调用底层Tcp的接口发送数据</font>
     *
     * @param requestParams, request请求参数， 包括target、req、timeout等
     * @param callback, TCP请求结果callback
     * @throws RemoteProxyNotInitException 未调用RemoteProxy init而直接使用本方法时抛出，
     *         RequestParamsIncorrectException requestParams为null时抛出
     */
    public void sendReq(RequestParams requestParams, ApiSendReqCallback callback) throws RemoteProxyNotInitException, RequestParamsIncorrectException, CanNotObtainRemoteException {
        if (!hasInit) {
            throw new RemoteProxyNotInitException("RemoteProxy not init");
        }

        if (requestParams == null) {
            throw new IllegalArgumentException("requestParams is null");
        }

        int target = requestParams.getTarget();
        AbstractRemote remote = obtainRemoteByTarget(target);
        if (remote == null) { // 自动选择路由
            AbstractRouteStrategy routeStrategy = requestParams.getRouteStrategy(); // 获取指定路由策略
            if (routeStrategy == null) { // 未指定路由策略
                routeStrategy = new DefaultStrategy(mServerStatus); // 采用缺省策略
            }
            // 获取路由目标
            AbstractRouteStrategy.RouteStrategyParams rsp = new AbstractRouteStrategy.RouteStrategyParams();
            rsp.setRequestMethod(requestParams.getReqMethod());
            // 根据路由策略, 获取指定Remote
            AbstractRouteStrategy.RouteStrategyResult routeStrategyResult = routeStrategy.selectRemoteTarget(rsp);
            remote = obtainRemoteByTarget(routeStrategyResult.getTarget());
            // 调整request发送目标
            requestParams.adjustTarget(routeStrategyResult.getTarget());
            // 尝试重连Remote
            tryReconnectRemoteIfNeeded(remote, routeStrategyResult);
        }

        if (remote == null) { // 采用路由策略后, 仍未选择路由目标
            throw new RequestParamsIncorrectException("TARGET_AUTO mode with incorrect route strategy");
        }

        AbstractRemote.RemoteStatus remoteStatus = remote.getRemoteStatus();
        if (remoteStatus == null || !remoteStatus.isConnected()) { // Remote状态未知或与远端断开, 本次发送请求流程中断
            BHLog.i(TAG, String.format("sendReq: Remote(%s) status unknown or disconnected, and try reconnecting", remote));
            // 重连网络
            TCPStateMachine stateMachine = remote.getStateMachine();
            stateMachine.connect();

            if (callback != null) {
                callback.onError(ErrorCode.ERROR_CODE_91017);
            }

            return;
        }
        // 发送请求
        remote.sendReq(requestParams, callback);
    }

    private void tryReconnectRemoteIfNeeded(AbstractRemote remote, AbstractRouteStrategy.RouteStrategyResult routeStrategyResult) {
        if (remote != null) {
            BHLog.i(TAG, "tryReconnectRemoteIfNeeded: remote is not null");
            return;
        }
        // Remote为null，表示策略未找到合适的通道，大多数时候是由于网络连接断开或登陆状态不符合要求
        int expectedTarget = routeStrategyResult.getTarget();
        if (expectedTarget == AbstractRouteStrategy.TARGET_CLOUD) {
            doConnectCloud();
        } else if (expectedTarget == AbstractRouteStrategy.TARGET_ROUTER) {
            doConnectRoute();
        } else {
            doConnectAll();
        }
    }

    private AbstractRemote obtainRemoteByTarget(int target) {
        switch (target) {
            case AbstractRouteStrategy.TARGET_CLOUD:
            case AbstractRouteStrategy.TARGET_CLOUD_NOT_WAITING_RESPONSE:
            case AbstractRouteStrategy.TARGET_CLOUD_TRANSPARENT_TRANSFER:
                 return mRemotes.get(REMOTE_CLOUD);
            case AbstractRouteStrategy.TARGET_ROUTER:
            case AbstractRouteStrategy.TARGET_ROUTER_NOT_WAITING_RESPONSE:
                 return mRemotes.get(REMOTE_ROUTE);
            case AbstractRouteStrategy.TARGET_LOGGER_NOT_WAITING_RESPONSE:
                 // TODO： Add LoggerRemote
            case AbstractRouteStrategy.TARGET_INVALID:
            default:
                 return null;
        }
    }

    protected void initUUID(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("RemoteProxy: initUUID, context is null!");
        }
        // 获取目标版本
        int targetSdkVersion = Build.VERSION.SDK_INT;
        try {
            final PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            targetSdkVersion = info.applicationInfo.targetSdkVersion;
        } catch (PackageManager.NameNotFoundException e) {
            BHLog.e(TAG, e.getMessage(), e);
        }
        // 检查用户授权
        int checkPermResult = PackageManager.PERMISSION_GRANTED;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (targetSdkVersion >= Build.VERSION_CODES.M) {
                checkPermResult = context.checkSelfPermission("android.permission.READ_PHONE_STATE");
            } else {
                checkPermResult = PermissionChecker.checkSelfPermission(context, "android.permission.READ_PHONE_STATE");
            }
        }

        if (checkPermResult == PackageManager.PERMISSION_DENIED) {
            BHLog.i(TAG, "RemoteProxy: initUUID, no permission to getDeviceId");
            return;
        }
        // 获取MAC作为uuid
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        mUniqueID = telephonyManager.getDeviceId();
        // 将UUID传给底层网络库
        ProtocolUtil.setUniqueID(mUniqueID);
    }

    /**
     * 注册Remote状态监听器 @see IConnectionMonitor
     * @param remoteType remote长连接, 取值CloudRemote、RouteRemote、LoggerRemote的实例，或REMOTE_ALL
     * @param monitor Remote状态监听器
     */
    public void registerConnectionMonitor(int remoteType, AbstractRemote.IConnectionMonitor monitor) {
        if (monitor == null) {
            BHLog.i(TAG, "registerConnectionMonitor: monitor is null");
            return;
        }

        if (remoteType == REMOTE_ALL) {
            final Set<Integer> keyIds = mRemotes.keySet();
            for (Integer keyId : keyIds) {
                AbstractRemote remote = mRemotes.get(keyId);
                remote.registerConnectionMonitor(monitor);
            }
            return;
        }

        AbstractRemote remote = mRemotes.get(remoteType);
        if (remote == null) {
            throw new IllegalArgumentException(String.format("Cannot find Remote for remoteType=%d", remoteType));
        }

        remote.registerConnectionMonitor(monitor);
    }

    /**
     * 注销Remote状态监听器
     * @param remoteType remote长连接, 取值CloudRemote、RouteRemote、LoggerRemote的实例，或REMOTE_ALL
     * @param monitor 待注销的Remote状态监听器
     */
    public void unregisterConnectionMonitor(int remoteType, AbstractRemote.IConnectionMonitor monitor) {
        if (monitor == null) {
            BHLog.i(TAG, "unregisterConnectionMonitor: monitor is null");
            return;
        }

        if (remoteType == REMOTE_ALL) {
            final Set<Integer> keyIds = mRemotes.keySet();
            for (Integer keyId : keyIds) {
                AbstractRemote remote = mRemotes.get(keyId);
                remote.unregisterConnectionMonitor(monitor);
            }
            return;
        }

        AbstractRemote remote = mRemotes.get(remoteType);
        if (remote == null) {
            throw new IllegalArgumentException(String.format("Cannot find Remote for remoteType=%d", remoteType));
        }

        remote.unregisterConnectionMonitor(monitor);
    }

    /**
     * 注册Channel连接状态监听器 @see IEstablishChannelStateMonitor
     * @param remoteType remote长连接, 取值CloudRemote、RouteRemote、LoggerRemote的实例，或REMOTE_ALL
     * @param monitor Remote状态监听器
     */
    public void registerChannelStateMonitor(int remoteType, AbstractRemote.IEstablishChannelStateMonitor monitor) {
        if (monitor == null) {
            BHLog.i(TAG, "registerChannelStateMonitor: monitor is null");
            return;
        }

        if (remoteType == REMOTE_ALL) {
            final Set<Integer> keyIds = mRemotes.keySet();
            for (Integer keyId : keyIds) {
                AbstractRemote remote = mRemotes.get(keyId);
                remote.registerChannelMonitor(monitor);
            }
            return;
        }

        AbstractRemote remote = mRemotes.get(remoteType);
        if (remote == null) {
            throw new IllegalArgumentException(String.format("Cannot find Remote for remoteType=%d", remoteType));
        }

        remote.registerChannelMonitor(monitor);
    }

    /**
     * 注销Channel连接状态监听器
     * @param remoteType remote长连接, 取值CloudRemote、RouteRemote、LoggerRemote的实例，或REMOTE_ALL
     * @param monitor 待注销的Remote状态监听器
     */
    public void unregisterChannelStateMonitor(int remoteType, AbstractRemote.IEstablishChannelStateMonitor monitor) {
        if (monitor == null) {
            BHLog.i(TAG, "unregisterChannelStateMonitor: monitor is null");
            return;
        }

        if (remoteType == REMOTE_ALL) {
            final Set<Integer> keyIds = mRemotes.keySet();
            for (Integer keyId : keyIds) {
                AbstractRemote remote = mRemotes.get(keyId);
                remote.unregisterChannelMonitor(monitor);
            }
            return;
        }

        AbstractRemote remote = mRemotes.get(remoteType);
        if (remote == null) {
            throw new IllegalArgumentException(String.format("Cannot find Remote for remoteType=%d", remoteType));
        }

        remote.unregisterChannelMonitor(monitor);
    }

    /**
     * 注册Remote登陆/注销控制器
     * @param remoteType Remote类型，取值以下常量：REMOTE_CLOUD、REMOTE_ROUTE、REMOTE_LOGGER
     * @param controller 登陆/注销控制器
     */
    public void loadLoginOrLogoutController(Integer remoteType, ILoginOrOutController controller) {
        if (controller == null) {
            BHLog.i(TAG, String.format("registerLoginOrLogoutController: controller is null"));
            return;
        }

        AbstractRemote remote = mRemotes.get(remoteType);

        if (remote == null) {
            BHLog.i(TAG, String.format("registerLoginOrLogoutController: Not found Remote for remoteType=%s", remoteType));
            return;
        }

        remote.loadLoginOrLogoutController(controller);
    }

    /**
     * 注销Remote登陆/注销控制器
     * @param remoteType Remote类型，取值以下常量：REMOTE_CLOUD、REMOTE_ROUTE、REMOTE_LOGGER
     */
    public void unloadLoginOrLogoutController(Integer remoteType) {
        AbstractRemote remote = mRemotes.get(remoteType);

        if (remote == null) {
            BHLog.i(TAG, String.format("unregisterLoginOrLogoutController: Not found Remote for remoteType=%s", remoteType));
            return;
        }

        remote.unloadLoginOrLogoutController();
    }

    /**
     * RemoteProxy是否已初始化
     */
    public boolean hasInit() {
        return hasInit;
    }

    public Properties getNetworkProperties() {
        return mNetworkProperties;
    }

    /**
     * @return 应用实例全局唯一ID (GUID)
     */
    public String getUniqueID() throws RemoteProxyNotInitException {
        return mUniqueID;
    }

    Map<Integer, AbstractRemote> getRemotes() {
        return mRemotes;
    }

    AbstractRemote getRemote(int remoteType) {
        return mRemotes == null ? null : mRemotes.get(remoteType);
    }

    /**
     * 获取长连接的状态
     * @param remoteType Remote类型，取值以下常量：REMOTE_CLOUD、REMOTE_ROUTE、REMOTE_LOGGER
     * @return 指定Remote类型状态
     */
    public AbstractRemote.RemoteStatus getRemoteStatus(Integer remoteType) {
        Map<Integer, AbstractRemote> mapRemotes = getRemotes();
        if (mapRemotes == null || mapRemotes.isEmpty()) {
            BHLog.i(TAG, "getRemoteStatus: remotes is null or empty");
            return null;
        }

        AbstractRemote remote = mapRemotes.get(remoteType);
        if (remote == null) {
            BHLog.i(TAG, String.format("getRemoteStatus: not found remote for remoteType=%d", remoteType));
            return null;
        }

        return remote.getRemoteStatus();
    }

    /**
     * 是否已直连接路由器
     * @return false - Remote未与路由器建立Tcp连接
     *         true - Remote已与路由器建立Tcp连接，但不代表能上外网
     */
    public boolean isRouteConnected() {
        AbstractRemote.RemoteStatus routeRemoteStatus = getRemoteStatus(RemoteProxy.REMOTE_ROUTE);
        return routeRemoteStatus == null ? false : routeRemoteStatus.isConnected();
    }

    /**
     * 是否已登录路由器
     */
    public boolean isRouteLogined() {
        AbstractRemote.RemoteStatus routeRemoteStatus = getRemoteStatus(RemoteProxy.REMOTE_ROUTE);
        return routeRemoteStatus == null ? false : routeRemoteStatus.isLogined();
    }

    /**
     * 是否已连接云端
     */
    public boolean isCloudConnected() {
        AbstractRemote.RemoteStatus cloudRemoteStatus = getRemoteStatus(RemoteProxy.REMOTE_CLOUD);
        return cloudRemoteStatus == null ? false : cloudRemoteStatus.isConnected();
    }

    /**
     * 是否已登录云端
     */
    public boolean isCloudLogined() {
        AbstractRemote.RemoteStatus cloudRemoteStatus = getRemoteStatus(RemoteProxy.REMOTE_CLOUD);
        return cloudRemoteStatus == null ? false : cloudRemoteStatus.isLogined();
    }

    /**
     * 获取当前网络状态
     */
    public int getCurNetworkEnv() {
        return mCurNetworkEnv;
    }

    /**
     * 设置当前网络状态
     * @param curNetworkEnv 取值 NETWORK_UNKNOWN、NETWORK_WIFI、NETWORK_MOBILE、NETWORK_OTHER
     */
    public void setCurNetworkEnv(int curNetworkEnv) {
        mCurNetworkEnv = curNetworkEnv;
    }

    /**
     * 获取网络服务器状态，网络服务器种类如下：<br/>
     * 1) Sec服务器 <br/>
     * 2) LB服务器 <br/>
     * 3) Biz服务器 <br/>
     * 4) 日志服务器 <br/>
     * 等等
     * @return 网络服务器状态
     */
    public ServerStatus getServerStatus() {
        return mServerStatus;
    }

    /**
     * 获取最新的路由器网关地址
     */
    public void updateRouterGatewayIp(Context context) {
        String routerHostIp = "192.168.10.1";
        if (context != null) {
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();

            routerHostIp = intToIp(dhcpInfo.gateway);
            routerHostIp = routerHostIp.equals("0.0.0.0") ? "192.168.10.1" : routerHostIp;
        }

        BHLog.i(TAG, "updateRouterGatewayIp: executed: " + routerHostIp);

        if (mServerStatus == null) {
            BHLog.i(TAG, "updateRouterGatewayIp: mServerStatus is null");
            return;
        }

        Server routerServer = mServerStatus.getRouterServer();
        // 更新路由器地址信息
        routerServer.setHostIpAddress(routerHostIp);
        routerServer.setPort(RemoteProxy.DEFAULT_ROUTER_HOST_PORT);
    }

    private static String intToIp(int paramInt) {
        return String.format("%d.%d.%d.%d", paramInt & 0xFF, 0xFF & paramInt >> 8, 0xFF & paramInt >> 16, 0xFF & paramInt >> 24);
    }

    private boolean isSpecifiedEnv(String specifiedSecServerHostnameOrIp, String specifiedLBServerHostnameOrIp) {
        Server secServer = mServerStatus.getSecServer();
        Server lbServer = mServerStatus.getLBServer();

        String secServerHostnameOrIp = secServer.getHostname();
        String lbServerHostnameOrIp = lbServer.getHostname();

        return specifiedSecServerHostnameOrIp.equals(secServerHostnameOrIp) && specifiedLBServerHostnameOrIp.equals(lbServerHostnameOrIp);
    }

    /**
     * 判断当前环境是否为开发环境
     */
    public boolean isEnvDev() {
        return isSpecifiedEnv(SEC_SERVER_HOST_NAME_DEV, LB_SERVER_HOST_NAME_DEV);
    }

    /**
     * 判断当前环境是否为软件测试环境
     */
    public boolean isEnvSoftTest() {
        return isSpecifiedEnv(SEC_SERVER_HOST_NAME_SOFT_TEST, LB_SERVER_HOST_NAME_SOFT_TEST);
    }

    /**
     * 判断当前环境是否为硬件测试环境
     */
    public boolean isEnvHardTest() {
        return isSpecifiedEnv(SEC_SERVER_HOST_NAME_HARD_TEST, LB_SERVER_HOST_NAME_HARD_TEST);
    }

    /**
     * 判断当前环境是否为老板演示环境
     */
    public boolean isEnvBoss() {
        return isSpecifiedEnv(SEC_SERVER_HOST_NAME_FOR_BOSS, LB_SERVER_HOST_NAME_FOR_BOSS);
    }

    /**
     * 判断当前环境是否为预发布环境
     */
    public boolean isEnvYufabu() {
        return isSpecifiedEnv(SEC_SERVER_HOST_NAME_FOR_YUFABU, LB_SERVER_HOST_NAME_FOR_YUFABU);
    }

    /**
     * 判断当前环境是否为沙箱环境
     */
    public boolean isEnvSandbox() {
        return isSpecifiedEnv(SEC_SERVER_HOST_NAME_FOR_SANDBOX, LB_SERVER_HOST_NAME_FOR_SANDBOX);
    }

    public boolean isEnvJiaju() {
        return isSpecifiedEnv(SEC_SERVER_HOST_NAME_FOR_JIAJU, LB_SERVER_HOST_NAME_FOR_JIAJU);
    }

    /**
     * 检查是不是连接wifi，wifi能不能联网
     * @param context
     * @return 网络状态
     */
    public static NET_WORK_STATE checkNetWorkState(Context context) {
        ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        //检查网络连接
        NetworkInfo info = connectivity.getActiveNetworkInfo();
        if (info == null || !connectivity.getBackgroundDataSetting()) {
            return NET_WORK_STATE.NO_NET_WORK;
        }

        int netType = info.getType();
        switch (netType) {
            case ConnectivityManager.TYPE_WIFI:
                 return dealWifiNetworkState(info);
            case ConnectivityManager.TYPE_MOBILE:
                 return dealMobileNetworkState(info);
            default:
                 return dealOtherNetworkState();
        }

    }

    private static NET_WORK_STATE dealWifiNetworkState(NetworkInfo info) {
        if (info.isConnected()) { //WIFI
            return NET_WORK_STATE.WIFI_CONNECTED;
        } else {
            BHLog.i(TAG, "checkNetWorkState leave for wifi disconnect");
            return NET_WORK_STATE.WIFI_DISCONNECT;
        }
    }

    private static NET_WORK_STATE dealMobileNetworkState(NetworkInfo info) {
        if (info.isConnected()) {
            return NET_WORK_STATE.DATA_CONNECTED;
        } else {
            BHLog.i(TAG, "checkNetWorkState leave for mobilie disconnect");
            return NET_WORK_STATE.NO_NET_WORK;
        }
    }

    private static NET_WORK_STATE dealOtherNetworkState() {
        BHLog.i(TAG, "checkNetWorkState leave for no found network");
        return NET_WORK_STATE.NO_NET_WORK;
    }

    /**
     * 获取RemoteProxy持有的ApplicationContext
     * @return ApplicationContext
     */
    public Context getContext() {
        return mContext;
    }

    /**
     * 获取Handler线程实例
     * @return
     */
    HandlerThread getHandlerThread() {
        return mHandlerThread;
    }

    /**
     * 通知状态机注销远端服务器
     * @param remoteType REMOTE_CLOUD：连接云端，
     *                   REMOTE_ROUTE：连接路由器，
     *                   REMOTE_LOGGER：连接日志服务器，
     *                   REMOTE_ALL: 连接所有服务器，包括但不限于云端、路由器、日志服务器等
     */
    public void logout(int remoteType) {
        if (remoteType == REMOTE_ALL) {
            final Set<Integer> remoteTypeKeys = mRemotes.keySet();
            for (Integer remoteTypeKey : remoteTypeKeys) {
                AbstractRemote remote = mRemotes.get(remoteTypeKey);
                if (remote == null) {
                    continue;
                }

                TCPStateMachine stateMachine = remote.getStateMachine();
                stateMachine.loginOut();
            }

            return;
        }

        AbstractRemote remote = mRemotes.get(remoteType);
        if (remote == null) {
            BHLog.i(TAG, "gtzlogout 11 remoteProxy remote == null,remoteType="+remoteType);

            return;
        }

        TCPStateMachine stateMachine = remote.getStateMachine();
        stateMachine.loginOut();
    }

    /**
     * 网络状态
     */
    public enum NET_WORK_STATE {
        NO_NET_WORK, // 没有网络
        WIFI_CONNECTED, // wifi连接
        WIFI_DISCONNECT, // wifi不能上网
        DATA_CONNECTED // 数据网络连接
    }

    private class TcpConnectionChangedListenerIml implements TcpConnectionChangedListener {

        public final static String TAG = "TcpConnectionChangedListenerIml";

        @Override
        public void onSocketStatusChanged(int socketStatus, String ip, int port) {
            if (mRemotes == null || mRemotes.isEmpty()) {
                BHLog.i(TAG, String.format("TcpConnectionChangedListenerIml: onSocketStatusChanged: mRemotes is null or empty, dest Remote is %s:%s", ip, port));
                return;
            }

            final Set<Integer> keyIds = mRemotes.keySet();
            for (Integer keyId : keyIds) {
                AbstractRemote remote = mRemotes.get(keyId);
                if (remote.isTheSameRemote(ip, String.valueOf(port))) {
                    remote.notifyRemoteStatusChanged(socketStatus);
                    break;
                }
            }
        }
    }

    private static class TcpExceptionListenerImpl implements TcpExceptionListener {

        public final static String TAG = "TcpExceptionListenerImpl";

        private Context mContext;

        public TcpExceptionListenerImpl(Context context) {
            mContext = context;
        }

        @Override
        public void onException(Throwable throwable, String tcpConnectionKey) {
            if (TextUtils.isEmpty(tcpConnectionKey)) { // 未知来源异常, 忽略
                BHLog.i(TAG, String.format("%s comes form unknown source", throwable));
                return;
            }

            Map<Integer, AbstractRemote> remotes = RemoteProxy.getInstance().getRemotes();
            if (remotes == null || remotes.isEmpty()) {
                BHLog.i(TAG, "onException: remotes is null or empty");
                return;
            }

            BHLog.i(TAG, String.format("Channel(%s) occurs exception: %s", tcpConnectionKey, throwable.getMessage()), throwable);

            final Set<Integer> setKeyId = new HashSet<Integer>();
            for (Integer keyId : setKeyId) {
                AbstractRemote remote = remotes.get(keyId);
                String[] addressParts = tcpConnectionKey.split(":");
                if (remote.isTheSameRemote(addressParts[0], addressParts[1])) {
                    if (throwable instanceof SocketException
                            || throwable instanceof SocketTimeoutException) {
                        BHLog.i(TAG, String.format("onException: Remote(%s) occurs exception(%s), and try reconnecting", remote, throwable.getMessage()));

                        if (!NetworkUtil.isNetworkAvailable(mContext)) {
                            BHLog.i(TAG, "onException: Network unavailable");
                            return;
                        }

                        TCPStateMachine stateMachine = remote.getStateMachine();
                        // 重连网络
                        stateMachine.switchState(TCPStateMachine.TCP_STATE_DISCONNECTED);
                        stateMachine.connect();
                    }
                    return;
                }
            }
        }
    }

    public class ServerStatus {

        public final static String TAG = "ServerStatus";

        /**
         * 部署环境
         */
        private String mEnv;
        /**
         * 路由器服务器
         */
        private Server mRouterServer;
        /**
         * 云端服务器
         */
        private Server mCloudServer;
        /**
         * 安全服务器
         */
        private Server mSecServer;
        /**
         * LB服务器
         */
        private Server mLBServer;
        /**
         * 日志服务器
         */
        private Server mLogServer;

        /**
         * 初始化网络状态
         * @param env 取值参考ENV_RELEASE、ENV_BOSS、ENV_DEV、ENV_SOFT_TEST、ENV_HARD_TEST、ENV_YUFABU、ENV_SANDBOX
         */
        public ServerStatus(String env) {
            mSecServer = new Server();
            mLBServer = new Server();
            mCloudServer = new Server();
            mRouterServer = new Server();
            mLogServer = new Server();
            // 注册连接状态监听器
            TcpClient tcpClient = TcpClient.getInstance();

            tcpClient.registerTcpConnectionChangedListener(mCloudServer);
            tcpClient.registerTcpConnectionChangedListener(mRouterServer);
            tcpClient.registerTcpConnectionChangedListener(mLogServer);
            // 设置部署环境
            setEnv(env);
        }

        /**
         * 设置部署环境
         * @param env 取值参考ENV_RELEASE、ENV_BOSS、ENV_DEV、ENV_SOFT_TEST、ENV_HARD_TEST、ENV_YUFABU、ENV_SANDBOX
         */
        public void setEnv(String env) {
            mEnv = env;
            // 访问配置文件，获取服务器信息
            if (!configEnvWithConfigFile()) {
                configEnvWithPreconfig(env); // 从文件获取配置失败，直接从硬编码获取服务器信息
            }

            BHLog.i(TAG, String.format("setEnv: secServerUrl=%s, secServerPort=%d", mSecServer.getHostname(), mSecServer.getPort()));
            BHLog.i(TAG, String.format("setEnv: lbServerUrl=%s, lbServerPort=%d", mLBServer.getHostname(), mLBServer.getPort()));

            TcpClient tcpClient = TcpClient.getInstance();
            tcpClient.updateConnectionParams(TcpClient.MAP_KEY_SEC_SERVER_CONNECTION, mSecServer.getHostIpAddress(), mSecServer.getPort(), "", "", "");
            tcpClient.updateConnectionParams(TcpClient.MAP_KEY_LB_SERVER_CONNECTION, mLBServer.getHostIpAddress(), mLBServer.getPort(), "", "", "");
        }

        private boolean configEnvWithConfigFile() {
            final Properties props = mNetworkProperties;
            if (props == null || props.isEmpty()) {
                BHLog.i(TAG, "configEnvWithConfigFile: props is null or empty");
                return false;
            }
            // Sec服务器地址
            String secServerUrl = props.getProperty(NetworkConfigLoader.PROPERTY_KEY_SEC_SERVER_URL);
            String secServerPort = props.getProperty(NetworkConfigLoader.PROPERTY_KEY_SEC_SERVER_PORT);
            // LB服务器地址
            String lbServerUrl = props.getProperty(NetworkConfigLoader.PROPERTY_KEY_LB_SERVER_URL);
            String lbServerPort = props.getProperty(NetworkConfigLoader.PROPERTY_KEY_LB_SERVER_PORT);

            if (TextUtils.isEmpty(secServerUrl) || TextUtils.isEmpty(lbServerUrl)) {
                BHLog.i(TAG, "configEnvWithConfigFile: Config file invalid");
                return false;
            }

            mSecServer.setHostnameAndAutoGetHostIp(secServerUrl, true);
            try {
                mSecServer.setPort(Integer.parseInt(secServerPort));
            } catch (Exception ex) {
                BHLog.i(TAG, ex.getMessage(), ex);
                mSecServer.setPort(RemoteProxy.SEC_SERVER_HOST_PORT);
            }

            mLBServer.setHostnameAndAutoGetHostIp(lbServerUrl, true);
            try {
                mLBServer.setPort(Integer.parseInt(lbServerPort));
            } catch (Exception ex) {
                BHLog.i(TAG, ex.getMessage(), ex);
                mLBServer.setPort(RemoteProxy.LB_SERVER_HOST_PORT);
            }

            return true;
        }

        private void configEnvWithPreconfig(String env) {
            BHLog.i(TAG, String.format("configEnvWithPreconfig: env=%s", env));
            // 根据部署环境选择Sec、LB服务器地址
            switch (env) {
                case ENV_BOSS:
                case ENV_RELEASE:
                     mSecServer.setHostnameAndAutoGetHostIp(SEC_SERVER_HOST_NAME_FOR_BOSS, true);
                     mLBServer.setHostnameAndAutoGetHostIp(LB_SERVER_HOST_NAME_FOR_BOSS, true);
                     break;
                case ENV_DEV:
                     mSecServer.setHostnameAndAutoGetHostIp(SEC_SERVER_HOST_NAME_DEV, true);
                     mLBServer.setHostnameAndAutoGetHostIp(LB_SERVER_HOST_NAME_DEV, true);
                     break;
                case ENV_HARD_TEST:
                     mSecServer.setHostnameAndAutoGetHostIp(SEC_SERVER_HOST_NAME_HARD_TEST, true);
                     mLBServer.setHostnameAndAutoGetHostIp(LB_SERVER_HOST_NAME_HARD_TEST, true);
                     break;
                case ENV_SOFT_TEST:
                     mSecServer.setHostnameAndAutoGetHostIp(SEC_SERVER_HOST_NAME_SOFT_TEST, true);
                     mLBServer.setHostnameAndAutoGetHostIp(LB_SERVER_HOST_NAME_SOFT_TEST, true);
                     break;
                case ENV_YUFABU:
                     mSecServer.setHostnameAndAutoGetHostIp(SEC_SERVER_HOST_NAME_FOR_YUFABU, true);
                     mLBServer.setHostnameAndAutoGetHostIp(LB_SERVER_HOST_NAME_FOR_YUFABU, true);
                     break;
                case ENV_SANDBOX:
                     mSecServer.setHostnameAndAutoGetHostIp(SEC_SERVER_HOST_NAME_FOR_SANDBOX, true);
                     mLBServer.setHostnameAndAutoGetHostIp(LB_SERVER_HOST_NAME_FOR_SANDBOX, true);
                     break;
                default:
                     mSecServer.setHostnameAndAutoGetHostIp(SEC_SERVER_HOST_NAME_SOFT_TEST, true);
                     mLBServer.setHostnameAndAutoGetHostIp(LB_SERVER_HOST_NAME_SOFT_TEST, true);
            }
            // 设置Sec、LB服务器端口
            mSecServer.setPort(SEC_SERVER_HOST_PORT);
            mLBServer.setPort(LB_SERVER_HOST_PORT);

            mRouterServer.setPort(DEFAULT_ROUTER_HOST_PORT);
        }

        /**
         * 获取部署环境
         */
        public String getEnv() {
            return mEnv;
        }

        /**
         * 获取路由器Http服务器地址
         */
        public String getRouterHttpServerUrl() {
            return "http://" + mRouterServer.getHostIpAddress() + "/cgi-bin/test1";
        }

        public Server getRouterServer() {
            return mRouterServer;
        }

        public Server getCloudServer() {
            return mCloudServer;
        }

        public Server getSecServer() {
            return mSecServer;
        }

        public Server getLBServer() {
            return mLBServer;
        }

        public Server getLogServer() {
            return mLogServer;
        }

        /**
         * 释放资源
         */
        public void release() {
            // 注销连接状态监听器
            TcpClient tcpClient = TcpClient.getInstance();
            tcpClient.unregisterTcpConnectionChangedListener(mCloudServer);
            tcpClient.unregisterTcpConnectionChangedListener(mRouterServer);
        }

        private AbstractRemote getRemote(int remoteType) {
            AbstractRemote remote = mRemotes.get(remoteType);
            if (remote == null) {
                BHLog.i(TAG, String.format("adjustRemoteStateMachine: not found Remote for remoteType=%d", remoteType));
                return null;
            }

            return remote;
        }

        /**
         * 标记云端服务器为断开状态
         * @param cascadeToStatemachine 是否级联更新状态机
         *                              false - 只更新云端服务器状态，不级联更新状态机
         *                              true - 更新云端服务器状态，并级联更新状态机
         */
        public void markCloudServerDisconnected(boolean cascadeToStatemachine) {
            mCloudServer.setOnline(false);

            if (cascadeToStatemachine) {
                AbstractRemote remote = getRemote(REMOTE_CLOUD);
                if (remote != null) {
                    TCPStateMachine stateMachine = remote.getStateMachine();
                    stateMachine.disconnect();
                    stateMachine.sendEvent(TCPStateMachine.EVENT_CMD_DISCONNECT_SUCCESS, null);
                }
            }
        }

        /**
         * 标记云端服务器为登录状态
         * @param cascadeToStatemachine 是否级联更新状态机
         *                              false - 只更新云端服务器状态，不级联更新状态机
         *                              true - 更新云端服务器状态，并级联更新状态机
         */
        public void markCloudServerLogin(boolean cascadeToStatemachine) {
            mCloudServer.setLogin(true);

            if (cascadeToStatemachine) {
                AbstractRemote remote = getRemote(REMOTE_CLOUD);
                if (remote != null) {
                    TCPStateMachine stateMachine = remote.getStateMachine();
                    stateMachine.login();
                    stateMachine.sendEvent(TCPStateMachine.EVENT_CMD_LOGIN_SUCCESS, null);
                }
            }
        }

        /**
         * 标记云端服务器为登出状态
         * @param cascadeToStatemachine 是否级联更新状态机
         *                              false - 只更新云端服务器状态，不级联更新状态机
         *                              true - 更新云端服务器状态，并级联更新状态机
         */
        public void markCloudServerLogout(boolean cascadeToStatemachine) {
            mCloudServer.setLogin(false);

            if (cascadeToStatemachine) {
                AbstractRemote remote = getRemote(REMOTE_CLOUD);
                if (remote != null) {
                    TCPStateMachine stateMachine = remote.getStateMachine();
                    stateMachine.loginOut();
                    stateMachine.sendEvent(TCPStateMachine.EVENT_CMD_LOGOUT_SUCCESS, null);
                }
            }
        }

        /**
         * 标记路由器为断开状态
         * @param cascadeToStatemachine 是否级联更新状态机
         *                              false - 只更新云端服务器状态，不级联更新状态机
         *                              true - 更新云端服务器状态，并级联更新状态机
         */
        public void markRouteServerDisconnected(boolean cascadeToStatemachine) {
            mRouterServer.setOnline(false);

            if (cascadeToStatemachine) {
                AbstractRemote remote = getRemote(REMOTE_ROUTE);
                if (remote != null) {
                    TCPStateMachine stateMachine = remote.getStateMachine();
                    stateMachine.disconnect();
                    stateMachine.sendEvent(TCPStateMachine.EVENT_CMD_DISCONNECT_SUCCESS, null);
                }
            }
        }

        /**
         * 标记路由器为登录状态
         * @param cascadeToStatemachine 是否级联更新状态机
         *                              false - 只更新云端服务器状态，不级联更新状态机
         *                              true - 更新云端服务器状态，并级联更新状态机
         */
        public void markRouteServerLogin(boolean cascadeToStatemachine) {
            mRouterServer.setLogin(true);

            if (cascadeToStatemachine) {
                AbstractRemote remote = getRemote(REMOTE_ROUTE);
                if (remote != null) {
                    TCPStateMachine stateMachine = remote.getStateMachine();
                    stateMachine.login();
                    stateMachine.sendEvent(TCPStateMachine.EVENT_CMD_LOGIN_SUCCESS, null);
                }
            }
        }

        /**
         * 标记路由器为登出状态
         * @param cascadeToStatemachine 是否级联更新状态机
         *                              false - 只更新云端服务器状态，不级联更新状态机
         *                              true - 更新云端服务器状态，并级联更新状态机
         */
        public void markRouteServerLogout(boolean cascadeToStatemachine) {
            mRouterServer.setLogin(false);

            if (cascadeToStatemachine) {
                AbstractRemote remote = getRemote(REMOTE_ROUTE);
                if (remote != null) {
                    TCPStateMachine stateMachine = remote.getStateMachine();
                    stateMachine.loginOut();
                    stateMachine.sendEvent(TCPStateMachine.EVENT_CMD_LOGOUT_SUCCESS, null);
                }
            }
        }

        /**
         * 标记日志服务器为断开状态
         * @param cascadeToStatemachine 是否级联更新状态机
         *                              false - 只更新云端服务器状态，不级联更新状态机
         *                              true - 更新云端服务器状态，并级联更新状态机
         */
        public void markLoggerServerDisconnected(boolean cascadeToStatemachine) {
            mLogServer.setOnline(false);

            if (cascadeToStatemachine) {
                AbstractRemote remote = getRemote(REMOTE_LOGGER);
                if (remote != null) {
                    TCPStateMachine stateMachine = remote.getStateMachine();
                    stateMachine.disconnect();
                    stateMachine.sendEvent(TCPStateMachine.EVENT_CMD_DISCONNECT_SUCCESS, null);
                }
            }
        }

        /**
         * 标记日志服务器为登录状态
         * @param cascadeToStatemachine 是否级联更新状态机
         *                              false - 只更新云端服务器状态，不级联更新状态机
         *                              true - 更新云端服务器状态，并级联更新状态机
         */
        public void markLoggerServerLogin(boolean cascadeToStatemachine) {
            mLogServer.setLogin(true);

            if (cascadeToStatemachine) {
                AbstractRemote remote = getRemote(REMOTE_LOGGER);
                if (remote != null) {
                    TCPStateMachine stateMachine = remote.getStateMachine();
                    stateMachine.login();
                    stateMachine.sendEvent(TCPStateMachine.EVENT_CMD_LOGIN_SUCCESS, null);
                }
            }
        }

        /**
         * 标记日志服务器为登出状态
         * @param cascadeToStatemachine 是否级联更新状态机
         *                              false - 只更新云端服务器状态，不级联更新状态机
         *                              true - 更新云端服务器状态，并级联更新状态机
         */
        public void markLoggerServerLogout(boolean cascadeToStatemachine) {
            mLogServer.setLogin(false);

            if (cascadeToStatemachine) {
                AbstractRemote remote = getRemote(REMOTE_LOGGER);
                if (remote != null) {
                    TCPStateMachine stateMachine = remote.getStateMachine();
                    stateMachine.loginOut();
                    stateMachine.sendEvent(TCPStateMachine.EVENT_CMD_LOGOUT_SUCCESS, null);
                }
            }
        }
    }

    /**
     * 远程服务器抽象，有域名、IP地址、连接状态、登陆状态，可代表任何服务器对象，如安全服务器、负载均衡服务器、Biz服务器等
     */
    public class Server implements TcpConnectionChangedListener {

        public final static String TAG = "Server";
        /**
         * 服务器域名
         */
        private volatile String mHostname;
        /**
         * 服务器IP地址
         */
        private volatile String mHostIpAddress;
        /**
         * 服务器端口
         */
        private volatile int mPort;
        /**
         * 是否在线
         */
        private volatile boolean mOnline;
        /**
         * 是否登陆
         */
        private volatile boolean mLogin;
        /**
         * 接入流程状态
         */
        private volatile boolean mCheckInStatus;

        public Server() {
            mHostIpAddress = "0.0.0.0";
            mPort = -1;
        }

        /**
         * Server构造函数
         * @param hostname 主机域名
         * @param hostIpAddress 主机IP地址
         * @param port 端口
         */
        public Server(String hostname, String hostIpAddress, int port) {
            mHostname = hostname;
            mHostIpAddress = hostIpAddress;
            mPort = port;
        }

        public String getHostname() {
            return mHostname;
        }

        /**
         * 设置服务器域名，并自动将域名转换为IP地址
         * @param hostname 服务器域名
         * @param transform2IpAynsc true - 异步调用转换服务器域名为IP地址
         *                          false - 同步调用转换服务器域名为IP地址
         */
        public void setHostnameAndAutoGetHostIp(String hostname, boolean transform2IpAynsc) {
            mHostname = hostname;
            if (transform2IpAynsc) {
                transformHostname2IpAddressASync(hostname, 2000);
            } else {
                transformHostname2IpAddressSync(hostname);
            }
        }

        /**
         * 同步转化Host域名为IP地址<br/>
         * 注意，此调用可能导致阻塞
         * @param hostname 主机域名
         * @return 主机域名对应的IP地址
         */
        public String transformHostname2IpAddressSync(String hostname) {
            try { //域名解析为IP
                mHostIpAddress = InetAddress.getByName(hostname).getHostAddress();
            } catch (Throwable throwable) {
                BHLog.i(TAG, throwable.getMessage(), throwable);
            }

            return mHostIpAddress;
        }

        /**
         * 异步转化Host域名为IP地址
         * @param hostname 主机域名
         * @param timeout 异步获取主机IP地址线程的超时时间, 单位: ms
         */
        public void transformHostname2IpAddressASync(final String hostname, final long timeout) {
            Thread thread = new Thread() {
                public void run() {
                    try { //域名解析为IP
                        mHostIpAddress = InetAddress.getByName(hostname).getHostAddress();
                    } catch (Throwable throwable) {
                        BHLog.i(TAG, throwable.getMessage(), throwable);
                    }
                }
            };

            try {
                thread.start();
                thread.join(timeout);
            } catch (Throwable throwable) {
                BHLog.i(TAG, throwable.getMessage(), throwable);
            }

        }

        /**
         * 是否有有效的IP地址
         */
        public boolean hasValidIpAddress() {
            if (TextUtils.isEmpty(mHostIpAddress)) {
                return false;
            }

            if (mHostIpAddress.equals("0.0.0.0") || mPort < 0) {
                return false;
            }

            return true;
        }

        public String getHostIpAddress() {
            return mHostIpAddress;
        }

        public void setHostIpAddress(String hostIpAddress) {
            mHostIpAddress = hostIpAddress;
        }

        public int getPort() {
            return mPort;
        }

        public void setPort(int port) {
            mPort = port;
        }

        public boolean isOnline() {
            return mOnline;
        }

        public void setOnline(boolean online) {
            mOnline = online;
        }

        public boolean isLogin() {
            return mLogin;
        }

        public void setLogin(boolean login) {
            mLogin = login;
        }

        public boolean isCheckInStatus() {
            return mCheckInStatus;
        }

        public void setCheckInStatus(boolean checkInStatus) {
            mCheckInStatus = checkInStatus;
        }

        @Override
        public void onSocketStatusChanged(int socketStatus, String ip, int port) {
            if (TextUtils.isEmpty(mHostIpAddress) || TextUtils.isEmpty(ip)) { // 当前IP地址为空或传参ip为空，则忽略
                return;
            }

            if (!mHostIpAddress.equals(ip) || mPort != port) {
                return;
            }

            if (socketStatus == AbstractRemote.IConnectionMonitor.SOCKET_CONNECTION_CONNECTED) {
                mOnline = true;
            } else {
                mOnline = false;
            }

            BHLog.i(TAG, String.format("onSocketStatusChanged: mHostIpAddress(%s):mPort(%s) online=%s", mHostIpAddress, mPort, mOnline));
        }
    }

    // 由于历史原因, sendJson被上层业务大量使用, 且使用大多不规范, 本期重构暂不优化sendJson, 后期重构将对sendJson进行优化
    // ----------------------------------------- sendJson code snippet start ----------------------------------------------
    /**
     * 直接请求json
     *
     * @param contentJson 封装到content那一层的json串
     * @param timeout     消息等待超时时间
     * @param isImmediate 是否紧急即刻发送
     * @param bUniCast   mdp请求后云端是否只对请求端回复
     */
    @Deprecated
    public void sendJson(NetworkMsg.MSG_TYPE msgType, final int req_id, final String reqMethod, String contentJson,
                                int timeout, boolean isImmediate, boolean bUniCast, final ApiSendJsonCallback callback, AbstractRouteStrategy strategy) {
        boolean needEncrypt = false;
        boolean needMdp = false;
        TcpClient tcpClient = TcpClient.getInstance();

        if (strategy == null) {
            strategy = new DefaultStrategy(RemoteProxy.getInstance().getServerStatus());
        }

        AbstractRouteStrategy.RouteStrategyParams params = new AbstractRouteStrategy.RouteStrategyParams();
        params.setRequestMethod(reqMethod);

        AbstractRouteStrategy.RouteStrategyResult strategyResult = null;
        if (msgType == NetworkMsg.MSG_TYPE.TYPE_INVALID) {
            try {
                //根据方法名决定走哪条通道
                strategyResult = strategy.selectRemoteTarget(params);
                msgType = AbstractRouteStrategy.getMsgType(strategyResult.getTarget());
            } catch (Throwable throwable) {
                BHLog.i(TAG, String.format("sendJson: %s", throwable.getMessage()), throwable);
                callback.onError(ErrorCode.ERROR_CODE_91006);

                return;
            }

            if (msgType == NetworkMsg.MSG_TYPE.TYPE_INVALID) {
                tryReconnectRemoteIfNeeded(null, strategyResult);
            }
        }

        int dest = AbstractRouteStrategy.TARGET_INVALID;
        if (msgType == NetworkMsg.MSG_TYPE.TYPE_REQ_CLOUD || msgType == NetworkMsg.MSG_TYPE.TYPE_REQ_CLOUD_WITHOUT_RES) {
            needEncrypt = true;
            dest = AbstractRouteStrategy.TARGET_CLOUD;
        } else if (msgType == NetworkMsg.MSG_TYPE.TYPE_REQ_CLOUD_TRANS) {
            needEncrypt = true;
            needMdp = true;
            dest = AbstractRouteStrategy.TARGET_CLOUD;
        } else if (msgType == NetworkMsg.MSG_TYPE.TYPE_REQ_ROUTER || msgType == NetworkMsg.MSG_TYPE.TYPE_REQ_ROUTER_WITHOUT_RES) {
            needEncrypt = true;
            try {
                Properties properties = RemoteProxy.getInstance().getNetworkProperties();
                String propValue = properties.getProperty(PROPERTY_KEY_ENABLE_ROUTER_CHANNEL_ENCRYPT);

                needEncrypt = Boolean.parseBoolean(propValue);
            } catch (Exception ex) {
                BHLog.i(TAG, ex.getMessage());
            }

            dest = AbstractRouteStrategy.TARGET_ROUTER;
        } else if (msgType == NetworkMsg.MSG_TYPE.TYPE_REQ_LOGGER_WITHOUT_RES) {
            needEncrypt = false;
            dest = AbstractRouteStrategy.TARGET_LOGGER_NOT_WAITING_RESPONSE;
        } else if (msgType == NetworkMsg.MSG_TYPE.TYPE_INVALID) {
            BHLog.e(TAG, "request fail，can't pick a SocketChannel", false);
            if (callback != null && strategyResult != null) {
                int errCode = strategyResult.getErrorCode();
                callback.onError(errCode == 0 ? ErrorCode.ERROR_CODE_91006 : errCode);
            }

            return;
        }

        //加mdp
        if (needMdp) {
            long target_id = RouterRemote.getCurrentRouterId();
            if(bUniCast){
                contentJson = ProtocolUtil.addUniCastMdpHeader(NetworkMsg.MDP_MSG_TYPE.TYPE_P2R,
                        target_id, contentJson);
            }else{
                contentJson = ProtocolUtil.addMdpHeader(NetworkMsg.MDP_MSG_TYPE.TYPE_P2R,
                        target_id, contentJson);
            }
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
            case AbstractRouteStrategy.TARGET_LOGGER_NOT_WAITING_RESPONSE:
                 destStr = "logger";
                 break;
            default:
        }

        BHLog.i(TAG, String.format("[%s<-app] %s", destStr, contentJson));

        contentJson = ProtocolUtil.addSaferHeader(aesKey, aesIv, contentJson, needEncrypt) + "\n";

        final NetworkMsg networkMsg = new NetworkMsg(req_id, reqMethod, msgType, contentJson);

        final Handler handler = new Handler(Looper.getMainLooper());

        if (timeout == RequestParams.DEFAULT_NETWORKMSG_TIMEOUT) {
            timeout = strategy.decideMsgTimeout(reqMethod);
        }

        tcpClient.sendMsg(req_id, networkMsg, timeout, isImmediate, new TcpCallback() {
            @Override
            public void onSuccess(Object data) {

                final String response = (String) data;

                if (callback instanceof ApiSendJsonCallback) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
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
                BHLog.e(TAG, String.format("sendJson: request fail, req_id=%d errorCode=%d", req_id, errorCode), false);

                if (callback instanceof ApiSendJsonCallback) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError(errorCode);
                        }
                    });
                }
            }
        });

        return;
    }
    // ----------------------------------------- sendJson code snippet end ----------------------------------------------

}
