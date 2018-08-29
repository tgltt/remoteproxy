/**
 *****************************************************************************
 * Copyright (C) 2018-2023 HD Corporation. All rights reserved
 * File        : ConnectRouteRemoteAction.java
 *
 * Description : ConnectRouteRemoteAction,
 *
 * Creation    : 2018-05-29
 * Author      : tanguilong@evergrande.cn
 * History     : 2018-05-29, Creation
 *****************************************************************************
 */
package cn.evergrande.it.hdremoteproxylib.network.data.actions;

import com.google.gson.Gson;

import java.util.Properties;

import cn.evergrande.it.hdnetworklib.api.biz.GsonUtil;
import cn.evergrande.it.hdnetworklib.api.model.protocal.Protocal;
import cn.evergrande.it.hdnetworklib.network.common.biz.ProtocolUtil;
import cn.evergrande.it.hdnetworklib.network.common.model.NetworkMsg;
import cn.evergrande.it.hdnetworklib.network.common.model.protocal.CommonContentReq;
import cn.evergrande.it.hdnetworklib.network.tcp.TcpCallback;
import cn.evergrande.it.hdnetworklib.network.tcp.TcpClient;
import cn.evergrande.it.hdremoteproxylib.AbstractRemote;
import cn.evergrande.it.hdremoteproxylib.AbstractRemote.IEstablishChannelStateMonitor;
import cn.evergrande.it.hdremoteproxylib.HdNetworkMonitorService;
import cn.evergrande.it.hdremoteproxylib.RemoteProxy;
import cn.evergrande.it.hdremoteproxylib.RouterRemote;
import cn.evergrande.it.hdremoteproxylib.exceptions.RemoteProxyNotInitException;
import cn.evergrande.it.hdremoteproxylib.interfaces.ICheckInCallback;
import cn.evergrande.it.hdremoteproxylib.network.data.beans.CreateSecurityChannelParamsBean;
import cn.evergrande.it.hdremoteproxylib.network.request.RequestParams;
import cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine;
import cn.evergrande.it.logger.BHLog;

import static cn.evergrande.it.hdremoteproxylib.network.NetworkConfigLoader.PROPERTY_KEY_ENABLE_ROUTER_CHANNEL_ENCRYPT;

public class ConnectRouteRemoteAction extends RetryConnectRemoteAction {

    public final static String TAG = "ConnectRouteRemoteAction";

    private String mRouterIpAddress;
    private int mRouterPort;

    public ConnectRouteRemoteAction(int actionId, int actionSubId, Object[] args) {
        super(actionId, actionSubId, args);
    }

    @Override
    public void run() {
        if (!checkAndParseParams(mActionArgs)) {
            BHLog.i(TAG, "run: checkAndParseParams failed");
            notifyCheckinStage(RemoteProxy.REMOTE_ROUTE, mActionArgs, IEstablishChannelStateMonitor.CHANNEL_STAGE_FAIL);

            return;
        }

        final RouterRemote routerRemote = (RouterRemote) mActionArgs[0];
        final String routerIp = (String) mActionArgs[3];
        final Integer routerPort = (Integer) mActionArgs[4];

        mRouterIpAddress = routerIp;
        mRouterPort = routerPort;

        BHLog.i(TAG, String.format("run: routeRemote(%s:%s)", routerIp, routerPort));
        // 设置Remote的地址及端口
        routerRemote.setRemoteAddressPort(routerIp, routerPort.toString());
        TcpClient.getInstance().updateConnectionParams(TcpClient.MAP_KEY_ROUTE_CONNECTION, routerIp, routerPort, RouterRemote.IOT_PUB_KEY, RouterRemote.AES_KEY, RouterRemote.AES_IV);

        ICheckInCallback callback = new ICheckInCallback() {
            @Override
            public void onSuccess(ICheckInCallback.CheckinResult checkinResult) {
                if (checkinResult == null) {
                    BHLog.i(TAG, "run: onSuccess: can't get routeRemote address");

                    retryConnectRemote(routerRemote);
                    return;
                }

                // 设置状态机状态为Connected
                TCPStateMachine stateMachine = routerRemote.getStateMachine();
                stateMachine.sendEvent(TCPStateMachine.EVENT_CMD_CONNECT_SUCCESS, null);

                boolean needEncrypt = true;
                try {
                    Properties properties = RemoteProxy.getInstance().getNetworkProperties();
                    String propValue = properties.getProperty(PROPERTY_KEY_ENABLE_ROUTER_CHANNEL_ENCRYPT);

                    needEncrypt = Boolean.parseBoolean(propValue);
                } catch (Exception ex) {
                    BHLog.i(TAG, ex.getMessage());
                }

                if (needEncrypt) { // 需要交换密钥
                    exchangeAppSecKey(routerRemote);
                } else {
                    stateMachine.login();

                    BHLog.i(TAG, "run:onSuccess: invokeTcpConnectionHeartbeat");
                    // 启动心跳
                    HdNetworkMonitorService.getInstance().invokeTcpConnectionHeartbeat();
                }
            }

            @Override
            public void onError(int errorCode) {
                BHLog.i(TAG, String.format("run: onError: errorCode=%s", errorCode));
                retryConnectRemote(routerRemote);
            }
        };

        BHLog.i(TAG, "run: routeRemote connecting..");

        Object[] connectParams = new Object[] {callback, routerIp, routerPort};
        routerRemote.connect(connectParams);
    }

