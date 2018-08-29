/**
 *****************************************************************************
 * Copyright (C) 2018-2023 HD Corporation. All rights reserved
 * File        : BaseRemote.java
 *
 * Description : BaseRemote,
 *
 * Creation    : 2018-06-11
 * Author      : tanguilong@evergrande.cn
 * History     : 2018-06-11, Creation
 *****************************************************************************
 */
package cn.evergrande.it.hdremoteproxylib;

import android.content.Context;

import cn.evergrande.it.hdremoteproxylib.evaluate.NetworkEvaluator;
import cn.evergrande.it.hdremoteproxylib.statemachine.TCPStateMachine;

public class BaseRemote extends AbstractRemote {

    public final static String TAG = "BaseRemote";

    protected BaseRemote() {
        mStateMachine = new TCPStateMachine(); // 状态机从Invalid切换到Constructed
        mStateMachine.load(this); // 状态机从Constructed切换到Init
    }

    @Override
    public void init(Context context, RemoteProxy.ServerStatus serverStatus) {

    }

    @Override
    public void connect(Object[] args) {

    }

    @Override
    protected boolean checkConnectParams(Object[] args) {
        return true;
    }

    @Override
    public void sendHeartbeat() {
        super.sendHeartbeat();
    }

    @Override
    public void disconnect() {
        super.disconnect();
    }

    @Override
    public RemoteStatus getRemoteStatus() {
        return null;
    }

}