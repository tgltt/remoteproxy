/**
 *****************************************************************************
 * Copyright (C) 2018-2023 HD Corporation. All rights reserved
 * File        : AbstractAction.java
 *
 * Description : AbstractAction,
 *
 * Creation    : 2018-05-29
 * Author      : tanguilong@evergrande.cn
 * History     : 2018-05-29, Creation
 *****************************************************************************
 */
package cn.evergrande.it.hdremoteproxylib.network.data.actions;

public abstract class AbstractAction implements Runnable {
    /**
     * Action 主分类ID
     */
    protected int mActionId;

    /**
     * Action 子分类ID
     */
    protected int mActionSubId;

    /**
     * 执行Action所需参数
     */
    protected Object[] mActionArgs;

    public int getActionId() {
        return mActionId;
    }

    public void setActionId(int actionId) {
        mActionId = actionId;
    }

    public int getActionSubId() {
        return mActionSubId;
    }

    public void setActionSubId(int actionSubId) {
        mActionSubId = actionSubId;
    }

    public Object[] getActionArgs() {
        return mActionArgs;
    }

    public void setActionArgs(Object[] actionArgs) {
        mActionArgs = actionArgs;
    }

    protected abstract boolean checkAndParseParams(Object[] paramArgs);

}
