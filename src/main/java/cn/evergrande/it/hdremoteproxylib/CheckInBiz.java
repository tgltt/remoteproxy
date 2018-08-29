package cn.evergrande.it.hdremoteproxylib;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import cn.evergrande.it.hdnetworklib.api.biz.GsonUtil;
import cn.evergrande.it.hdnetworklib.network.common.biz.ProtocolUtil;
import cn.evergrande.it.hdnetworklib.api.model.protocal.ErrorCode;
import cn.evergrande.it.hdnetworklib.api.model.protocal.Protocal;
import cn.evergrande.it.hdnetworklib.network.common.model.NetworkMsg;
import cn.evergrande.it.hdnetworklib.network.common.model.protocal.CommonContentReq;
import cn.evergrande.it.hdnetworklib.network.tcp.TcpCallback;
import cn.evergrande.it.hdnetworklib.network.tcp.TcpClient;
import cn.evergrande.it.hdnetworklib.network.tcp.TcpConnection;
import cn.evergrande.it.hdremoteproxylib.AbstractRemote.IEstablishChannelStateMonitor;
import cn.evergrande.it.hdremoteproxylib.RemoteProxy.Server;
import cn.evergrande.it.hdremoteproxylib.RemoteProxy.ServerStatus;
import cn.evergrande.it.hdremoteproxylib.evaluate.NetworkEvaluator;
import cn.evergrande.it.hdremoteproxylib.exceptions.RemoteProxyNotInitException;
import cn.evergrande.it.hdremoteproxylib.interfaces.ICheckInCallback;
import cn.evergrande.it.hdremoteproxylib.network.data.beans.CreateSecurityChannelParamsBean;
import cn.evergrande.it.hdremoteproxylib.network.data.beans.LBParamsBean;
import cn.evergrande.it.logger.BHLog;

/**
 * Created by wangyuhang@evergrande.cn on 2017/7/25 0025.
 */

class CheckInBiz {

    public final static String TAG = "CheckInBiz";

    private final int CREATE_SEC_CONNECTION = 1;//创建安全连接
    private final int REQ_SEC_CHANNEL = 2;//请求安全连接
    private final int CREATE_LB_CONNECTION = 3;//创建LB连接
    private final int REQ_LB_CHANNEL = 4;//请求LB连接
    private final int CREATE_BIZ_CONNECTION = 5;//创建业务连接

    private final int CHECK_IN_SUCCESS = 6;//接入流程成功
    private final int CHECK_IN_FAIL = 7;//接入流程失败

    private int currStep;// 当前配置的步骤

    private TcpClient tcpClient;

    private TcpConnection tcpConnection;

    private Gson gson;

    private JsonParser jsonParser;

    private ICheckInCallback checkInManagerCallback;

    private String bizServerIpAddress;
    private String bizServerPort;

    private Context mContext;

    private AbstractRemote mRemote;
    private ServerStatus mServerStatus;


    public CheckInBiz(Context context, ICheckInCallback callback, AbstractRemote remote, ServerStatus serverStatus) {
        mContext = context;
        mRemote = remote;
        mServerStatus = serverStatus;

        tcpClient = TcpClient.getInstance();

        gson = new Gson();
        jsonParser = new JsonParser();

        checkInManagerCallback = callback;

        currStep = CREATE_SEC_CONNECTION;
    }

    public synchronized void confirmProcess() {
        switch (currStep) {
            case CREATE_SEC_CONNECTION:
                createSecConnection();
                break;
            case REQ_SEC_CHANNEL:
                reqSecChannel();
                break;
            case CREATE_LB_CONNECTION:
                createLBConnection();
                break;
            case REQ_LB_CHANNEL:
                reqLBChannel();
                break;
            case CREATE_BIZ_CONNECTION:
                createBizConnection();
                break;
            case CHECK_IN_FAIL:
                checkInFail();
                break;
            case CHECK_IN_SUCCESS:
                checkInSuccess();
                break;
        }
    }

    public void createSecConnection() {
        Server secServer = mServerStatus.getSecServer();

        if (!secServer.hasValidIpAddress()) {
            secServer.transformHostname2IpAddressSync(secServer.getHostname()); // 获取云端IP地址
            secServer.setPort(RemoteProxy.SEC_SERVER_HOST_PORT);
        }

        String secServerHostIp = secServer.getHostIpAddress();
        int secServerPort = secServer.getPort();

        BHLog.i(TAG, String.format("createSecConnection: sec_server=%s, sec_server_port=%s", secServerHostIp, secServerPort));

        try {
            notifyChannelStateChanged(IEstablishChannelStateMonitor.CHANNEL_STAGE_START);
            tcpConnection = tcpClient.createTcpConnection(secServerHostIp, secServerPort);

            if (tcpConnection != null) {
                BHLog.i(TAG, "createSecConnection: success");

                currStep = REQ_SEC_CHANNEL;
                confirmProcess();
            } else {
                BHLog.e(TAG, "createBizConnection: fail");

                currStep = CHECK_IN_FAIL;
                confirmProcess();
            }
        } catch (Exception e) {
            BHLog.e(TAG, e.getMessage(), e);

            currStep = CHECK_IN_FAIL;
            confirmProcess();
        }
    }

