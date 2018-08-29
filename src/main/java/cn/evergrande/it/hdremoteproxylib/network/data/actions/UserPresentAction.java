/**
 *****************************************************************************
 * Copyright (C) 2018-2023 HD Corporation. All rights reserved
 * File        : UserPresentAction.java
 *
 * Description : UserPresentAction,
 *
 * Creation    : 2018-07-05
 * Author      : tanguilong@evergrande.cn
 * History     : 2018-07-05, Creation
 *****************************************************************************
 */
package cn.evergrande.it.hdremoteproxylib.network.data.actions;

import cn.evergrande.it.hdremoteproxylib.RemoteProxy;
import cn.evergrande.it.hdremoteproxylib.evaluate.NetworkEvaluator;
import cn.evergrande.it.logger.BHLog;

public class UserPresentAction extends BaseAction {

    public final static String TAG = "UserPresentAction";

    public UserPresentAction(int actionId, int actionSubId, Object[] args) {
        super(actionId, actionSubId, args);
    }

    @Override
    public void run() {
        BHLog.i(TAG, "User is present, now reconnecting...");
        RemoteProxy.getInstance().connect(RemoteProxy.REMOTE_ALL);
    }

}
