/**
 *****************************************************************************
 * Copyright (C) 2018-2023 HD Corporation. All rights reserved
 * File        : AbstractRouteStrategy.java
 *
 * Description : AbstractRouteStrategy,
 *
 * Creation    : 2018-05-12
 * Author      : tanguilong@evergrande.cn
 * History     : 2018-05-12, Creation
 *****************************************************************************
 */
package cn.evergrande.it.hdremoteproxylib.network.policy;

import cn.evergrande.it.hdnetworklib.network.common.model.NetworkMsg.MSG_TYPE;
import cn.evergrande.it.hdremoteproxylib.network.request.RequestParams;
import cn.evergrande.it.hdremoteproxylib.exceptions.CanNotObtainRemoteException;

public abstract class AbstractRouteStrategy {
    /**
     * 无效目标
     */
    public final static int TARGET_INVALID = -1;
    /**
     * 自动选择走云端，还是走路由器
     */
    public final static int TARGET_AUTO = 0;
    /**
     * 选择走云端
     */
    public final static int TARGET_CLOUD = 1;
    /**
     * 选择走路由器
     */
    public final static int TARGET_ROUTER = 2;
    /**
     * 选择走云端, 并且不等待云端响应
     */
    public final static int TARGET_CLOUD_NOT_WAITING_RESPONSE = 3;
    /**
     * 选择走路由器, 并且不等待路由器响应
     */
    public final static int TARGET_ROUTER_NOT_WAITING_RESPONSE = 4;
    /**
     * 选择走日志服务器, 并且不等待日志服务器响应
     */
    public final static int TARGET_LOGGER_NOT_WAITING_RESPONSE = 5;
    /**
     * 选择走云端透传
     */
    public final static int TARGET_CLOUD_TRANSPARENT_TRANSFER = 6;

    /**
     * 选择远程目标对象，以决定走云端，还是走路由器
     * @param params， 选择策略所需要的参数
     * @return 远程目标对象结果
     */
    public abstract RouteStrategyResult selectRemoteTarget(RouteStrategyParams params) throws CanNotObtainRemoteException;

    /**
     * 确定消息发送超时时间
     * @param method 家居协议方法
     * @return 超时时间
     */
    public abstract int decideMsgTimeout(String method);

    public static MSG_TYPE getMsgType(int target) {
        switch (target) {
            case TARGET_CLOUD:
                return MSG_TYPE.TYPE_REQ_CLOUD;
            case TARGET_ROUTER:
                return MSG_TYPE.TYPE_REQ_ROUTER;
            case TARGET_CLOUD_NOT_WAITING_RESPONSE:
                return MSG_TYPE.TYPE_REQ_CLOUD_WITHOUT_RES;
            case TARGET_ROUTER_NOT_WAITING_RESPONSE:
                return MSG_TYPE.TYPE_REQ_ROUTER_WITHOUT_RES;
            case TARGET_LOGGER_NOT_WAITING_RESPONSE:
                return MSG_TYPE.TYPE_REQ_LOGGER_WITHOUT_RES;
            case TARGET_CLOUD_TRANSPARENT_TRANSFER:
                return MSG_TYPE.TYPE_REQ_CLOUD_TRANS;
            case TARGET_AUTO:
            default:
                return MSG_TYPE.TYPE_INVALID;
        }
    }

    public static int getTarget(MSG_TYPE msgType) {
        if (msgType == null) {
            return TARGET_INVALID;
        }

        if (msgType == MSG_TYPE.TYPE_REQ_CLOUD) {
            return TARGET_CLOUD;
        } else if (msgType == MSG_TYPE.TYPE_REQ_ROUTER) {
            return TARGET_ROUTER;
        } else if (msgType == MSG_TYPE.TYPE_REQ_CLOUD_WITHOUT_RES) {
            return TARGET_CLOUD_NOT_WAITING_RESPONSE;
        } else if (msgType == MSG_TYPE.TYPE_REQ_ROUTER_WITHOUT_RES) {
            return TARGET_ROUTER_NOT_WAITING_RESPONSE;
        } else if (msgType == MSG_TYPE.TYPE_REQ_LOGGER_WITHOUT_RES) {
            return TARGET_LOGGER_NOT_WAITING_RESPONSE;
        } else if (msgType == MSG_TYPE.TYPE_REQ_CLOUD_TRANS) {
            return TARGET_CLOUD_TRANSPARENT_TRANSFER;
        }
        // msgType取值MSG_TYPE.TYPE_INVALID，或取值不在下列范围：
        // TYPE_REQ_CLOUD、TYPE_REQ_ROUTER、TYPE_REQ_CLOUD_WITHOUT_RES、TYPE_REQ_ROUTER_WITHOUT_RES、TYPE_REQ_LOGGER_WITHOUT_RES、
        // TYPE_REQ_CLOUD_TRANS、TYPE_INVALID
        return TARGET_AUTO;
    }

    public static class RouteStrategyResult {
        public final static int INVALID_VALUE = Integer.MIN_VALUE;
        /**
         * 远程目标，取值如下：
         * TARGET_INVALID、TARGET_AUTO、TARGET_CLOUD、TARGET_ROUTER、TARGET_LOGGER、TARGET_CLOUD_NOT_WAITING_RESPONSE、TARGET_ROUTER_NOT_WAITING_RESPONSE、
         * TARGET_LOGGER_NOT_WAITING_RESPONSE、TARGET_CLOUD_TRANSPARENT_TRANSFER
         */
        private int mTarget = TARGET_AUTO;
        /**
         * 当前路由器ID，在mTarget取值为TARGET_CLOUD_TRANSPARENT_TRANSFER时，必须提供该参数，
         * 可通过FamilyManager.getInstance().getCurrentRouterId()获取
         */
        private long mCurRouteId = INVALID_VALUE;
        /**
         * TCP请求等待响应超时时间，单位s，取值如下：
         * RequestParams.DEFAULT_NETWORKMSG_TIMEOUT、RequestParams.SHORT_NETWORKMSG_TIMEOUT、RequestParams.NORMAL_NETWORKMSG_TIMEOUT、
         * RequestParams.LONG_NETWORKMSG_TIMEOUT
         */
        private int mTimeout = RequestParams.DEFAULT_NETWORKMSG_TIMEOUT;
        /**
         * 错误码
         */
        private int mErrorCode;
        /**
         * 期望target，用于策略找不到合适target时，告知RemoteProxy重连该target
         */
        private int mExpectedTarget = AbstractRouteStrategy.TARGET_AUTO;

        public int getTarget() {
            return mTarget;
        }

        public void setTarget(int target) {
            mTarget = target;
        }

        public long getCurRouteId() {
            return mCurRouteId;
        }

        public void setCurRouteId(long curRouteId) {
            mCurRouteId = curRouteId;
        }

        public int getTimeout() {
            return mTimeout;
        }

        public void setTimeout(int timeout) {
            mTimeout = timeout;
        }

        public int getErrorCode() {
            return mErrorCode;
        }

        public void setErrorCode(int errorCode) {
            mErrorCode = errorCode;
        }

        public int getExpectedTarget() {
            return mExpectedTarget;
        }

        public void setExpectedTarget(int expectedTarget) {
            mExpectedTarget = expectedTarget;
        }
    }

    public static class RouteStrategyParams {
        /**
         * 协议方法
         */
        private String mRequestMethod;

        public String getRequestMethod() {
            return mRequestMethod;
        }

        public void setRequestMethod(String requestMethod) {
            mRequestMethod = requestMethod;
        }
    }
}
