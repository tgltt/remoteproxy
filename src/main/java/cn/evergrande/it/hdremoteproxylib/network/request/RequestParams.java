/**
 *****************************************************************************
 * Copyright (C) 2018-2023 HD Corporation. All rights reserved
 * File        : RequestParams.java
 *
 * Description : RequestParams,
 *
 * Creation    : 2018-05-10
 * Author      : tanguilong@evergrande.cn
 * History     : 2018-05-10, Creation
 *****************************************************************************
 */
package cn.evergrande.it.hdremoteproxylib.network.request;

import android.text.TextUtils;

import cn.evergrande.it.hdnetworklib.network.common.model.protocal.ParamsBean;
import cn.evergrande.it.hdnetworklib.network.common.model.protocal.ResultBean;
import cn.evergrande.it.hdremoteproxylib.RouterRemote;
import cn.evergrande.it.hdremoteproxylib.network.policy.AbstractRouteStrategy;

public class RequestParams {

    /*网络消息等待超时时间*/
    /**
     * 短超时时间
     */
    public static final int SHORT_NETWORKMSG_TIMEOUT   = 10;
    /**
     * 正常超时时间
     */
    public static final int NORMAL_NETWORKMSG_TIMEOUT  = 30;
    /**
     * 长超时时间
     */
    public static final int LONG_NETWORKMSG_TIMEOUT    = 60;
    /**
     * 缺省超时时间
     */
    public static final int DEFAULT_NETWORKMSG_TIMEOUT = SHORT_NETWORKMSG_TIMEOUT;

    /**
     * Request请求的目标：云端或路由器, 默认自动选择走云端或路由器
     */
    private int mTarget = AbstractRouteStrategy.TARGET_AUTO;
    /**
     * Request请求目标端执行的操作ID
     */
    private String mReqMethod;
    /**
     * Request请求操作的设备节点
     */
    private String mNodeId;
    /**
     * Request请求目标端执行的操作所需要的参数
     */
    private ParamsBean mParamsBean;
    /**
     * 存储Response响应结果的bean类
     */
    private Class<? extends ResultBean> mResultBeanClass;

    /**
     * Request的超时时间，单位s，取值如下：<br/>
     * DEFAULT_NETWORKMSG_TIMEOUT、SHORT_NETWORKMSG_TIMEOUT、NORMAL_NETWORKMSG_TIMEOUT、LONG_NETWORKMSG_TIMEOUT
     */
    private int mTimeout;
    /**
     * Request是否立即发送，<br/>
     * true - 立即发送，false - 正常发送
     */
    private boolean mImmediate;
    /**
     * 路由策略，在mTarget为AbstractRouteStrategy.TARGET_AUTO时起作用
     */
    private AbstractRouteStrategy mRouteStrategy;

    public int getTarget() {
        return mTarget;
    }

    public String getReqMethod() {
        return mReqMethod;
    }

    public String getNodeId() {
        return mNodeId;
    }

    public ParamsBean getParamsBean() {
        return mParamsBean;
    }

    public int getTimeout() {
        return mTimeout;
    }

    public boolean isImmediate() {
        return mImmediate;
    }

    public long getCurRouterId() {
        return RouterRemote.getCurrentRouterId();
    }
    /**
     * 调整请求发送目标
     * @param newTarget 取值TARGET_AUTO、TARGET_CLOUD、TARGET_ROUTER，参见AbstractRouteStrategy
     */
    public void adjustTarget(int newTarget) {
        mTarget = newTarget;
    }

    public AbstractRouteStrategy getRouteStrategy() {
        return mRouteStrategy;
    }

    public void setRouteStrategy(AbstractRouteStrategy routeStrategy) {
        mRouteStrategy = routeStrategy;
    }

    public Class<? extends ResultBean> getResultBeanClass() {
        return mResultBeanClass;
    }

    public void setResultBeanClass(Class<? extends ResultBean> resultBeanClass) {
        mResultBeanClass = resultBeanClass;
    }

