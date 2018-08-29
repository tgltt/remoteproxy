/**
 *****************************************************************************
 * Copyright (C) 2018-2023 HD Corporation. All rights reserved
 * File        : IJsonResponseParser.java
 *
 * Description : IJsonResponseParser,
 *
 * Creation    : 2018-05-16
 * Author      : tanguilong@evergrande.cn
 * History     : 2018-05-16, Creation
 *****************************************************************************
 */
package cn.evergrande.it.hdremoteproxylib.interfaces;

import cn.evergrande.it.hdnetworklib.network.common.model.protocal.CommonContentRes;
import cn.evergrande.it.hdnetworklib.network.common.model.protocal.ResultBean;

public interface IJsonResponseParser {

    /**
     * 解码，将json解析成对应的bean
     *
     * @param reqMethod 协议命令
     * @param subCmd 协议子命令
     * @param json 响应结果json串
     * @param resultBeanClass 存储json串的bean类
     * @return 响应结果协议结构体
     */
    public CommonContentRes decodeJsonToResponse(String reqMethod, String subCmd, String json, Class<? extends ResultBean> resultBeanClass);
}
