/**
 *****************************************************************************
 * Copyright (C) 2018-2023 HD Corporation. All rights reserved
 * File        : ReconnectRouteRemoteAction.java
 *
 * Description : ReconnectRouteRemoteAction,
 *
 * Creation    : 2018-05-29
 * Author      : tanguilong@evergrande.cn
 * History     : 2018-05-29, Creation
 *****************************************************************************
 */
package cn.evergrande.it.hdremoteproxylib.network.data.actions;

import cn.evergrande.it.hdremoteproxylib.RouterRemote;

import cn.evergrande.it.logger.BHLog;

public class ReconnectRouteRemoteAction extends BaseAction {

    public final static String TAG = "ReconnectRouteRemoteAction";

    public ReconnectRouteRemoteAction(int actionId, int actionSubId, Object[] args) {
        super(actionId, actionSubId, args);
    }

    @Override
    public void run() {
        if (mActionArgs == null || mActionArgs.length <= 0) {
            BHLog.i(TAG, "mActionArgs is null or empty");
            return;
        }

        if (!(mActionArgs[0] instanceof RouterRemote)) {
            BHLog.i(TAG, "mActionArgs[0] is not RouterRemote");
            return;
        }

        BHLog.i(TAG, "routerRemote is reconnecting");

        new ConnectRouteRemoteAction(mActionId, mActionSubId, mActionArgs).run();
    }
}