    public static class Builder {
        private int mTarget;
        private String mReqMethod;
        private String mNodeId;
        private ParamsBean mParamsBean;
        private int mTimeout;
        private boolean mImmediate;
        private AbstractRouteStrategy mRouteStratey;
        private Class<? extends ResultBean> mResultBeanClass;

        public Builder() {
            mTarget = AbstractRouteStrategy.TARGET_AUTO;
        }

        /**
         * 添加Request目标服务器
         * @param target 取值TARGET_ROUTER、TARGET_CLOUD、TARGET_AUTO，@see AbstractRouteStrategy
         * @return Request构造器
         */
        public Builder addTarget(int target) {
            switch (target) {
                case AbstractRouteStrategy.TARGET_ROUTER:
                case AbstractRouteStrategy.TARGET_CLOUD:
                     mTarget = target;
                     break;
                case AbstractRouteStrategy.TARGET_AUTO:
                default:
                      mTarget = AbstractRouteStrategy.TARGET_AUTO;
            }

            return this;
        }

        /**
         * 添加请求命令类型
         * @param reqMethod 请求命令类型
         * @return Request构造器
         */
        public Builder addReqMethod(String reqMethod) {
            if (reqMethod == null || reqMethod.isEmpty()) {
                throw new IllegalArgumentException("RequestParams.Builder: addReqMethod, reqMethod is null or empty!");
            }

            mReqMethod = reqMethod;
            return this;
        }

        /**
         * 请求参数中的nodeId
         * @param nodeId
         * @return Request构造器
         */
        public Builder addNodeId(String nodeId) {
            mNodeId = nodeId;
            return this;
        }

        /**
         * 添加请求命令参数
         * @param pb 请求命令参数
         * @return Request构造器
         */
        public Builder addParamsBean(ParamsBean pb) {
            mParamsBean = pb;
            return this;
        }

        /**
         * 添加请求超时时间
         * @param timeout 超时时间
         * @return Request构造器
         */
        public Builder addTimeout(int timeout) {
            mTimeout = timeout;
            return this;
        }

        /**
         * 定义请求是立即发送还是延迟发送
         * @param isImmediate false - 延迟发送
         *                    true - 立即发送
         * @return Request构造器
         */
        public Builder addImmediate(boolean isImmediate) {
            mImmediate = isImmediate;
            return this;
        }

        /**
         * 定义请求使用的路由策略
         * @param strategy 路由策略
         * @return Request构造器
         */
        public Builder addRouteStrategy(AbstractRouteStrategy strategy) {
            mRouteStratey = strategy;
            return this;
        }

        /**
         * 添加Request请求响应数据的结果类对象
         * @param resultBeanClass 响应数据的结果类对象
         * @return Request构造器
         */
        public Builder addResultBeanClass(Class<? extends ResultBean> resultBeanClass) {
            mResultBeanClass = resultBeanClass;
            return this;
        }

        /**
         * 构建Request请求参数
         * @return Request请求参数
         */
        public RequestParams build() {
            if (TextUtils.isEmpty(mReqMethod)) {
                throw new IllegalArgumentException("RequestParams.Builder: build, mReqMethod is null or empty!");
            }

            RequestParams requestParams = new RequestParams();

            requestParams.mTarget = mTarget;
            requestParams.mReqMethod = mReqMethod;
            requestParams.mNodeId = mNodeId;
            requestParams.mParamsBean = mParamsBean;
            requestParams.mImmediate = mImmediate;
            requestParams.mTimeout = mTimeout;
            requestParams.mRouteStrategy = mRouteStratey;

            if (mResultBeanClass == null) { // 使用默认ResultBean存储Json响应结果
                requestParams.mResultBeanClass = ResultBean.class;
            } else {
                requestParams.mResultBeanClass = mResultBeanClass;
            }

            return requestParams;
        }
    }
}