    public void reqSecChannel() {
        CommonContentReq commonContentReq = new CommonContentReq();
        commonContentReq.setMethod(Protocal.CLOUD_REQ_METHODS_CREATE_SECURITY_CHANNEL);

        CreateSecurityChannelParamsBean paramsBean = new CreateSecurityChannelParamsBean();
        try {
            RemoteProxy remoteProxy = RemoteProxy.getInstance();

            String uuid = remoteProxy.getUniqueID();
            if (TextUtils.isEmpty(uuid)) {
                remoteProxy.initUUID(mContext);
            }

            paramsBean.setUuid(remoteProxy.getUniqueID());
            paramsBean.setKey(CloudRemote.AES_KEY);
        } catch (RemoteProxyNotInitException rpniex) {
            BHLog.i(TAG, rpniex.getMessage(), rpniex);
            return;
        }

        commonContentReq.setParams(paramsBean);

        String reqDataStr = gson.toJson(commonContentReq);
        BHLog.i(TAG, String.format("[cloud<-app] %s (aesKey=%s, aesIv=%s)", reqDataStr, CloudRemote.AES_KEY, CloudRemote.AES_IV));

        // 创建安全通道需要使用RSA加密业务数据
        reqDataStr = ProtocolUtil.encryptWithIOTPubKey(CloudRemote.IOT_PUB_KEY, reqDataStr);
        reqDataStr += "\n";

        NetworkMsg networkMsg = new NetworkMsg(commonContentReq.getReq_id(), commonContentReq.getMethod()
                , NetworkMsg.MSG_TYPE.TYPE_REQ_SEC, reqDataStr);

        tcpClient.sendMsg(commonContentReq.getReq_id(), networkMsg, 10, false, new TcpCallback() {
            @Override
            public void onSuccess(Object data) {
                tcpClient.closeTcpConnection(tcpConnection, false);

                int code = GsonUtil.str2JsonObj((String) data).get(Protocal.PARAMS_KEY_COMNON_CODE).getAsInt();

                if (code == 0) {
                    BHLog.i(TAG, "reqSecChannel:sendMsg:onSuccess: success");

                    currStep = CREATE_LB_CONNECTION;
                    confirmProcess();
                } else {
                    currStep = CHECK_IN_FAIL;
                    confirmProcess();
                }
            }

            @Override
            public void onError(int errorCode) {
                BHLog.e(TAG, "reqSecChannel:sendMsg:orError: fail");
                tcpClient.closeTcpConnection(tcpConnection, false);

                currStep = CHECK_IN_FAIL;
                confirmProcess();
            }
        });
    }

    public void createLBConnection() {
        Server lbServer = mServerStatus.getLBServer();

        if (!lbServer.hasValidIpAddress()) {
            lbServer.transformHostname2IpAddressSync(lbServer.getHostname()); // 获取云端IP地址
            lbServer.setPort(RemoteProxy.LB_SERVER_HOST_PORT);
        }

        String lbServerHostIp = lbServer.getHostIpAddress();
        int lbServerPort = lbServer.getPort();

        BHLog.i(TAG, String.format("createLBConnection: lb_server=%s, lb_server_port=%s", lbServerHostIp, lbServerPort));

        try {
            tcpConnection = tcpClient.createTcpConnection(lbServerHostIp, lbServerPort);

            if (tcpConnection != null) {
                BHLog.i(TAG, "createLBConnection: success");

                currStep = REQ_LB_CHANNEL;
                confirmProcess();
            } else {
                BHLog.e(TAG, "createLBConnection: fail");

                currStep = CHECK_IN_FAIL;
                confirmProcess();
            }
        } catch (Exception e) {
            BHLog.e(TAG, e.getMessage(), e);

            currStep = CHECK_IN_FAIL;
            confirmProcess();
        }
    }

