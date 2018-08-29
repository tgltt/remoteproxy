/**
 *****************************************************************************
 * Copyright (C) 2018-2023 HD Corporation. All rights reserved
 * File        : NetworkConfigLoader.java
 *
 * Description : NetworkConfigLoader,
 *
 * Creation    : 2018-06-13
 * Author      : tanguilong@evergrande.cn
 * History     : 2018-06-13, Creation
 *****************************************************************************
 */
package cn.evergrande.it.hdremoteproxylib.network;

import android.content.Context;

import cn.evergrande.it.logger.BHLog;
import cn.evergrande.it.hdremoteproxylib.utils.PropertyUtil;
import cn.evergrande.it.hdremoteproxylib.utils.StorageManager;

import java.io.File;
import java.util.List;
import java.util.Properties;

/**
 * 策略文件加载策略分四个层级： <br/>
 * 1. 内置sdcard中的配置文件； <br/>
 * 2. 外置sdcard中的配置文件； <br/>
 * 3. assets中的配置文件； <br/>
 * 4. 硬编码在代码中的配置信息 <br/>
 *  <br/>
 * 网络配置加载器依次按照以上顺序查找配置文件并加载配置信息, 只要命中一个配置文件并成功 <br/>
 * 加载配置信息, 加载配置信息流程即结束
 */
public class NetworkConfigLoader {

    public final static String TAG = "NetworkConfigLoader";

    public final static String CONFIG_FILE = "network.config";
    public final static String APP_DATA_DIR = "ControlPad";

    public final static String MODULE_NETWORK = "network";
    public final static String ASSET_NETWORK = "network";
    /**
     * Sec服务器地址、端口
     */
    public final static String PROPERTY_KEY_SEC_SERVER_URL  = "SEC_SERVER_URL";
    public final static String PROPERTY_KEY_SEC_SERVER_PORT = "SEC_SERVER_PORT";
    /**
     * LB服务器地址、端口
     */
    public final static String PROPERTY_KEY_LB_SERVER_URL  = "LB_SERVER_URL";
    public final static String PROPERTY_KEY_LB_SERVER_PORT = "LB_SERVER_PORT";
    /**
     * 通道心跳间隔，单位：s
     */
    public final static String PROPERTY_KEY_CHANNEL_HEARTBEAT_PERIOD = "CHANNEL_HEARTBEAT_PERIOD";
    /**
     * 云端通道心跳发送内容
     */
    public final static String PROPERTY_KEY_CLOUD_CHANNEL_HEARTBEAT_CONTENT = "CLOUD_CHANNEL_HEARTBEAT_CONTENT";
    /**
     * 路由器通道心跳发送内容
     */
    public final static String PROPERTY_KEY_ROUTER_CHANNEL_HEARTBEAT_CONTENT = "ROUTER_CHANNEL_HEARTBEAT_CONTENT";
    /**
     * 是否允许路由通道加密
     */
    public final static String PROPERTY_KEY_ENABLE_ROUTER_CHANNEL_ENCRYPT = "ENABLE_ROUTER_CHANNEL_ENCRYPT";

    /**
     * 评估文件存放路径及文件名称
     */
    public final static String PROPERTY_KEY_EVALUATE_FILE_PATH  = "EVALUATE_FILE_PATH";
    public final static String PROPERTY_KEY_EVALUATE_FILE_NAME = "EVALUATE_FILE_NAME";
    /**
     * 评估文件写入模式
     */
    public final static String PROPERTY_KEY_EVALUATE_FILE_WRITE_MODE = "EVALUATE_FILE_WRITE_MODE";

    /**
     * 加载网络模块所需的硬编码属性, 比如网址、域名等属性。 <br/>
     * 加载策略: 1、加载内部sdcard配置文件 <br/>
     *          2、加载外部sdcard配置文件 <br/>
     *          3、加载assets中的配置文件 <br/>
     */
    public static Properties loadNetworkConfig(Context ctx) {
        StorageManager storageManager = StorageManager.getInstance();

        String innerSdcardPath = storageManager.getInnerSDCardPath();
        String configFilePath = composePath(innerSdcardPath, APP_DATA_DIR, MODULE_NETWORK, CONFIG_FILE);

        Properties props = null;
        // 加载内部sdcard配置文件
        File fileConfig = new File(configFilePath);
        if (fileConfig.exists()) {
            props = PropertyUtil.loadConfigFromSdcard(fileConfig);
        }

        if (props != null && !props.isEmpty()) {
            BHLog.i(TAG, String.format("loadNetworkConfig: Loading %s", configFilePath));
            return props;
        }
        // 加载外部sdcard配置文件
        final List<String> outerSdcardPaths = storageManager.getExtSDCardPath();
        if (outerSdcardPaths != null && !outerSdcardPaths.isEmpty()) {
            for (String sdcardPath : outerSdcardPaths) {
                configFilePath = composePath(sdcardPath, APP_DATA_DIR, MODULE_NETWORK, CONFIG_FILE);

                fileConfig = new File(configFilePath);
                if (!fileConfig.exists()) {
                    continue;
                }

                props = PropertyUtil.loadConfigFromSdcard(fileConfig);

                if (props != null && !props.isEmpty()) {
                    BHLog.i(TAG, String.format("loadNetworkConfig: Loading %s", configFilePath));
                    return props;
                }
            }
        }
        // 加载raw中的配置文件
        props = PropertyUtil.loadConfigFromAsset(ctx, String.format("%s%s%s", ASSET_NETWORK, File.separator, CONFIG_FILE));

        return props;
    }

    private static String composePath(String sdcardPath, String appHomeDir, String module, String file) {
        return String.format("%s%s%s%s%s%s%s", sdcardPath, File.separator, appHomeDir, File.separator, module, File.separator, file);
    }
}
