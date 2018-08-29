/**
 *****************************************************************************
 * Copyright (C) 2018-2023 HD Corporation. All rights reserved
 * File        : RemoteProxyHelper.java
 *
 * Description : RemoteProxyHelper, RemoteProxy辅助类，简化RemoteProxy接口使用
 *
 * Creation    : 2018-05-10
 * Author      : tanguilong@evergrande.cn
 * History     : 2018-05-10, Creation
 *****************************************************************************
 */
package cn.evergrande.it.hdremoteproxylib;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.TextUtils;

import cn.evergrande.it.hdnetworklib.api.biz.ApiSendJsonCallback;
import cn.evergrande.it.hdnetworklib.api.biz.ApiSendReqCallback;
import cn.evergrande.it.hdnetworklib.api.biz.GsonUtil;
import cn.evergrande.it.hdnetworklib.api.model.protocal.ErrorCode;
import cn.evergrande.it.hdnetworklib.network.common.biz.ProtocolUtil;
import cn.evergrande.it.hdnetworklib.api.model.protocal.Protocal;
import cn.evergrande.it.hdnetworklib.network.common.model.NetworkMsg;
import cn.evergrande.it.hdnetworklib.network.common.model.protocal.ParamsBean;
import cn.evergrande.it.hdremoteproxylib.exceptions.CanNotObtainRemoteException;
import cn.evergrande.it.hdremoteproxylib.exceptions.RemoteProxyNotInitException;
import cn.evergrande.it.hdremoteproxylib.exceptions.RequestParamsIncorrectException;
import cn.evergrande.it.hdremoteproxylib.network.policy.AbstractRouteStrategy;
import cn.evergrande.it.hdremoteproxylib.network.request.RequestParams;
import cn.evergrande.it.hdtoolkits.utils.Constants;
import cn.evergrande.it.logger.BHLog;

public class RemoteProxyHelper {

    public final static String TAG = "RemoteProxyHelper";

    private static RemoteProxy sRemoteProxy;
    static {
        sRemoteProxy = RemoteProxy.getInstance();
    }

    /**
     * Android系统类型（平板）
     */
    public static final String ANDROID_OS_TYPE = "Android_Pad";

    /**
     * Android系统类型（手机）
     */
    public static final String ANDROID_OS_TYPE_PHONE = "Android_App";

    /**
     * 调用底层RemoteProxy的接口，决策通道，加密数据，发送数据
     *
     * @param target 请求发送的目标，必须为以下取值TARGET_INVALID、TARGET_AUTO、TARGET_CLOUD、TARGET_ROUTER，这四个常量定义在@see AbstractRouteStrategy.java
     * @param reqMethod 请求协议命令（方法）
     * @param subCmd    请求协议子命令（子方法）
     * @param params    封装好的bean, 如无则置为null
     * @param resultBeanClass Json响应结果bean, 如无则置为null
     * @param timeout   消息等待超时时间
     * @param isImmediate 是否紧急即刻发送
     * @param callback TCP请求结果回调
     */
    public static void sendReq(int target, final String reqMethod, final String subCmd, final ParamsBean params, final Class resultBeanClass,
                                         int timeout, boolean isImmediate, final ApiSendReqCallback callback) {
        sendReq(target, reqMethod, subCmd, params, resultBeanClass, timeout, isImmediate, callback, null);
    }