    public void reqLBChannel() {
        CommonContentReq commonContentReq = new CommonContentReq();

        commonContentReq.setMethod(Protocal.CLOUD_REQ_METHODS_LB);

        LBParamsBean paramsBean = new LBParamsBean();

        commonContentReq.setParams(paramsBean);

        String reqDataStr = gson.toJson(commonContentReq);
        reqDataStr += "\n";

        BHLog.i(TAG, String.format("[cloud<-app] %s", reqDataStr));

        NetworkMsg networkMsg = new NetworkMsg(commonContentReq.getReq_id(), commonContentReq.getMethod()
                , NetworkMsg.MSG_TYPE.TYPE_REQ_LB, reqDataStr);

        tcpClient.sendMsg(commonContentReq.getReq_id(), networkMsg, 10, false, new TcpCallback() {
            @Override
            public void onSuccess(Object data) {
                tcpClient.closeTcpConnection(tcpConnection, false);

                String result = (String) data;

                int code = GsonUtil.str2JsonObj(result).get(Protocal.PARAMS_KEY_COMNON_CODE).getAsInt();

                if (code == 0) {
                    try {
                        String ip = jsonParser.parse(result).getAsJsonObject().getAsJsonObject("result").get("ip").getAsString();
                        Integer port = jsonParser.parse(result).getAsJsonObject().getAsJsonObject("result").get("port").getAsInt();

                        if (ip != null && port != null) {
                            // 保存ip和端口号
                            Server cloudServer = mServerStatus.getCloudServer();
                            cloudServer.setHostIpAddress(ip);
                            cloudServer.setPort(port);

                            bizServerIpAddress = ip;
                            bizServerPort = String.valueOf(port);

                            BHLog.i(TAG, String.format("reqLBChannel:sendMsg:onSuccess: success, cloud server: %s:%s", ip, port));

                            currStep = CREATE_BIZ_CONNECTION;
                            confirmProcess();
                        } else {
                            BHLog.e(TAG, "reqLBChannel:sendMsg:onSuccess: fail");

                            currStep = CHECK_IN_FAIL;
                            confirmProcess();
                        }
                    } catch (Exception e) {
                        BHLog.e(TAG, e.getMessage(), e);

                        currStep = CHECK_IN_FAIL;
                        confirmProcess();
                    }
                } else {
                    BHLog.e(TAG, "reqLBChannel:sendMsg:onSuccess fail: code" + code);

                    currStep = CHECK_IN_FAIL;
                    confirmProcess();
                }
            }

            @Override
            public void onError(int errorCode) {
                BHLog.i(TAG, "reqLBChannel:sendMsg:onError: fail");
                tcpClient.closeTcpConnection(tcpConnection, false);

                currStep = CHECK_IN_FAIL;
                confirmProcess();
            }
        });
    }

    public void createBizConnection() {
        Server cloudServer = mServerStatus.getCloudServer();
        String cloudServerHostIp = cloudServer.getHostIpAddress();
        if (TextUtils.isEmpty(cloudServerHostIp)) {
            cloudServerHostIp = cloudServer.transformHostname2IpAddressSync(cloudServer.getHostname()); // 获取云端IP地址
        }

        int cloudServerPort = cloudServer.getPort();

        BHLog.i(TAG, String.format("createBizConnection: cloud_host_ip=%s, cloud_host_port=%s", cloudServerHostIp, cloudServerPort));

        try {
            mRemote.setRemoteAddressPort(cloudServerHostIp, "" + cloudServerPort);
            tcpConnection = tcpClient.createTcpConnection(cloudServerHostIp, cloudServerPort);

            if (tcpConnection != null) {
                BHLog.i(TAG, "createBizConnection: success");

                currStep = CHECK_IN_SUCCESS;

                cloudServer.setOnline(true);
                confirmProcess();
            } else {
                BHLog.e(TAG, "createBizConnection: fail");

                currStep = CHECK_IN_FAIL;

                cloudServer.setOnline(false);
                confirmProcess();
            }
        } catch (Exception e) {
            BHLog.e(TAG, e.getMessage(), e);

            currStep = CHECK_IN_FAIL;

            cloudServer.setOnline(false);
            confirmProcess();
        }
    }

    public void checkInSuccess() {
        BHLog.i(TAG, "checkInSuccess");

        Server cloudServer = mServerStatus.getCloudServer();
        cloudServer.setCheckInStatus(true);

        notifyChannelStateChanged(AbstractRemote.IEstablishChannelStateMonitor.CHANNEL_STAGE_SUCCESS);
        CloudRemote.getInstance().getCheckInSemaphore().release();//接入结束释放信号量

        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                ICheckInCallback.CheckinResult checkinResult = new ICheckInCallback.CheckinResult();
                checkinResult.setBizServerAddress(bizServerIpAddress);
                checkinResult.setBizServerPort(bizServerPort);

                if (checkInManagerCallback != null) {
                    checkInManagerCallback.onSuccess(checkinResult);
                }
            }
        });

    }

    public void checkInFail() {
        BHLog.i(TAG, "checkInFail");

        Server cloudServer = mServerStatus.getCloudServer();
        cloudServer.setCheckInStatus(false);

        notifyChannelStateChanged(AbstractRemote.IEstablishChannelStateMonitor.CHANNEL_STAGE_FAIL);
        CloudRemote.getInstance().getCheckInSemaphore().release();//接入结束释放信号量

        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (checkInManagerCallback != null) {
                    checkInManagerCallback.onError(ErrorCode.ERROR_CODE_91002);
                }
            }
        });

    }

    private void notifyChannelStateChanged(int stage){
        if (mRemote == null) {
            BHLog.i(TAG, String.format("notifyChannelStateChanged: stage=%d, but mRemote is null", stage));
            return;
        }

        mRemote.notifyChannelChanged(stage);
    }
}
