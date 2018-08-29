/**
 *****************************************************************************
 * Copyright (C) 2018-2023 HD Corporation. All rights reserved
 * File        : BaseAction.java
 *
 * Description : BaseAction,
 *
 * Creation    : 2018-05-29
 * Author      : tanguilong@evergrande.cn
 * History     : 2018-05-29, Creation
 *****************************************************************************
 */
package cn.evergrande.it.hdremoteproxylib.network.data.actions;

public class BaseAction extends AbstractAction {

    public final static String TAG = "BaseAction";

    public BaseAction(int actionId, int actionSubId, Object[] args) {
        mActionId = actionId;
        mActionSubId = actionSubId;
        mActionArgs = args;
    }

    @Override
    public void run() {

    }

    protected boolean checkAndParseParams(Object[] paramArgs) {
        return true;
    }
}
