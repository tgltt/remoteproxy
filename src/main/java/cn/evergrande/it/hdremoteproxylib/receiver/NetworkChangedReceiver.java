package cn.evergrande.it.hdremoteproxylib.receiver;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;

import cn.evergrande.it.hdremoteproxylib.RemoteConnector;
import cn.evergrande.it.hdremoteproxylib.RemoteProxy;
import cn.evergrande.it.hdremoteproxylib.NetworkStatusHandler;
import cn.evergrande.it.hdremoteproxylib.evaluate.NetworkEvaluator;
import cn.evergrande.it.logger.BHLog;

/**
 * Created by wangyuhang@evergrande.cn on 2017/7/12 0012.
 * <p>
 * 负责监听网络状态的变化
 * Android API为M及以上时，需要动态注册监听
 */
public class NetworkChangedReceiver extends BroadcastReceiver {
    private static final String TAG = "NetworkChangedReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        BHLog.i(TAG, "onReceive: Network connection state has changed");
        handleConnectStateChange(context);
    }

    /**
     * 获取连接状态，并进行处理
     */
    private void handleConnectStateChange(Context context) {
        BHLog.i(TAG, String.format("handleConnectStateChange: Current API level %d", android.os.Build.VERSION.SDK_INT));
        //检测API是不是小于21，因为到了API 21之后getNetworkInfo(int networkType)方法被弃用
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
            dealConnectStateBeforeLOLLIPOP(context);
        } else {
            dealConnectStateEqualAfterLOLLIPOP(context);
        }
    }

    private void dealConnectStateBeforeLOLLIPOP(Context context) {
        //获得ConnectivityManager对象
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        //获取ConnectivityManager对象对应的NetworkInfo对象
        //获取WIFI连接的信息
        NetworkInfo wifiNetworkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        //获取移动数据连接的信息
        NetworkInfo dataNetworkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        if (wifiNetworkInfo.isConnected() && dataNetworkInfo.isConnected()) {
            doWifiConnectedAndDataNetworkConnectedOld(context);
        } else if (wifiNetworkInfo.isConnected() && !dataNetworkInfo.isConnected()) {
            doWifiConnectedAndDataNetworkNotConnectedOld(context);
        } else if (!wifiNetworkInfo.isConnected() && dataNetworkInfo.isConnected()) {
            doWifiNotConnectedAndDataNetworkConnectedOld(context);
        } else {
            doWifiNotConnectedAndDataNetworkNotConnectedOld(context);
        }
    }

    @TargetApi(21)
    private int getNetworkCondition(Context context) {
        //获得ConnectivityManager对象
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        //获取所有当前已有连接上状态的网络连接的信息
        Network[] networks = connMgr.getAllNetworks();
        //用于记录最后的网络连接信息

        BHLog.i(TAG, "getNetworkCondition： networks.length: " + networks.length);

        int networkCondition = 0;//mobile false = 1, mobile true = 2, wifi = 4
        //通过循环将网络信息逐个取出来
        for (int i = 0; i < networks.length; i++) {
            //获取ConnectivityManager对象对应的NetworkInfo对象
            NetworkInfo networkInfo = connMgr.getNetworkInfo(networks[i]);

            if (networkInfo == null) {
                break;
            }

            //检测到有数据连接，但是并连接状态未生效，此种状态为wifi和数据同时已连接，以wifi连接优先
            if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE && !networkInfo.isConnected()) {
                networkCondition += 1;
            }

            //检测到有数据连接，并连接状态已生效，此种状态为只有数据连接，wifi并未连接上
            if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE && networkInfo.isConnected()) {
                networkCondition += 2;
            }

            //检测到有wifi连接，连接状态必为true
            if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                networkCondition += 4;
            }
        }

        return networkCondition;
    }

    /**
     * Wifi未连接、Mobile未连接
     * @param context
     */
    private void doWifiNotConnectedAndDataNetworkNotConnectedNew(Context context) {
        BHLog.i(TAG, "doWifiNotConnectedAndDataNetworkNotConnectedNew： WIFI disconnected, Mobile disconnected");
        NetworkEvaluator.getInstance().stat("", "NetworkState: WIFI disconnected, Mobile disconnected", true);

        reportConnChange(context, RemoteProxy.NETWORK_UNKNOWN);

        RemoteProxy remoteProxy = RemoteProxy.getInstance();
        if (!remoteProxy.hasInit()) {
            BHLog.i(TAG, "doWifiNotConnectedAndDataNetworkNotConnectedNew： RemoteProxy not init, abandom this message");
            return;
        }

        remoteProxy.disconnectAll();
        remoteProxy.setCurNetworkEnv(RemoteProxy.NETWORK_UNKNOWN);//更新当前网络状态的记录标识变量
        remoteProxy.disableRemotes();
    }

    /**
     * Wifi未连接、Mobile已连接
     * @param context
     */
    private void doWifiNotConnectedAndDataNetworkConnectedNew(Context context) {
        BHLog.i(TAG, "doWifiNotConnectedAndDataNetworkConnectedNew： WIFI disconnected, Mobile connected");
        NetworkEvaluator.getInstance().stat("", "NetworkState: WIFI disconnected, Mobile connected", true);

        reportConnChange(context, RemoteProxy.NETWORK_MOBILE);

        RemoteProxy remoteProxy = RemoteProxy.getInstance();
        if (!remoteProxy.hasInit()) {
            BHLog.i(TAG, "doWifiNotConnectedAndDataNetworkConnectedNew： RemoteProxy not init, abandom this message");
            return;
        }

        remoteProxy.enableRemotes();
        remoteProxy.setCurNetworkEnv(RemoteProxy.NETWORK_MOBILE); //更新当前网络状态的记录标识变量

        RemoteProxy.getInstance().connect(RemoteProxy.REMOTE_CLOUD);
    }

    /**
     * Wifi已连接、Mobile未连接
     * @param context
     */
    private void doWifiConnectedAndDataNetworkNotConnectedNew(Context context) {
        BHLog.i(TAG, "doWifiConnectedAndDataNetworkNotConnectedNew： WIFI connected, Mobile disconnected");
        NetworkEvaluator.getInstance().stat("", "NetworkState: WIFI connected, Mobile disconnected", true);

        reportConnChange(context, RemoteProxy.NETWORK_WIFI);

        RemoteProxy remoteProxy = RemoteProxy.getInstance();
        if (!remoteProxy.hasInit()) {
            BHLog.i(TAG, "doWifiConnectedAndDataNetworkNotConnectedNew： RemoteProxy not init, abandom this message");
            return;
        }

        remoteProxy.enableRemotes();
        remoteProxy.setCurNetworkEnv(RemoteProxy.NETWORK_WIFI); //更新当前网络状态的记录标识变量

        remoteProxy.updateRouterGatewayIp(context);//更新路由器网关地址
        remoteProxy.connect(RemoteProxy.REMOTE_ALL);
    }

    /**
     * Wifi已连接、Mobile已连接
     * @param context
     */
    private void doWifiConnectedAndDataNetworkConnectedNew(Context context) {
        BHLog.i(TAG, "doWifiConnectedAndDataNetworkConnectedNew: WIFI connected, Mobile connected");
        NetworkEvaluator.getInstance().stat("", "NetworkState: WIFI connected, Mobile connected", true);

        reportConnChange(context, RemoteProxy.NETWORK_WIFI);

        RemoteProxy remoteProxy = RemoteProxy.getInstance();
        if (!remoteProxy.hasInit()) {
            BHLog.i(TAG, "doWifiConnectedAndDataNetworkConnectedNew： RemoteProxy not init, abandom this message");
            return;
        }

        remoteProxy.enableRemotes();
        remoteProxy.setCurNetworkEnv(RemoteProxy.NETWORK_WIFI); //更新当前网络状态的记录标识变量

        remoteProxy.updateRouterGatewayIp(context);//更新路由器网关地址
        remoteProxy.connect(RemoteProxy.REMOTE_ALL);
    }

    private void dealConnectStateEqualAfterLOLLIPOP(Context context) {
        int networkCondition = getNetworkCondition(context);
        //因为存在上述情况的组合情况，以组合相加的唯一值作为最终状态的判断
        switch (networkCondition) {
            case 0: // Wifi未连接、Mobile未连接
                 doWifiNotConnectedAndDataNetworkNotConnectedNew(context);
                 break;
            case 2: // Wifi未连接、Mobile已连接
                 doWifiNotConnectedAndDataNetworkConnectedNew(context);
                 break;
            case 4: // Wifi连接、Mobile未连接
                 doWifiConnectedAndDataNetworkNotConnectedNew(context);
                 break;
            case 5: // Wifi已连接、Mobile已连接
                 doWifiConnectedAndDataNetworkConnectedNew(context);
                 break;
        }
    }

    /**
     * Wifi已连接、Mobile已连接
     * @param context
     */
    private void doWifiConnectedAndDataNetworkConnectedOld(Context context) {
        BHLog.i(TAG, "doWifiConnectedAndDataNetworkConnectedOld: WIFI connected, Mobile connected");
        NetworkEvaluator.getInstance().stat("", "NetworkState: WIFI connected, Mobile connected", true);

        reportConnChange(context, RemoteProxy.NETWORK_WIFI);
        //更新路由器网关地址
        RemoteProxy remoteProxy = RemoteProxy.getInstance();
        if (!remoteProxy.hasInit()) {
            BHLog.i(TAG, "doWifiConnectedAndDataNetworkConnectedOld： RemoteProxy not init, abandom this message");
            return;
        }

        remoteProxy.setCurNetworkEnv(RemoteProxy.NETWORK_WIFI); // 更新当前网络状态的记录标识变量
        remoteProxy.enableRemotes();

        remoteProxy.updateRouterGatewayIp(context);
        remoteProxy.connect(RemoteProxy.REMOTE_ALL);
    }

    /**
     * Wifi已连接、Mobile未连接
     * @param context
     */
    private void doWifiConnectedAndDataNetworkNotConnectedOld(Context context) {
        BHLog.i(TAG, "doWifiConnectedAndDataNetworkNotConnectedOld: WIFI connected, Mobile disconnected");
        NetworkEvaluator.getInstance().stat("", "NetworkState: WIFI connected, Mobile disconnected", true);

        reportConnChange(context, RemoteProxy.NETWORK_WIFI);

        RemoteProxy remoteProxy = RemoteProxy.getInstance();
        if (!remoteProxy.hasInit()) {
            BHLog.i(TAG, "doWifiConnectedAndDataNetworkNotConnectedOld： RemoteProxy not init, abandom this message");
            return;
        }

        remoteProxy.setCurNetworkEnv(RemoteProxy.NETWORK_WIFI); // 更新当前网络状态的记录标识变量
        remoteProxy.enableRemotes();

        remoteProxy.updateRouterGatewayIp(context);//更新路由器网关地址
        remoteProxy.connect(RemoteProxy.REMOTE_ALL);
    }

    /**
     * Wifi未连接、Mobile已连接
     * @param context
     */
    private void doWifiNotConnectedAndDataNetworkConnectedOld(Context context) {
        BHLog.i(TAG, "doWifiNotConnectedAndDataNetworkConnectedOld: WIFI disconnected, Mobile connected");
        NetworkEvaluator.getInstance().stat("", "NetworkState: WIFI disconnected, Mobile connected", true);

        reportConnChange(context, RemoteProxy.NETWORK_MOBILE);

        RemoteProxy remoteProxy = RemoteProxy.getInstance();
        if (!remoteProxy.hasInit()) {
            BHLog.i(TAG, "doWifiNotConnectedAndDataNetworkConnectedOld： RemoteProxy not init, abandom this message");
            return;
        }

        remoteProxy.setCurNetworkEnv(RemoteProxy.NETWORK_MOBILE); // 更新当前网络状态的记录标识变量
        remoteProxy.enableRemotes();

        remoteProxy.connect(RemoteProxy.REMOTE_CLOUD);
    }

    /**
     * Wifi未连接、Mobile未连接
     * @param context
     */
    private void doWifiNotConnectedAndDataNetworkNotConnectedOld(Context context) {
        BHLog.i(TAG, "doWifiNotConnectedAndDataNetworkNotConnectedOld: WIFI disconnected, Mobile disconnected");
        NetworkEvaluator.getInstance().stat("", "NetworkState: WIFI disconnected, Mobile disconnected", true);

        reportConnChange(context, RemoteProxy.NETWORK_UNKNOWN);

        RemoteProxy remoteProxy = RemoteProxy.getInstance();
        if (!remoteProxy.hasInit()) {
            BHLog.i(TAG, "doWifiNotConnectedAndDataNetworkNotConnectedOld： RemoteProxy not init, abandom this message");
            return;
        }

        remoteProxy.setCurNetworkEnv(RemoteProxy.NETWORK_UNKNOWN); // 更新当前网络状态的记录标识变量
        RemoteConnector.getInstance().disconnectAll();

        remoteProxy.disableRemotes();
    }

    /**
     * 上报网络连接状态变化至上层
     */
    private void reportConnChange(Context context, int state) {
        BHLog.i(TAG, String.format("reportConnChange: network state=%s", state));
        // 无论网络断开还是刚连接上, 此时的socket连接都视为断开
        int connStatus = RemoteProxy.NETWORK_STATUS_DISCONNECTED;
        switch (state) { // 判断网络和连接状态
            case RemoteProxy.NETWORK_WIFI:
                 connStatus = RemoteProxy.NETWORK_STATUS_CONNECTED;
                 break;
            case RemoteProxy.NETWORK_MOBILE:
                 connStatus = RemoteProxy.NETWORK_STATUS_CONNECTED;
                 break;
            case RemoteProxy.NETWORK_OTHER:
                 connStatus = RemoteProxy.NETWORK_STATUS_CONNECTED;
                 break;
            case RemoteProxy.NETWORK_UNKNOWN:
            default:
        }
        // 处理Remote状态
        NetworkStatusHandler.dealConnectionStatusChanged(connStatus, state, RemoteProxy.REMOTE_ALL);
    }

}
