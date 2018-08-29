/**
 *****************************************************************************
 * Copyright (C) 2018-2023 HD Corporation. All rights reserved
 * File        : ScreenOnAction.java
 *
 * Description : ScreenOnAction,
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

public class ScreenOnAction extends BaseAction {

    public final static String TAG = "ScreenOnAction";

    public ScreenOnAction(int actionId, int actionSubId, Object[] args) {
        super(actionId, actionSubId, args);
    }

    @Override
    public void run() {
        BHLog.i(TAG, "Screen is on, now reconnecting...");
        RemoteProxy.getInstance().connect(RemoteProxy.REMOTE_ALL);
    }

}
