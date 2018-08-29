/**
 *****************************************************************************
 * Copyright (C) 2018-2023 HD Corporation. All rights reserved
 * File        : IMdpMessageHandler.java
 *
 * Description : IMdpMessageHandler,
 *
 * Creation    : 2018-05-15
 * Author      : tanguilong@evergrande.cn
 * History     : 2018-05-15, Creation
 *****************************************************************************
 */
package cn.evergrande.it.hdremoteproxylib.interfaces;

import cn.evergrande.it.hdnetworklib.network.common.model.RemoteEventMsg;

public interface IMdpMessageHandler {
    /**
     * 处理推送过来的网络数据
     * @param event
     */
    public void dealMDP(RemoteEventMsg event);
}
