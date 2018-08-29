/**
 *****************************************************************************
 * Copyright (C) 2018-2023 HD Corporation. All rights reserved
 * File        : ReconnectCloudRemoteAction.java
 *
 * Description : ReconnectCloudRemoteAction,
 *
 * Creation    : 2018-05-29
 * Author      : tanguilong@evergrande.cn
 * History     : 2018-05-29, Creation
 *****************************************************************************
 */
package cn.evergrande.it.hdremoteproxylib.network.data.actions;

import cn.evergrande.it.hdremoteproxylib.CloudRemote;
import cn.evergrande.it.logger.BHLog;

public class ReconnectCloudRemoteAction extends BaseAction {

    public final static String TAG = "ReconnectCloudRemoteAction";

    public ReconnectCloudRemoteAction(int actionId, int actionSubId, Object[] args) {
        super(actionId, actionSubId, args);
    }

    @Override
    public void run() {
        if (mActionArgs == null || mActionArgs.length <= 0) {
            BHLog.i(TAG, "mActionArgs is null or empty");
            return;
        }

        if (!(mActionArgs[0] instanceof CloudRemote)) {
            BHLog.i(TAG, "mActionArgs[0] is not CloudRemote");
            return;
        }

        BHLog.i(TAG, "doReconnectCloud: cloudRemote is reconnecting");

        new ConnectCloudRemoteAction(mActionId, mActionSubId, mActionArgs).run();
    }
}
