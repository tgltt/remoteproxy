package cn.evergrande.it.hdremoteproxylib;

import android.content.Context;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import cn.evergrande.it.hdnetworklib.network.common.model.RemoteEventMsg;
import cn.evergrande.it.hdremoteproxylib.AbstractRemote.IConnectionMonitor;
import cn.evergrande.it.hdremoteproxylib.AbstractRemote.RemoteStatus;
import cn.evergrande.it.hdremoteproxylib.evaluate.NetworkEvaluator;
import cn.evergrande.it.hdremoteproxylib.interfaces.IMdpMessageHandler;
import cn.evergrande.it.hdremoteproxylib.network.data.actions.AbstractAction;
import cn.evergrande.it.hdremoteproxylib.network.data.actions.ScreenOnAction;
import cn.evergrande.it.hdremoteproxylib.network.data.actions.UserPresentAction;
import cn.evergrande.it.hdremoteproxylib.receiver.NetworkChangedReceiver;
import cn.evergrande.it.hdremoteproxylib.utils.ScreenObserver;
import cn.evergrande.it.logger.BHLog;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class HdNetworkMonitorService {

    private final static String TAG = "HdNetworkMonitorService";

    public static final String ACTION = "cn.evergrande.it.pad.service.PollingService";

    public static final String FLAG_TASK = "tast_flag";

    public static final int CATCH_MDP = 1;
    public static final int HEARTBEAT = 3;
    // 缺省心跳发送间隔：5分钟，单位：秒
    public static final long HEART_BEAT_PERIOD = 5 * 60 * 1000;

    private volatile Looper mServiceLooper;
    private volatile ServiceHandler mServiceHandler;

    private Context mContext;

    private IMdpMessageHandler sMdpMessageHandler;

    private NetworkChangedReceiver mNetWorkStateReceiver;

    private RemoteConnectionMonitor mRemoteConnectionMonitor;

    private ScreenObserver mScreenObserver;
    private ScreenStateMonitor mScreenStateMonitor;

    /**
     * 单位：s
     */
    private long mHeartbeatPeriod = HEART_BEAT_PERIOD;

    private boolean mEnableHeartbeat;
    private long mLastSendHeartBeatTime;

    private static class SingleInstanceHolder {
        static HdNetworkMonitorService sHdNetworkMonitorService = new HdNetworkMonitorService();
    }

    public static HdNetworkMonitorService getInstance() {
        return SingleInstanceHolder.sHdNetworkMonitorService;
    }

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CATCH_MDP:
                     dealMdp(msg.obj);
                     break;
                case HEARTBEAT:
                     doSendHeartbeat();
                     break;
            }
        }
    }

    private void dealMdp(Object params) {
        if (params == null || !(params instanceof RemoteEventMsg)) {
            BHLog.i(TAG, "dealMdp: params is null, or not a RemoteEventMsg");
        }

        RemoteEventMsg event = (RemoteEventMsg) params;

        if (sMdpMessageHandler != null) {
            sMdpMessageHandler.dealMDP(event);
        }
    }

    private HdNetworkMonitorService() {
    }

    /**
     * 网络监听服务，包括长连接心跳监听，以及Mdp消息接收处理
     * @param context Context上下文
     * @param heartbeatPeriod 心跳间隔，单位：秒
     */
    public void init(Context context, long heartbeatPeriod) {
        mContext = context;
        mHeartbeatPeriod = heartbeatPeriod * 1000;

        RemoteProxy remoteProxy = RemoteProxy.getInstance();

        HandlerThread thread = remoteProxy.getHandlerThread();
        if (thread == null) {
            throw new IllegalStateException("RemoteProxy state abnormal");
        }

        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
        // 初始化Socket连接状态监听器
        mRemoteConnectionMonitor = new RemoteConnectionMonitor();
        //注册网络层的网络连接状态广播
        mNetWorkStateReceiver = new NetworkChangedReceiver();

        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

        context.registerReceiver(mNetWorkStateReceiver, filter);
        // 立即发送心跳检测
        sendMSG(HEARTBEAT, null, 0);
        // 初始化屏幕监控器
        mScreenStateMonitor = new ScreenStateMonitor();

        mScreenObserver = new ScreenObserver(context);
        mScreenObserver.registerScreenStateListener(mScreenStateMonitor);
        mScreenObserver.startObserver();
    }

    /*
     * 当前网络模块是全局变量的方式存在的。建议是放在本service。
     */
    @Subscribe(threadMode = ThreadMode.POSTING) //在ui线程执行
    public void catchMdpEventMsg(RemoteEventMsg event) {
        sendMSG(CATCH_MDP, event, 0);
    }

    /**
     * 发送Tcp心跳
     */
    private void doSendHeartbeat() {
        if (!mEnableHeartbeat) {
            BHLog.i(TAG, "doSendHeartbeat: mEnableHeartbeat=false");
            return;
        }

        BHLog.i(TAG, "doSendHeartbeat: active");

        Map<Integer, AbstractRemote> mapRemotes = RemoteProxy.getInstance().getRemotes();
        if (mapRemotes == null || mapRemotes.isEmpty()) {
            BHLog.i(TAG, "sendTcpHeartbeat: mapRemotes null or empty");
            return;
        }

        boolean hasConnectedRemote = false;
        // 遍历Remote集合以发送心跳
        Set<Integer> keySet = mapRemotes.keySet();
        for (Iterator<Integer> it = keySet.iterator() ; it.hasNext() ;) {
            Integer key = it.next();
            AbstractRemote remote = mapRemotes.get(key);
            if (remote == null) {
                BHLog.i(TAG, String.format("doSendHeartbeat: remote(keyId=%s) is null", key));
                continue;
            }
            // 记录Remote状态
            RemoteStatus remoteStatus = remote.getRemoteStatus();
            if (remoteStatus != null && remoteStatus.isConnected()) {
                hasConnectedRemote = true;
            }

            BHLog.i(String.format("doSendHeartbeat: %s(keyId=%s) is sending heart beat", remote, key));
            // 发送Remote心跳
            remote.sendHeartbeat();
        }
        // 记录本次发送心跳时间
        mLastSendHeartBeatTime = System.currentTimeMillis();
        // 如果没有Connected的Remote, 则停止发送定时心跳
        if (!hasConnectedRemote) {
            BHLog.i(TAG, "doSendHeartbeat: no connected remote, then stop send heartbeat");
            return;
        }
        // mHeartbeatPeriod秒后, 发送下次的心跳
        sendMSG(HEARTBEAT, null, mHeartbeatPeriod);
    }

    /**
     * 释放网络模块资源
     */
    public void releaseHdNetwork() {
        BHLog.i(TAG, "releaseHdNetwork");

        RemoteProxy remoteProxy = RemoteProxy.getInstance();

        AbstractRemote cloudRemote = remoteProxy.getRemote(RemoteProxy.REMOTE_CLOUD);
        AbstractRemote routeRemote = remoteProxy.getRemote(RemoteProxy.REMOTE_ROUTE);

        mContext.unregisterReceiver(mNetWorkStateReceiver);
        unregisterMdpMsgHandler();

        mServiceLooper.quit();

        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
        // 将缓存写入到文件中
        BHLog.flushCacheLogToFile();

        mScreenObserver.unregisterScreenStateListener(mScreenStateMonitor);
        mScreenObserver.shutdownObserver();
    }

    /**
     * 获取上次发送心跳的时间, 单位：ms
     */
    public long getLastSendHeartBeatTime() {
        return mLastSendHeartBeatTime;
    }

    /**
     * 发送心跳的间隔时间, 单位：ms
     */
    public long getHeartbeatPeriod() {
        return mHeartbeatPeriod;
    }

    /**
     * 触发TcpConnection心跳机制, 因为心跳机制在Tcp长连接断开后会被暂停, 重新被激活地场景有:
     * 1) 网络恢复连接;
     * 2) 网络保持连接, 并且Tcp长连接重新建立
     */
    public void invokeTcpConnectionHeartbeat() {
        setEnableHeartbeat(true);
        sendMSG(HEARTBEAT, null, mHeartbeatPeriod);
    }

    /**
     * 停止Tcp长连接心跳
     */
    public void stopTcpConnectionHeartbeat() {
        setEnableHeartbeat(false);

        if (mServiceHandler != null) {
            BHLog.i(TAG, "stopTcpConnectionHeartbeat: Removing pending HEARTBEAT messages...");
            mServiceHandler.removeMessages(HEARTBEAT);
        }
    }

    private void setEnableHeartbeat(boolean enableHeartbeat) {
        if (enableHeartbeat) {
            BHLog.i(TAG, "setEnableHeartbeat: enabling heartbeat");
        } else {
            BHLog.i(TAG, "setEnableHeartbeat: disabling heartbeat");
        }

        mEnableHeartbeat = enableHeartbeat;
    }

    private void sendMSG(int what, Object param, long delay) {
        if (mServiceHandler.hasMessages(what)) {
            BHLog.i(TAG, String.format("sendMSG: already has message(what=%d)", what));
            return;
        }

        Message msg = mServiceHandler.obtainMessage();
        msg.what = what;
        msg.obj = param;

        mServiceHandler.sendMessageDelayed(msg, delay);
    }

    public RemoteConnectionMonitor getRemoteConnectionMonitor() {
        return mRemoteConnectionMonitor;
    }

    public void registerMdpMsgHandler(IMdpMessageHandler mdpMessageHandler) {
        sMdpMessageHandler = mdpMessageHandler;
    }

    public void unregisterMdpMsgHandler() {
        sMdpMessageHandler = null;
    }

    public void notifyScreenStateChanged(int event) {
        if (mScreenObserver == null) {
            BHLog.i(TAG, String.format("notifyScreenStateChanged: mScreenObserver is null, ignoring event(%d)", event));
            return;
        }

        mScreenObserver.notifyScreenStateListener(event);
    }

    private class RemoteConnectionMonitor implements IConnectionMonitor {

        @Override
        public void onNetworkChanged(int connStatus, int netType, AbstractRemote remote) {

        }

        @Override
        public void onSocketStatusChanged(int socketStatus, AbstractRemote remote) {
            if (remote == null) {
                BHLog.i(TAG, "RemoteConnectionMonitor:onSocketStatusChanged: remote is null");
                return;
            }

            if (socketStatus == SOCKET_CONNECTION_CONNECTED) {
                BHLog.i(TAG, String.format("Remote(%s) is connected, restartup sendHeartbeat", remote.getClass().getCanonicalName()));
                // mHeartbeatPeriod秒后，发送下次的心跳
                sendMSG(HEARTBEAT, null, mHeartbeatPeriod);
            }
        }
    }

    private class ScreenStateMonitor implements ScreenObserver.ScreenStateListener {

        @Override
        public void onScreenOn() {
            AbstractAction action = new ScreenOnAction(ActionManager.ACTION_SCREEN_ON, ActionManager.SUB_ACTION_UNDEFINED, null);
            ActionManager.getInstance().postActionDelayed(action, 0l, true);
        }

        @Override
        public void onScreenOff() {

        }

        @Override
        public void onUserPresent() {
            AbstractAction action = new UserPresentAction(ActionManager.ACTION_USER_PRESENT, ActionManager.SUB_ACTION_UNDEFINED, null);
            ActionManager.getInstance().postActionDelayed(action, 0l, true);
        }
    }
}
