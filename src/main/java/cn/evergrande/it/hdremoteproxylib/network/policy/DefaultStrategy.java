package cn.evergrande.it.hdremoteproxylib.network.policy;

import cn.evergrande.it.hdnetworklib.api.model.protocal.ErrorCode;
import cn.evergrande.it.hdnetworklib.api.model.protocal.Protocal;
import cn.evergrande.it.hdnetworklib.network.common.model.NetworkMsg.MSG_TYPE;
import cn.evergrande.it.hdremoteproxylib.RemoteProxy;
import cn.evergrande.it.hdremoteproxylib.RouterRemote;
import cn.evergrande.it.hdremoteproxylib.network.request.RequestParams;
import cn.evergrande.it.hdremoteproxylib.exceptions.CanNotObtainRemoteException;
import cn.evergrande.it.logger.BHLog;

/**
 * 消息发送决策
 */
public class DefaultStrategy extends AbstractRouteStrategy {
    private static final String TAG = "DefaultStrategy";

    private RemoteProxy.ServerStatus mServerStatus;

    public DefaultStrategy(RemoteProxy.ServerStatus serverStatus) {
        mServerStatus = serverStatus;
    }

    public static int mappingMsgType2Target(MSG_TYPE msgType) {
        if (msgType == MSG_TYPE.TYPE_REQ_CLOUD) {
            return TARGET_CLOUD;
        } else if (msgType == MSG_TYPE.TYPE_REQ_ROUTER) {
            return TARGET_ROUTER;
        } else if (msgType == MSG_TYPE.TYPE_REQ_CLOUD_WITHOUT_RES) {
            return TARGET_CLOUD_NOT_WAITING_RESPONSE;
        } else if (msgType == MSG_TYPE.TYPE_REQ_ROUTER_WITHOUT_RES) {
            return TARGET_ROUTER_NOT_WAITING_RESPONSE;
        } else if (msgType == MSG_TYPE.TYPE_REQ_LOGGER_WITHOUT_RES) {
            return TARGET_LOGGER_NOT_WAITING_RESPONSE;
        } else if (msgType == MSG_TYPE.TYPE_REQ_CLOUD_TRANS) {
            return TARGET_CLOUD_TRANSPARENT_TRANSFER;
        }

        return TARGET_INVALID;
    }

    @Override
    public RouteStrategyResult selectRemoteTarget(RouteStrategyParams params) throws CanNotObtainRemoteException {
        if (params == null)
            return null;

        RouteStrategyResult routeStrategyResult = new RouteStrategyResult();

        String reqMethod = params.getRequestMethod();
        StrategyResult strategyResult = getStrategy(reqMethod);

        MSG_TYPE msgType = strategyResult.getMsg_type();
        if (msgType == MSG_TYPE.TYPE_INVALID) {
            throw new CanNotObtainRemoteException("Can't obtain Remote");
        }

        routeStrategyResult.setTarget(mappingMsgType2Target(msgType));
        routeStrategyResult.setCurRouteId(RouterRemote.getCurrentRouterId());
        routeStrategyResult.setTimeout(decideMsgTimeout(reqMethod));
        routeStrategyResult.setErrorCode(strategyResult.getErrorCode());
        routeStrategyResult.setExpectedTarget(strategyResult.getExpectedTarget());

        return routeStrategyResult;
    }