    /**
     * 调用底层RemoteProxy的接口，决策通道，加密数据，发送数据
     *
     * @param target 请求发送的目标，必须为以下取值TARGET_INVALID、TARGET_AUTO、TARGET_CLOUD、TARGET_ROUTER，这四个常量定义在@see AbstractRouteStrategy.java
     * @param reqMethod 请求协议命令（方法）
     * @param subCmd    请求协议子命令（子方法）
     * @param params    封装好的bean, 如无则置为null
     * @param resultBeanClass Json响应结果bean, 如无则置为null
     * @param timeout   消息等待超时时间
     * @param isImmediate 是否紧急即刻发送
     * @param callback TCP请求结果回调
     * @param strategy 上层业务自定义路由选择策略
     */
    public static void sendReq(int target, final String reqMethod, final String subCmd, final ParamsBean params, final Class resultBeanClass,
                               int timeout, boolean isImmediate, final ApiSendReqCallback callback, final AbstractRouteStrategy strategy) {
        RequestParams.Builder requestBuilder = new RequestParams.Builder();
        requestBuilder.addTarget(target)
                      .addReqMethod(reqMethod)
                      .addNodeId(subCmd)
                      .addParamsBean(params)
                      .addResultBeanClass(resultBeanClass)
                      .addTimeout(timeout)
                      .addImmediate(isImmediate)
                      .addRouteStrategy(strategy);

        RequestParams requestParams = requestBuilder.build();

        boolean sendReqResultFlag = false;
        try {
            sRemoteProxy.sendReq(requestParams, callback);
            sendReqResultFlag = true;
        } catch (RemoteProxyNotInitException rpniex) {
            BHLog.i(TAG, rpniex.getMessage(), rpniex);
        } catch (RequestParamsIncorrectException rpiex) {
            BHLog.i(TAG, rpiex.getMessage(), rpiex);
        } catch (CanNotObtainRemoteException cnorex) {
            BHLog.i(TAG, cnorex.getMessage(), cnorex);
        } finally {
            if (!sendReqResultFlag && callback != null) { // 发送过程出现异常，则执行发送失败回调
                callback.onError(ErrorCode.ERROR_CODE_90002);
            }
        }
    }

    /**
     * @return 应用实例全局唯一ID (GUID)
     */
    public static String getUniqueID() {
        try {
            return sRemoteProxy.getUniqueID();
        } catch (RemoteProxyNotInitException rpniex) {
            BHLog.i(TAG, rpniex.getMessage(), rpniex);
            return "";
        }
    }

    /**
     * 获取版本名
     */
    public static String getAppVersionName() {
        PackageInfo packageInfo = getPackageInfo();
        return packageInfo == null ? null : packageInfo.versionName;
    }

