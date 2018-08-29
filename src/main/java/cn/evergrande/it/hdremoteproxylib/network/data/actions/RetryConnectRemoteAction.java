/**
 *****************************************************************************
 * Copyright (C) 2018-2023 HD Corporation. All rights reserved
 * File        : RetryConnectRemoteAction.java
 *
 * Description : RetryConnectRemoteAction,
 *
 * Creation    : 2018-06-04
 * Author      : tanguilong@evergrande.cn
 * History     : 2018-06-04, Creation
 *****************************************************************************
 */
package cn.evergrande.it.hdremoteproxylib.network.data.actions;

import cn.evergrande.it.hdremoteproxylib.AbstractRemote;
import cn.evergrande.it.hdremoteproxylib.ActionManager;
import cn.evergrande.it.hdremoteproxylib.CloudRemote;
import cn.evergrande.it.hdremoteproxylib.RemoteProxy;
import cn.evergrande.it.hdremoteproxylib.RouterRemote;
import cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine;
import cn.evergrande.it.logger.BHLog;

public class RetryConnectRemoteAction extends BaseAction {

    public final static String TAG = "RetryConnectRemoteAction";
    /**
     * 已重试次数
     */
    private int mRetryCount;
    /**
     * 最大重试次数
     */
    private int mMaxRetryCount;

    /**
     * 重试间隔, 单位：s
     */
    private long mRetryInterval;
    /**
     * 当前Action是否有效
     */
    protected boolean mValid;

    public RetryConnectRemoteAction(int actionId, int actionSubId, Object[] args) {
        super(actionId, actionSubId, args);
        parseRetryParams();
    }

    private void parseRetryParams() {
        if (mActionArgs == null || mActionArgs.length <= 0) {
            BHLog.i(TAG, "parseRetryParams: mActionArgs is null or empty");
            return;
        }

        try {
            mMaxRetryCount = (int) mActionArgs[1];
            mRetryInterval = (long) mActionArgs[2] * 1000;
        } catch (Throwable throwable) {
            BHLog.i(TAG, throwable.getMessage(), throwable);
        } finally {
            if (mMaxRetryCount < 0) {
                mMaxRetryCount = 0;
            }

            if (mRetryInterval < 0) {
                mRetryInterval = 0;
            }
        }

        mValid = true;
    }

    protected void notifyCheckinStage(int remoteType, Object[] paramArgs, int stage) {
        try {
            AbstractRemote remote = null;
            if (remoteType == RemoteProxy.REMOTE_ROUTE) {
                remote = (RouterRemote) paramArgs[0];
            } else if (remoteType == RemoteProxy.REMOTE_CLOUD) {
                remote = (CloudRemote) paramArgs[0];
            }

            if (remote != null) {
                remote.notifyChannelChanged(stage);
            }
        } catch (Throwable throwable) {
            BHLog.i(TAG, throwable.getMessage(), throwable);
        }
    }

    protected void retryConnectRemote(AbstractRemote remote) {
        int networkState = RemoteProxy.getInstance().getCurNetworkEnv();
        boolean curNetworkEnabled = networkState == RemoteProxy.NETWORK_WIFI || networkState == RemoteProxy.NETWORK_MOBILE;

        if (mRetryCount >= mMaxRetryCount) {
            BHLog.i(TAG, String.format("retryConnectRemote(%s): mRetryCount(%d) >= mMaxRetryCount(%d)", remote, mRetryCount, mMaxRetryCount));

            TCPStateMachine stateMachine = remote.getStateMachine();
            stateMachine.sendEvent(TCPStateMachine.EVENT_CMD_RETRY_CONNECT_FAILED, null);

            return;
        }

        mRetryCount++;

        BHLog.i(TAG, String.format("retryConnectRemote(%s): retry connecting, retryCount=%d, maxRetryCount=%d, retryInterval=%d", remote, mRetryCount, mMaxRetryCount, mRetryInterval));

        if (!curNetworkEnabled) {
            BHLog.i(TAG, "retryConnectRemote: curNetworkEnabled=false, stop retry connecting remote");

            TCPStateMachine stateMachine = remote.getStateMachine();
            stateMachine.sendEvent(TCPStateMachine.EVENT_CMD_RETRY_CONNECT_FAILED, null);

            return;
        }

        ActionManager.getInstance().postActionDelayed(this, mRetryInterval);
    }
}