    public StrategyResult getStrategy(String method) {
        StrategyResult strategyResult = new StrategyResult();
        strategyResult.setMsg_type(MSG_TYPE.TYPE_INVALID);
        strategyResult.setErrorCode(0);

        RemoteProxy.Server cloudServer = mServerStatus.getCloudServer();
        RemoteProxy.Server routeServer = mServerStatus.getRouterServer();

        boolean cloudChannelOnline = cloudServer.isOnline();
        boolean cloudLogin = cloudServer.isLogin();
        boolean routerChannelOnline = routeServer.isOnline();
        boolean routerLogin = routeServer.isLogin();

        switch (method) {
            //只走云端(判断通道可达和登录状态)
            case Protocal.CLOUD_REQ_METHODS_UM_GET_OTHER_LOGIN_CLIENTS://获取其他登录的终端列表
            case Protocal.CLOUD_REQ_METHODS_UM_KICKOFF_CLIENT://踢出其他终端用户
            case Protocal.CLOUD_REQ_METHODS_FM_GET_FAMILY_LIST://获取家庭列表
            case Protocal.CLOUD_REQ_METHODS_UM_SET_PWD://设置密码
            case Protocal.CLOUD_REQ_METHODS_UM_UPDATE_USER_PROFILE://更新用户资料
            case Protocal.CLOUD_REQ_METHODS_FM_CREATE_FAMILY://创建家庭
            case Protocal.CLOUD_REQ_METHODS_FM_DELETE_FAMILY://删除家庭
            case Protocal.CLOUD_REQ_METHODS_FM_BIND_ROUTER://云端绑定路由器
            case Protocal.CLOUD_REQ_METHODS_FM_UNBIND_ROUTER://云端解绑路由器
            case Protocal.CLOUD_REQ_METHODS_UM_GET_USER_ACCOUNT://获取用户帐户信息
            case Protocal.CLOUD_REQ_METHODS_ACC_LOGOUT://退出登录
            case Protocal.CLOUD_REQ_METHODS_FM_CREATE_MEMBER://创建家庭成员
            case Protocal.CLOUD_REQ_METHODS_FM_GET_APPLY_CODE://获取用户申请码
            case Protocal.CLOUD_REQ_METHODS_FM_REMOVE_MEMBER://删除家庭成员
            case Protocal.CLOUD_REQ_METHODS_FM_EXIT_FAMILY://退出家庭
            case Protocal.CLOUD_REQ_METHODS_FM_GET_MEMBER_INFO://获取成员信息
            case Protocal.CLOUD_REQ_METHODS_FM_GET_MEMBER_ID_LIST://获取成员id列表
            case Protocal.CLOUD_REQ_METHODS_FM_UPDATE_FAMILY://更新家庭基本信息
            case Protocal.CLOUD_REQ_METHODS_FM_GET_INVITATION://获取用户邀请函
            case Protocal.CLOUD_REQ_METHODS_FM_UPDATE_MEMBER://修改成员昵称
            case Protocal.CLOUD_REQ_METHODS_FM_UPDATE_MEMBER_RIGHTS://修改家庭成员权限
            case Protocal.CLOUD_REQ_METHODS_DL_UPGRADE_APP: // 查询APP升级信息
            case Protocal.CLOUD_REQ_METHODS_DL_UPGRADE_H5: // 查询H5升级信息
            case Protocal.CLOUD_REQ_METHODS_DL_REPORT_UPGRADE_STA: // APP升级状态上报
            case Protocal.CLOUD_METHODS_MG_GET_MSG_LIST://获取离线消息列表
            case Protocal.CLOUD_METHODS_MG_ACK_MSG://确认已读消息
            case Protocal.CLOUD_REQ_METHODS_ACC_CHECK_CLIENT_ONLINE://判断客户端是否在线
            case Protocal.CLOUD_REQ_METHODS_FM_GET_BACKGROUND_LIST://获取家庭背景图片
            case Protocal.CLOUD_REQ_METHODS_3D_GET_PRODUCT_GUIDE_INFO: // 获取产品的引导信息
            case Protocal.CLOUD_REQ_METHODS_3D_GET_PRODUCT_BY_URL://通过二维码链接获取引导页信息
            case Protocal.CLOUD_REQ_METHODS_DM_GET_DEV_ADDTYPE: // 添加方式设置信息查询
            case Protocal.CLOUD_REQ_METHODS_3D_GET_DISTRIBUTOR_LIST: // 获取渠道商列表
            case Protocal.ROUTER_REQ_METHODS_DM_GET_DEV_PRODUCT_LIST: // 获取产品列表
            case Protocal.CLOUD_REQ_METHODS_3D_GET_KEY_PROPERTY://获取设备属性
                 if (cloudChannelOnline && cloudLogin) {
                     strategyResult.setMsg_type(MSG_TYPE.TYPE_REQ_CLOUD);
                 } else {
                     strategyResult.setErrorCode(ErrorCode.ERROR_CODE_91010);
                     strategyResult.setExpectedTarget(AbstractRouteStrategy.TARGET_CLOUD);
                 }
                 break;
            //只走路由器(判断通道可达和登录状态)
            case Protocal.ROUTER_REQ_METHODS_DM_ADD_ROOM://添加房间
            case Protocal.ROUTER_REQ_METHODS_DM_DEL_ROOM://删除房间
            case Protocal.ROUTER_REQ_METHODS_DM_UPDATE_ROOM://修改房间名称
            case Protocal.ROUTER_REQ_METHODS_DM_UPDATE_ROOM_ORDER://修改房间排序
            case Protocal.CLOUD_REQ_METHODS_DM_UPDATE_DEVICE: // 修改设备名称
            case Protocal.CLOUD_REQ_METHODS_DM_MOVE_DEVICES: // 转移设备到新房间
            case Protocal.ROUTER_REQ_METHODS_DM_UPDATE_SHORTCUT: // 修改总开关绑定设备
            case Protocal.ROUTER_REQ_METHODS_DM_GET_LIGHT_CONTROL_DEVICES: // 获取全屋灯控制设备列表
            case Protocal.CLOUD_REQ_METHODS_DM_ADD_DEVICE: // 添加子设备
            case Protocal.CLOUD_REQ_METHODS_DM_ADD_DEVICE_ABORT: // 取消添加子设备
            case Protocal.CLOUD_REQ_METHODS_DM_DEL_DEVICE: // 删除子设备
            case Protocal.ROUTER_REQ_METHODS_DM_SORT_SHORTCUT: // 快捷方式排序
            case Protocal.ROUTER_REQ_METHODS_DM_UPDATE_SHORTCUT_VISIBLE://设置快捷方式可见状态
            case Protocal.ROUTER_REQ_METHODS_DM_ADD_SHORTCUT: // 添加快捷方式
            case Protocal.ROUTER_REQ_METHODS_DM_DEL_SHORTCUT: // 删除快捷方式
            case Protocal.ROUTER_REQ_METHODS_DM_UNBIND_ROUTER://路由器端解绑路由器
            case Protocal.ROUTER_REQ_METHODS_DM_CALL_LIFT://呼叫电梯,只能在连接路由器的状态下才能操作
            case Protocal.CLOUD_REQ_METHODS_DM_UPDATE_SUBSWC:
            case Protocal.CLOUD_REQ_METHODS_DM_ADD_CUSTOMSWC:
            case Protocal.CLOUD_REQ_METHODS_DM_ADD_DEVICESWC:
            case Protocal.CLOUD_REQ_METHODS_DM_DEL_SUBSWC:
            case Protocal.CLOUD_REQ_METHODS_DM_UPDATE_SUBSWC_VISIBLE:
            case Protocal.CLOUD_REQ_METHODS_DM_SORT_SUBSWC:
                 if (routerChannelOnline && routerLogin) {
                     strategyResult.setMsg_type(MSG_TYPE.TYPE_REQ_ROUTER);
                 } else {
                     strategyResult.setErrorCode(ErrorCode.ERROR_CODE_91011);
                     strategyResult.setExpectedTarget(AbstractRouteStrategy.TARGET_ROUTER);
                 }
                 break;
            //优先路由器(判断通道可达和登录状态)
            case Protocal.CLOUD_REQ_METHODS_DM_GET_ROOM_LIST://获取房间列表
            case Protocal.CLOUD_REQ_METHODS_DM_GET_DEVICES_BY_FAMILY: // 按家庭查询设备列表
            case Protocal.CLOUD_REQ_METHODS_DM_GET_DEVICES_BY_ROOM: // 按房间查询设备列表
            case Protocal.ROUTER_REQ_METHODS_DM_GET_SHORTCUT_FILTER: // 获取设备属性过滤条件列表
            case Protocal.ROUTER_REQ_METHODS_DM_GET_SHORTCUT_MODE: // 是否已经设置主页快捷方式
            case Protocal.ROUTER_REQ_METHODS_DM_SET_SHORTCUT_MODE: // 开启/关闭房间主页快捷方式
            case Protocal.ROUTER_REQ_METHODS_DM_GET_FAMILY_DEV_TYPE_LIST: // 获取当前家庭已有设备的品类
            case Protocal.ROUTER_REQ_METHODS_DM_GET_DEV_TYPE_LIST: // 获取设备品类
                 if (routerChannelOnline && routerLogin) {
                     strategyResult.setMsg_type(MSG_TYPE.TYPE_REQ_ROUTER);
                 } else if (cloudChannelOnline && cloudLogin) {
                     strategyResult.setErrorCode(ErrorCode.ERROR_CODE_91011);
                     strategyResult.setMsg_type(MSG_TYPE.TYPE_REQ_CLOUD);
                 }

                 if (strategyResult.getMsg_type() == MSG_TYPE.TYPE_INVALID) {
                     strategyResult.setErrorCode(ErrorCode.ERROR_CODE_91012);
                 }
                 break;
            //优先云端(判断通道可达和登录状态)
            case Protocal.CLOUD_REQ_METHODS_UM_GET_USER_PROFILE://获取用户信息
            case Protocal.CLOUD_REQ_METHODS_FM_GET_FAMILY_INFO://获取家庭基本信息
            case Protocal.CLOUD_REQ_METHODS_FM_GET_MEMBER_LIST://获取成员信息列表
            case Protocal.CLOUD_REQ_METHODS_DA_GET_DEV_ALERT_LIST: // 获取设备告警列表
            case Protocal.CLOUD_REQ_METHODS_DR_GET_DEV_STATUS_LIST: // 获取设备状态列表
                 if (cloudChannelOnline && cloudLogin) {
                     strategyResult.setMsg_type(MSG_TYPE.TYPE_REQ_CLOUD);
                 } else if (routerChannelOnline && routerLogin) {
                     strategyResult.setErrorCode(ErrorCode.ERROR_CODE_91010);
                     strategyResult.setMsg_type(MSG_TYPE.TYPE_REQ_ROUTER);
                 }

                 if (strategyResult.getMsg_type() == MSG_TYPE.TYPE_INVALID) {
                     strategyResult.setErrorCode(ErrorCode.ERROR_CODE_91012);
                 }
                 break;
            //直接请求云端(只判断通道可达)
            case Protocal.CLOUD_REQ_METHODS_UM_RESET_PWD://修改密码
            case Protocal.CLOUD_REQ_METHODS_UM_LOGIN_PWD://密码登录
            case Protocal.CLOUD_REQ_METHODS_UM_AUTH://自动登录
            case Protocal.CLOUD_REQ_METHODS_UM_LOGIN_CODE://验证码登录
            case Protocal.CLOUD_REQ_METHODS_UM_GET_PHONE_CODE://获取手机验证码
            case Protocal.CLOUD_REQ_METHODS_UM_CHECK_CODE://校验手机验证码
            case Protocal.CLOUD_REQ_METHODS_UM_REGISTER_USER://用户注册
            case Protocal.CLOUD_REQ_METHODS_GET_MOJI_WEATHER: // 获取墨迹天气信息
                 if (cloudChannelOnline) {
                     strategyResult.setMsg_type(MSG_TYPE.TYPE_REQ_CLOUD);
                 } else {
                     strategyResult.setErrorCode(ErrorCode.ERROR_CODE_91010);
                     strategyResult.setExpectedTarget(AbstractRouteStrategy.TARGET_CLOUD);
                 }
                 break;
            //直接请求路由器(只判断通道可达)
            case Protocal.CLOUD_REQ_METHODS_UM_SYNC_PWD: // 同步密码
            case Protocal.ROUTER_REQ_METHODS_DM_VERIFY_ROUTER://校验路由器合法性
            case Protocal.ROUTER_REQ_METHODS_DM_BIND_ROUTER://路由器绑定路由器
                 if (routerChannelOnline) {
                     strategyResult.setMsg_type(MSG_TYPE.TYPE_REQ_ROUTER);
                 } else {
                     strategyResult.setErrorCode(ErrorCode.ERROR_CODE_91011);
                     strategyResult.setExpectedTarget(AbstractRouteStrategy.TARGET_ROUTER);
                 }
                 break;
            //优先走路由器，次选走云端且为透传
            case Protocal.ROUTER_REQ_METHODS_DM_GET:
            case Protocal.CLOUD_REQ_METHODS_DM_GET_DEVICE_INFO: // 获取设备信息
            case Protocal.ROUTER_REQ_METHODS_DM_SET: // wifi设备和ZigBee设备控制
            case Protocal.REQ_METHOD_DM_BIND_ABORD://取消绑定开关
            case Protocal.ROUTER_REQ_METHODS_DM_SET_LIGHT_CONTROL: // 开启/关闭全屋灯
            case Protocal.ROUTER_REQ_METHODS_DM_SET_TOTAL_CONTROL: // 开启/关闭总开关
            case Protocal.ROUTER_OR_DEVICE_REQ_METHODS_DM_GET_VERSION:// 查询路由器及子设备固件升级信息(单个)
            case Protocal.ROUTER_REQ_METHODS_DM_GET_VERSION: // 查询路由器及子设备固件升级信息
            case Protocal.ROUTER_REQ_METHODS_DM_SET_UPGRADE: // 设置路由器及子固件升级
            case Protocal.ROUTER_REQ_METHODS_SUBSCRIBE: // 路由器订阅消息
            case Protocal.VISUAL_TELCOM_METHOD_CALL://可视对讲呼叫
            case Protocal.VISUAL_TELCOM_METHOD_CALL_FROM_DOOR://可视对讲呼叫应答
            case Protocal.VISUAL_TELCOM_METHOD_ACCEPT://可视对讲接听
            case Protocal.VISUAL_TELCOM_METHOD_OPEN_DOOR://可视对讲开门
            case Protocal.VISUAL_TELCOM_METHOD_RELEASE://可视对讲挂断
            case Protocal.ROUTER_REQ_METHODS_DM_GET_RECORD_IPC://获取路由器硬盘上自动录制的录像
            case Protocal.ROUTER_REQ_METHODS_DM_DM_CALL_IPC://请求IPC 资源
            case Protocal.ROUTER_REQ_METHODS_DM_DM_MUTE_IPC://请求IPC 静音
            case Protocal.ROUTER_REQ_METHODS_DM_DM_RELEASE_IPC://释放IPC 资源
            case Protocal.ROUTER_REQ_METHODS_DM_DM_QUALITY_IPC://设置IPC 视频流的质量
            case Protocal.ROUTER_REQ_METHOD_DM_CALL_PROPERTY://呼叫物业
            case Protocal.ROUTER_REQ_METHOD_DM_RELEASE_PROPERTY://呼叫物业释放接口
            case Protocal.ROUTER_REQ_METHODS_DM_GET_SHORTCUT_LIST: // 获取快捷方式列表
            case Protocal.CLOUD_REQ_METHOD_DM_VERIFY_DEVICE_PWD://智能门锁验证管理员密码
            case Protocal.CLOUD_REQ_METHOD_DM_ADD_DEVICE_USER://智能门锁添加用户
            case Protocal.CLOUD_REQ_METHOD_DM_DEL_DEVICES_USER://智能门锁删除用户
            case Protocal.CLOUD_REQ_METHOD_DM_UPDATE_DEVICE_USER://智能门锁更新用户
            case Protocal.CLOUD_REQ_METHODS_DM_GET_SUB_SWITCH:
                 if (routerChannelOnline && routerLogin) {
                     strategyResult.setMsg_type(MSG_TYPE.TYPE_REQ_ROUTER);
                 } else if (cloudChannelOnline && cloudLogin) {
                     strategyResult.setErrorCode(ErrorCode.ERROR_CODE_91011);
                     strategyResult.setMsg_type(MSG_TYPE.TYPE_REQ_CLOUD_TRANS);
                 }

                 if (strategyResult.getMsg_type() == MSG_TYPE.TYPE_INVALID) {
                     strategyResult.setErrorCode(ErrorCode.ERROR_CODE_91012);
                 }
                 break;
            default:
                BHLog.i(TAG, "getStrategy: can't find the strategy by method");
                break;
        }

        return strategyResult;
    }

