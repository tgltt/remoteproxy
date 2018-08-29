/**
 *****************************************************************************
 * Copyright (C) 2018-2023 HD Corporation. All rights reserved
 * File        : CanNotObtainRemoteException.java
 *
 * Description : 无法获取TCP发送通道,
 *
 * Creation    : 2018-05-14
 * Author      : tanguilong@evergrande.cn
 * History     : 2018-05-14, Creation
 *****************************************************************************
 */
package cn.evergrande.it.hdremoteproxylib.exceptions;

public class CanNotObtainRemoteException extends Exception {

    public CanNotObtainRemoteException(String message) {
        super(message);
    }
}