    public static String getOsType(){
        if( TextUtils.equals("PHONE",Constants.OUTPUT_TYPE)){
            return ANDROID_OS_TYPE_PHONE;
        }
        return ANDROID_OS_TYPE;
    }
    /**
     * 获取包信息
     */
    private static PackageInfo getPackageInfo() {
        Context context = sRemoteProxy.getContext();
        String packageName = context.getPackageName();
        PackageManager packageManager = context.getPackageManager();
        try {
            return packageManager.getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 通过errorCode获取errorMsg
     * @param errorCode 错误码
     * @return 错误码对应的文本消息
     */
    public static String getErrorMsg(int errorCode) {
        final Context context = sRemoteProxy.getContext();
        if (context == null) {
            return null;
        }

        if (errorCode < 0) {
            errorCode = 0 - errorCode;
        }

        String stringName = "error_code_msg_" + errorCode;

        int resId = context.getResources().getIdentifier(stringName, "string", context.getPackageName());
        if (resId != 0) {
            return context.getResources().getString(resId);
        } else {
            return null;
        }
    }

    /**
     * 通过errorCode获取errorTips
     * @param errorCode 错误码
     * @return 错误码对应的文本说明
     */
    public static String getErrorTips(int errorCode) {
        final Context context = sRemoteProxy.getContext();

        if (errorCode < 0) {
            errorCode = 0 - errorCode;
        }

        String stringName = "error_code_tips_" + errorCode;

        int resId = context.getResources().getIdentifier(stringName, "string", context.getPackageName());
        if (resId != 0) {
            return context.getResources().getString(resId);
        } else {
            return null;
        }
    }

    /**
     * 获取最新的路由器MAC地址
     */
    public static String getRouterMac(Context context) {
        String connectedWifiMacAddress = "";

        if (context != null) {
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                WifiInfo info = wifiManager.getConnectionInfo();
                if (info == null) {
                    return connectedWifiMacAddress;
                } else {
                    connectedWifiMacAddress = info.getBSSID();
                }
            }
        }

        return connectedWifiMacAddress;
    }

    // 由于历史原因, sendJson被上层业务大量使用, 且使用大多不规范, 本期重构暂不优化sendJson, 后期重构将对sendJson进行优化
    // ----------------------------------------- sendJson code snippet start ----------------------------------------------
    /**
     * @param paramsJson 封装到params那一层的json串
     */
    @Deprecated
    public static void sendJson(NetworkMsg.MSG_TYPE msgType, int req_id, final String method, String paramsJson, int timeout, final ApiSendJsonCallback callback) {
        sendJson(msgType, req_id, method, paramsJson, timeout, callback, null);
    }

    /**
     * @param paramsJson 封装到params那一层的json串
     */
    @Deprecated
    public static void sendUniCastJson(NetworkMsg.MSG_TYPE msgType, final String method, String paramsJson, int timeout, final ApiSendJsonCallback callback) {
        sendJson(msgType, method, paramsJson, timeout, callback, null);
    }

    /**
     * @param paramsJson 封装到params那一层的json串
     */
    @Deprecated
    public static void sendJson(NetworkMsg.MSG_TYPE msgType, final String method, String paramsJson, int timeout, final ApiSendJsonCallback callback) {
        sendJson(msgType, method, paramsJson, timeout, callback, null);
    }

    /**
     * @param paramsJson 封装到params那一层的json串
     */
    @Deprecated
    public static void sendJson(NetworkMsg.MSG_TYPE msgType, final String method, final String nodeId, String paramsJson, int timeout, final ApiSendJsonCallback callback) {
        sendJson(msgType, method, nodeId, paramsJson, timeout, callback, null);
    }

    /**
     * @param paramsJson 封装到params那一层的json串
     */
    @Deprecated
    public static void sendJson(NetworkMsg.MSG_TYPE msgType, final String method, final String nodeId, String paramsJson, boolean bUniCast, int timeout, final ApiSendJsonCallback callback) {
        sendJson(msgType, method, nodeId, paramsJson, bUniCast, timeout, callback, null);
    }

    /**
     * @param contentJson 封装到content那一层的json串
     */
    @Deprecated
    public static void sendJson(NetworkMsg.MSG_TYPE msgType, String contentJson, int timeout, final ApiSendJsonCallback callback) {
        sendJson(msgType, contentJson, timeout, callback, null);
    }

    /**
     * 直接请求json
     *
     * @param contentJson 封装到content那一层的json串
     * @param timeout     消息等待超时时间
     * @param isImmediate 是否紧急即刻发送
     * @param bUniCast   mdp请求后云端是否只对请求端回复
     */
    @Deprecated
    public static void sendJson(NetworkMsg.MSG_TYPE msgType, final int req_id, final String reqMethod, String contentJson,
                         int timeout, boolean isImmediate, boolean bUniCast, final ApiSendJsonCallback callback) {
        sendJson(msgType, req_id, reqMethod, contentJson, timeout, isImmediate, bUniCast, callback, null);
    }

    // 以下为带策略参数的sendJson接口
    /**
     * @param paramsJson 封装到params那一层的json串
     */
    @Deprecated
    public static void sendJson(NetworkMsg.MSG_TYPE msgType, int req_id, final String method, String paramsJson, int timeout, final ApiSendJsonCallback callback, AbstractRouteStrategy strategy) {
        String contentJson = GsonUtil.generateContentJsonStr(req_id, method, paramsJson);

        RemoteProxy.getInstance().sendJson(msgType, req_id, method, contentJson, timeout, false,false, callback, strategy);
    }

    /**
     * @param paramsJson 封装到params那一层的json串
     */
    @Deprecated
    public static void sendUniCastJson(NetworkMsg.MSG_TYPE msgType, final String method, String paramsJson, int timeout, final ApiSendJsonCallback callback, AbstractRouteStrategy strategy) {
        int req_id = ProtocolUtil.getReq_id();
        String contentJson = GsonUtil.generateContentJsonStr(req_id, method, paramsJson);

        RemoteProxy.getInstance().sendJson(msgType, req_id, method, contentJson, timeout, false,true, callback, strategy);
    }

    /**
     * @param paramsJson 封装到params那一层的json串
     */
    @Deprecated
    public static void sendJson(NetworkMsg.MSG_TYPE msgType, final String method, String paramsJson, int timeout, final ApiSendJsonCallback callback, AbstractRouteStrategy strategy) {
        int req_id = ProtocolUtil.getReq_id();
        String contentJson = GsonUtil.generateContentJsonStr(req_id, method, paramsJson);

        RemoteProxy.getInstance().sendJson(msgType, req_id, method, contentJson, timeout, false,false, callback, strategy);
    }

    /**
     * @param paramsJson 封装到params那一层的json串
     */
    @Deprecated
    public static void sendJson(NetworkMsg.MSG_TYPE msgType, final String method, final String nodeId, String paramsJson, int timeout, final ApiSendJsonCallback callback, AbstractRouteStrategy strategy) {
        int req_id = ProtocolUtil.getReq_id();
        String contentJson = GsonUtil.generateContentJsonStr(req_id, method, nodeId, paramsJson);

        RemoteProxy.getInstance().sendJson(msgType, req_id, method, contentJson, timeout, false,false, callback, strategy);
    }

    /**
     * @param paramsJson 封装到params那一层的json串
     */
    @Deprecated
    public static void sendJson(NetworkMsg.MSG_TYPE msgType, final String method, final String nodeId, String paramsJson, boolean bUniCast, int timeout, final ApiSendJsonCallback callback, AbstractRouteStrategy strategy) {
        int req_id = ProtocolUtil.getReq_id();
        String contentJson = GsonUtil.generateContentJsonStr(req_id, method, nodeId, paramsJson);

        RemoteProxy.getInstance().sendJson(msgType, req_id, method, contentJson, timeout, false,bUniCast, callback, strategy);
    }

    /**
     * @param contentJson 封装到content那一层的json串
     */
    @Deprecated
    public static void sendJson(NetworkMsg.MSG_TYPE msgType, String contentJson, int timeout, final ApiSendJsonCallback callback, AbstractRouteStrategy strategy) {
        String method = GsonUtil.getStringProp(Protocal.PARAMS_KEY_COMNON_METHOD, contentJson);
        int req_id = GsonUtil.getIntProp(Protocal.PARAMS_KEY_COMNON_REQ_ID, contentJson);

        RemoteProxy.getInstance().sendJson(msgType, req_id, method, contentJson, timeout, false,false, callback, strategy);
    }

    /**
     * 直接请求json
     *
     * @param contentJson 封装到content那一层的json串
     * @param timeout     消息等待超时时间
     * @param isImmediate 是否紧急即刻发送
     * @param bUniCast   mdp请求后云端是否只对请求端回复
     */
    @Deprecated
    public static void sendJson(NetworkMsg.MSG_TYPE msgType, final int req_id, final String reqMethod, String contentJson,
                                int timeout, boolean isImmediate, boolean bUniCast, final ApiSendJsonCallback callback, AbstractRouteStrategy strategy) {
        RemoteProxy.getInstance().sendJson(msgType, req_id, reqMethod, contentJson, timeout, isImmediate, bUniCast, callback, strategy);
    }

    /**
     * 转发H5 json串命令
     * @param contentJson json串命令
     * @param callback 成功/失败回调
     * @param strategy 请求的路由策略
     */
    public static void forwardH5Request(String contentJson, ApiSendJsonCallback callback, AbstractRouteStrategy strategy) {
        if (TextUtils.isEmpty(contentJson)) {
            BHLog.i(TAG, "forwardH5Request: contentJson is null or empty");
            return;
        }

        if (strategy == null) {
            BHLog.i(TAG, "forwardH5Request: strategy is null");
            return;
        }

        final int req_id = GsonUtil.getIntProp(Protocal.PARAMS_KEY_COMNON_REQ_ID, contentJson);
        final String reqMethod = GsonUtil.getStringProp(Protocal.PARAMS_KEY_COMNON_METHOD, contentJson);

        BHLog.i(TAG, String.format("forwardH5Request reqMethod=%s", reqMethod));

        RemoteProxy.getInstance().sendJson(NetworkMsg.MSG_TYPE.TYPE_INVALID, req_id, reqMethod, contentJson, RequestParams.DEFAULT_NETWORKMSG_TIMEOUT, false, false, callback, strategy);
    }

    // ----------------------------------------- sendJson code snippet end ----------------------------------------------
}