    /**
     * 决策业务请求超时时间
     * */
    public int decideMsgTimeout(String method) {
        int timeout = RequestParams.DEFAULT_NETWORKMSG_TIMEOUT;

        switch (method) {
            case Protocal.CLOUD_REQ_METHODS_UM_GET_OTHER_LOGIN_CLIENTS://获取其他登录的终端列表
            case Protocal.CLOUD_REQ_METHODS_UM_KICKOFF_CLIENT://踢出其他终端用户
            case Protocal.CLOUD_REQ_METHODS_FM_GET_FAMILY_LIST://获取家庭列表
            case Protocal.CLOUD_REQ_METHODS_DA_GET_DEV_ALERT_LIST: // 获取设备告警列表
            case Protocal.CLOUD_REQ_METHODS_DR_GET_DEV_STATUS_LIST: // 获取设备状态列表
            case Protocal.CLOUD_REQ_METHODS_UM_UPDATE_USER_PROFILE://更新用户资料
            case Protocal.CLOUD_REQ_METHODS_FM_CREATE_FAMILY://创建家庭
            case Protocal.CLOUD_REQ_METHODS_UM_GET_USER_ACCOUNT://获取用户帐户信息
            case Protocal.CLOUD_REQ_METHODS_FM_CREATE_MEMBER://创建家庭成员
            case Protocal.CLOUD_REQ_METHODS_FM_REMOVE_MEMBER://删除家庭成员
            case Protocal.CLOUD_REQ_METHODS_FM_EXIT_FAMILY://退出家庭
            case Protocal.CLOUD_REQ_METHODS_FM_GET_MEMBER_INFO://获取成员信息
            case Protocal.CLOUD_REQ_METHODS_FM_GET_MEMBER_ID_LIST://获取成员id列表
            case Protocal.CLOUD_REQ_METHODS_FM_UPDATE_FAMILY://更新家庭基本信息
            case Protocal.CLOUD_REQ_METHODS_FM_UPDATE_MEMBER://修改成员昵称
            case Protocal.CLOUD_REQ_METHODS_DL_UPGRADE_APP: // 查询APP升级信息
            case Protocal.ROUTER_REQ_METHODS_DM_ADD_ROOM://添加房间
            case Protocal.ROUTER_REQ_METHODS_DM_DEL_ROOM://删除房间
            case Protocal.ROUTER_REQ_METHODS_DM_UPDATE_ROOM://修改房间名称
            case Protocal.ROUTER_REQ_METHODS_DM_UPDATE_ROOM_ORDER://修改房间排序
            case Protocal.CLOUD_REQ_METHODS_DM_UPDATE_DEVICE: // 修改设备名称
            case Protocal.CLOUD_REQ_METHODS_DM_DEL_DEVICE: // 删除子设备
            case Protocal.CLOUD_REQ_METHODS_DM_ADD_DEVICE_ABORT: // 取消添加子设备
            case Protocal.ROUTER_REQ_METHODS_DM_ADD_SHORTCUT: // 添加快捷方式
            case Protocal.ROUTER_REQ_METHODS_DM_DEL_SHORTCUT: // 删除快捷方式
            case Protocal.ROUTER_REQ_METHODS_DM_SET_LIGHT_CONTROL: // 开启/关闭全屋灯
            case Protocal.ROUTER_REQ_METHODS_DM_SET_TOTAL_CONTROL: // 开启/关闭总开关
            case Protocal.CLOUD_REQ_METHODS_UM_GET_USER_PROFILE://获取用户信息
            case Protocal.CLOUD_REQ_METHODS_FM_GET_FAMILY_INFO://获取家庭基本信息
            case Protocal.CLOUD_REQ_METHODS_FM_GET_MEMBER_LIST://获取成员信息列表
            case Protocal.ROUTER_REQ_METHODS_DM_GET_DEV_PRODUCT_LIST: // 获取产品列表
            case Protocal.CLOUD_REQ_METHODS_3D_GET_PRODUCT_GUIDE_INFO: // 获取产品的引导信息
            case Protocal.CLOUD_REQ_METHODS_3D_GET_PRODUCT_BY_URL: //通过二维码链接获取产品信息
            case Protocal.CLOUD_REQ_METHODS_DM_GET_DEVICE_INFO: // 获取设备信息
            case Protocal.ROUTER_REQ_METHODS_DM_GET_VERSION: // 查询路由器及子设备固件升级信息
            case Protocal.ROUTER_OR_DEVICE_REQ_METHODS_DM_GET_VERSION: // 查询路由器及子设备固件升级信息（个人）
            case Protocal.CLOUD_REQ_METHODS_ACC_CHECK_CLIENT_ONLINE://判断客户端是否在线
            case Protocal.ROUTER_REQ_METHODS_DM_VERIFY_ROUTER://校验路由器合法性
            case Protocal.CLOUD_REQ_METHODS_DM_GET_ROOM_LIST://获取房间列表
            case Protocal.CLOUD_REQ_METHODS_DM_GET_DEVICES_BY_FAMILY: // 按家庭查询设备列表
            case Protocal.CLOUD_REQ_METHODS_DM_GET_DEVICES_BY_ROOM: // 按房间查询设备列表
            case Protocal.ROUTER_REQ_METHODS_DM_GET_FAMILY_DEV_TYPE_LIST: // 获取当前家庭已有设备的品类
            case Protocal.ROUTER_REQ_METHODS_DM_SORT_SHORTCUT: // 快捷方式排序
            case Protocal.CLOUD_REQ_METHODS_DM_GET_DEV_ADDTYPE: // 添加方式设置信息查询
            case Protocal.CLOUD_REQ_METHODS_3D_GET_DISTRIBUTOR_LIST: // 获取渠道商列表
            case Protocal.CLOUD_REQ_METHOD_DM_VERIFY_DEVICE_PWD://智能门锁验证管理员密码
            case Protocal.CLOUD_REQ_METHOD_DM_ADD_DEVICE_USER://智能门锁添加用户
            case Protocal.CLOUD_REQ_METHOD_DM_DEL_DEVICES_USER://智能门锁删除用户
            case Protocal.CLOUD_REQ_METHOD_DM_UPDATE_DEVICE_USER://智能门锁更新用户
                 timeout = RequestParams.SHORT_NETWORKMSG_TIMEOUT;
                 break;

            case Protocal.CLOUD_REQ_METHODS_UM_RESET_PWD://修改密码
            case Protocal.CLOUD_REQ_METHODS_UM_SET_PWD://设置密码
            case Protocal.CLOUD_REQ_METHODS_FM_BIND_ROUTER://云端绑定路由器
            case Protocal.CLOUD_REQ_METHODS_FM_UNBIND_ROUTER://云端解绑路由器
            case Protocal.CLOUD_REQ_METHODS_ACC_LOGOUT://退出登录
            case Protocal.CLOUD_REQ_METHODS_FM_GET_APPLY_CODE://获取用户申请码
            case Protocal.CLOUD_REQ_METHODS_FM_GET_INVITATION://获取用户邀请函
            case Protocal.CLOUD_REQ_METHODS_DM_MOVE_DEVICES: // 转移设备到新房间
            case Protocal.ROUTER_REQ_METHODS_DM_UPDATE_SHORTCUT: // 修改总开关绑定设备
            case Protocal.ROUTER_REQ_METHODS_DM_GET_LIGHT_CONTROL_DEVICES: // 获取全屋灯控制设备列表
            case Protocal.CLOUD_REQ_METHODS_UM_LOGIN_PWD://密码登录
            case Protocal.CLOUD_REQ_METHODS_UM_AUTH://自动登录
            case Protocal.CLOUD_REQ_METHODS_UM_LOGIN_CODE://验证码登录
            case Protocal.CLOUD_REQ_METHODS_UM_GET_PHONE_CODE://获取手机验证码
            case Protocal.CLOUD_REQ_METHODS_UM_CHECK_CODE://校验手机验证码
            case Protocal.CLOUD_REQ_METHODS_UM_REGISTER_USER://用户注册
            case Protocal.ROUTER_REQ_METHODS_DM_UNBIND_ROUTER://路由器端解绑路由器
            case Protocal.ROUTER_REQ_METHODS_DM_GET_SHORTCUT_FILTER: // 获取设备属性过滤条件列表
            case Protocal.ROUTER_REQ_METHODS_DM_GET_SHORTCUT_MODE: // 是否已经设置主页快捷方式
            case Protocal.ROUTER_REQ_METHODS_DM_SET_SHORTCUT_MODE: // 开启/关闭房间主页快捷方式
            case Protocal.CLOUD_REQ_METHODS_UM_SYNC_PWD: // 同步密码
            case Protocal.ROUTER_REQ_METHODS_DM_BIND_ROUTER://路由器绑定路由器
            case Protocal.ROUTER_REQ_METHODS_DM_SET_UPGRADE: // 设置路由器及子固件升级
            case Protocal.ROUTER_REQ_METHODS_SUBSCRIBE: // 路由器订阅消息
            case Protocal.ROUTER_REQ_METHODS_DM_GET: //
            case Protocal.ROUTER_REQ_METHODS_DM_GET_SHORTCUT_LIST: // 获取快捷方式列表
            case Protocal.CLOUD_REQ_METHODS_GET_MOJI_WEATHER: // 获取墨迹天气信息
            case Protocal.ROUTER_REQ_METHODS_DM_GET_DEV_TYPE_LIST: // 获取设备品类
                 timeout = RequestParams.NORMAL_NETWORKMSG_TIMEOUT;
                 break;

            case Protocal.CLOUD_REQ_METHODS_DM_ADD_DEVICE: // 添加子设备
                 timeout = RequestParams.LONG_NETWORKMSG_TIMEOUT;
                 break;

            default:
                 break;
        }

        return timeout;
    }

    public class StrategyResult {
        private MSG_TYPE msg_type;
        private int ErrorCode;
        private int mExpectedTarget = AbstractRouteStrategy.TARGET_AUTO;

        public StrategyResult() {
        }

        public StrategyResult(MSG_TYPE msg_type, int errorCode) {
            this.msg_type = msg_type;
            ErrorCode = errorCode;
        }

        public MSG_TYPE getMsg_type() {
            return msg_type;
        }

        public void setMsg_type(MSG_TYPE msg_type) {
            this.msg_type = msg_type;
        }

        public int getErrorCode() {
            return ErrorCode;
        }

        public void setErrorCode(int errorCode) {
            ErrorCode = errorCode;
        }

        public int getExpectedTarget() {
            return mExpectedTarget;
        }

        public void setExpectedTarget(int expectedTarget) {
            mExpectedTarget = expectedTarget;
        }
    }
}