    protected boolean checkAndParseParams(Object[] paramArgs) {
        if (!mValid) {
            BHLog.i(TAG, "checkAndParseParams: Action not valid");
            return false;
        }

        final int expectedCount = 5;
        if (mActionArgs.length < expectedCount) {
            BHLog.i(TAG, String.format("checkAndParseParams: Params not enough(expectd=%d, actual=%d)", expectedCount, mActionArgs.length));
            return false;
        }

        if (!(mActionArgs[0] instanceof RouterRemote)) {
            BHLog.i(TAG, String.format("checkAndParseParams: mActionArgs[0](%s) is not RouterRemote", mActionArgs[0]));
            return false;
        }

        if (!(mActionArgs[3] instanceof String)) {
            BHLog.i(TAG, String.format("checkAndParseParams: mActionArgs[3](%s) is not String", mActionArgs[3]));
            return false;
        }

        if (!(mActionArgs[4] instanceof Integer)) {
            BHLog.i(TAG, String.format("checkAndParseParams: mActionArgs[4](%s) is not Integer", mActionArgs[4]));
            return false;
        }

        return true;
    }

    @Override
    protected void retryConnectRemote(AbstractRemote remote) {
        RemoteProxy remoteProxy = RemoteProxy.getInstance();
        remoteProxy.updateRouterGatewayIp(remoteProxy.getContext());
        // 刷新路由器地址, 在重试的过程中, 网络可能会发生变化，比如从AP1切换到AP2等
        try {
            RemoteProxy.ServerStatus serverStatus = RemoteProxy.getInstance().getServerStatus();
            RemoteProxy.Server routerServer = serverStatus.getRouterServer();

            mActionArgs[3] = routerServer.getHostIpAddress();
            mActionArgs[4] = routerServer.getPort();
        } catch (Throwable throwable) {
            BHLog.i(TAG, throwable.getMessage(), throwable);
        }

        super.retryConnectRemote(remote);
    }

    private void exchangeAppSecKey(final AbstractRemote remote) {
        BHLog.i(TAG, "exchangeAppSecKey");

        RouterRemote.refreshAesKey();
        TcpClient.getInstance().updateConnectionParams(TcpClient.MAP_KEY_ROUTE_CONNECTION, mRouterIpAddress, mRouterPort, RouterRemote.IOT_PUB_KEY, RouterRemote.AES_KEY, RouterRemote.AES_IV);
        // 设置密钥交换参数
        CommonContentReq commonContentReq = new CommonContentReq();
        commonContentReq.setMethod(Protocal.ROUTER_REQ_METHODS_EXCHANGE_APP_SEC_KEY);

        CreateSecurityChannelParamsBean paramsBean = new CreateSecurityChannelParamsBean();
        try {
            RemoteProxy remoteProxy = RemoteProxy.getInstance();
            paramsBean.setUuid(remoteProxy.getUniqueID());
            paramsBean.setKey(RouterRemote.AES_KEY);
        } catch (RemoteProxyNotInitException rpniex) {
            BHLog.i(TAG, rpniex.getMessage(), rpniex);
            return;
        }

        commonContentReq.setParams(paramsBean);

        String reqDataStr = new Gson().toJson(commonContentReq);
        // 创建安全通道需要使用RSA加密业务数据
        StringBuilder sbReqData = new StringBuilder();
        sbReqData.append(ProtocolUtil.encryptWithIOTPubKey(RouterRemote.IOT_PUB_KEY, reqDataStr)).append("\n");

        NetworkMsg networkMsg = new NetworkMsg(commonContentReq.getReq_id(), commonContentReq.getMethod(), NetworkMsg.MSG_TYPE.TYPE_REQ_ROUTER, sbReqData.toString());

        final TcpClient tcpClient = TcpClient.getInstance();
        tcpClient.sendMsg(commonContentReq.getReq_id(), networkMsg, RequestParams.SHORT_NETWORKMSG_TIMEOUT, false, new TcpCallback() {
            @Override
            public void onSuccess(Object data) {
                int code = GsonUtil.str2JsonObj((String) data).get(Protocal.PARAMS_KEY_COMNON_CODE).getAsInt();
                if (code == 0) {
                    BHLog.i(TAG, String.format("exchangeAppSecKey: onSuccess success(aesKey=%s, aesIv=%s)", RouterRemote.AES_KEY, RouterRemote.AES_IV));

                    TCPStateMachine stateMachine = remote.getStateMachine();
                    stateMachine.login();

                    BHLog.i(TAG, "exchangeAppSecKey:onSuccess: invokeTcpConnectionHeartbeat");
                    // 启动心跳
                    HdNetworkMonitorService.getInstance().invokeTcpConnectionHeartbeat();
                } else {
                    BHLog.i(TAG, String.format("exchangeAppSecKey: onSuccess but fail, code=%d", code));
                }
            }

            @Override
            public void onError(int errorCode) {
                BHLog.i(TAG, String.format("exchangeAppSecKey: onError but fail, code=%d", errorCode));
            }
        });
    }
}
