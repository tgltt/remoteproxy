/**
 *****************************************************************************
 * Copyright (C) 2018-2023 HD Corporation. All rights reserved
 * File        : ActionManager.java
 *
 * Description : ActionManager,
 *
 * Creation    : 2018-05-29
 * Author      : tanguilong@evergrande.cn
 * History     : 2018-05-29, Creation
 *****************************************************************************
 */
package cn.evergrande.it.hdremoteproxylib;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.StringBuilderPrinter;

import cn.evergrande.it.hdremoteproxylib.evaluate.NetworkEvaluator;
import cn.evergrande.it.hdremoteproxylib.network.data.actions.AbstractAction;
import cn.evergrande.it.logger.BHLog;

public class ActionManager {

    public final static String TAG = "ActionManager";

    /*ACTION常量含义*/
    /**
     * 空闲动作，不做任何操作
     */
    public final static int ACTION_IDLE              = 0;
    /**
     * 连接到云端
     */
    public final static int ACTION_CONNECT_CLOUD     = 1;
    /**
     * 连接到路由
     */
    public final static int ACTION_CONNECT_ROUTE     = 2;
    /**
     * 连接到日志服务器
     */
    public final static int ACTION_CONNECT_LOGGER    = 3;
    /**
     * 断开与云端的连接
     */
    public final static int ACTION_DISCONNECT_CLOUD  = 4;
    /**
     * 断开与路由器的连接
     */
    public final static int ACTION_DISCONNECT_ROUTE  = 5;
    /**
     * 断开与日志服务器的连接
     */
    public final static int ACTION_DISCONNECT_LOGGER = 6;
    /**
     * 重连接到云端
     */
    public final static int ACTION_RECONNECT_CLOUD   = 7;
    /**
     * 重连接到路由
     */
    public final static int ACTION_RECONNECT_ROUTE   = 8;
    /**
     * 重连接到日志服务器
     */
    public final static int ACTION_RECONNECT_LOGGER  = 9;
    /**
     * 屏幕被点亮
     */
    public final static int ACTION_SCREEN_ON  = 10;
    /**
     * 屏幕被灭屏
     */
    public final static int ACTION_SCREEN_OFF = 11;
    /**
     * 屏幕被解锁
     */
    public final static int ACTION_USER_PRESENT = 12;
    /**
     * 未定义子Action，用于以后扩展
     */
    public final static int SUB_ACTION_UNDEFINED = 0;

    private Handler mHandler;

    static class HOLDER {
        public static ActionManager sActionManager = new ActionManager();
    }

    public static ActionManager getInstance() {
        return HOLDER.sActionManager;
    }

    private ActionManager() {

    }

    /**
     * 初始化ActionManager
     */
    public void init() {
        HandlerThread thread = RemoteProxy.getInstance().getHandlerThread();
        if (thread == null) {
            throw new IllegalStateException("RemoteProxy state abnormal");
        }

        mHandler = new ActionHandler(thread.getLooper());
    }

    /**
     * 释放ActionManager所持有的资源
     */
    public void release() {
        mHandler = null;
    }

    /**
     * 发送指定动作到HandlerThread执行
     * @param action
     */
    public void postAction(AbstractAction action) {
        if (action == null) {
            BHLog.i(TAG, "postAction: action is null");
            return;
        }

        if (!checkStatus()) {
            BHLog.i(TAG, String.format("postAction: HandlerThread status ircorrect, abandom Action(action=%s", action));
            return;
        }

        Message message = mHandler.obtainMessage();
        message.what = action.getActionId();
        message.arg1 = action.getActionSubId();
        message.obj = action;

        mHandler.sendMessage(message);
    }

    /**
     * 发送指定动作到HandlerThread执行
     * @param action 待执行的action
     * @param delay 延迟时间，取值参考Handler的时间
     */
    public void postActionDelayed(AbstractAction action, long delay) {
        if (action == null) {
            BHLog.i(TAG, "postActionDelay: action is null");
            return;
        }

        if (!checkStatus()) {
            BHLog.i(TAG, String.format("postActionDelay: HandlerThread status ircorrect, abandom Action(action=%s", action));
            return;
        }

        Message message = mHandler.obtainMessage();
        message.what = action.getActionId();
        message.arg1 = action.getActionSubId();
        message.obj = action;

        mHandler.sendMessageDelayed(message, delay);
    }

    /**
     * 发送指定动作到HandlerThread执行
     * @param action 待执行的action
     * @param delay 延迟时间，取值参考Handler的时间
     * @param clearPreviousSameActionId false - 不清除屏幕唤醒抖动产生的重复事件
     *                                  true - 清除屏幕唤醒抖动产生的重复事件
     */
    public void postActionDelayed(AbstractAction action, long delay, boolean clearPreviousSameActionId) {
        if (action == null) {
            BHLog.i(TAG, "postActionDelay: action is null");
            return;
        }

        if (!checkStatus()) {
            BHLog.i(TAG, String.format("postActionDelay: HandlerThread status ircorrect, abandom Action(action=%s", action));
            return;
        }

        if (clearPreviousSameActionId) {
            mHandler.removeMessages(action.getActionId());
        }

        Message message = mHandler.obtainMessage();
        message.what = action.getActionId();
        message.arg1 = action.getActionSubId();
        message.obj = action;

        mHandler.sendMessageDelayed(message, delay);
    }

    private boolean checkStatus() {
        if (mHandler == null) {
            BHLog.i(TAG , "checkStatus: mHandler is null");
            return false;
        }

        Looper looper = mHandler.getLooper();
        if (looper == null) {
            BHLog.i(TAG , "checkStatus: looper is null");
            return false;
        }

        Thread handlerThread = looper.getThread();
        if (!handlerThread.isAlive()) {
            BHLog.i(TAG , "checkStatus: HandlerThread is not alive");
            return false;
        }

        return true;
    }

    /**
     * 清除HandlerThread消息队列指定类型消息
     * @param actionIds 待删除的消息类型ID
     */
    public void clearPreviousAction(int[] actionIds) {
        if (actionIds == null || actionIds.length <= 0) {
            BHLog.i(TAG, "clearPreviousAction: actionIds is null or empty");
            return;
        }

        if (mHandler == null) {
            BHLog.i(TAG, "clearPreviousAction: mHandler is null");
            return;
        }

        for (int actionId : actionIds) {
            mHandler.removeMessages(actionId);
        }
    }

    /**
     * 打印HandlerThread的消息队列
     * @param thread
     */
    public void dump(HandlerThread thread) {
        if (thread == null) {
            return;
        }

        Looper looper = thread.getLooper();
        if (looper == null) {
            return;
        }

        StringBuilder sbLog = new StringBuilder();
        StringBuilderPrinter stringBuilderPrinter = new StringBuilderPrinter(sbLog);

        looper.dump(stringBuilderPrinter, "");

        BHLog.i(TAG, "-------------- dump start --------------");
        BHLog.i(TAG, sbLog.toString());
        BHLog.i(TAG, "-------------- dump end --------------");
    }

}
