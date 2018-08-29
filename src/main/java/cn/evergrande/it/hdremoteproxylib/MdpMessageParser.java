package cn.evergrande.it.hdremoteproxylib;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import cn.evergrande.it.hdnetworklib.api.biz.GsonUtil;

import cn.evergrande.it.hdnetworklib.api.model.protocal.Protocal;
import cn.evergrande.it.hdnetworklib.network.common.model.RemoteEventMsg;
import cn.evergrande.it.hdnetworklib.network.common.model.protocal.CommonContentRes;
import cn.evergrande.it.hdnetworklib.network.tcp.TcpCallback;
import cn.evergrande.it.hdnetworklib.network.tcp.TcpClient;
import cn.evergrande.it.logger.BHLog;


import org.greenrobot.eventbus.EventBus;

import java.io.StringReader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by tam on 18-5-14.
 */
public class MdpMessageParser implements TcpClient.IMdpMessageParser {

    public final static String TAG = "MdpMessageParser";

    private static JsonParser sJsonParser = new JsonParser();

    private static Map<String, Long> dr_report_dev_status_timestamp_map = new ConcurrentHashMap<>();//设备状态上报特殊去重处理容器
    private static Map<String, Long> dr_report_upgrade_sta_timestamp_map = new ConcurrentHashMap<>();//版本更新特殊去重处理容器
    private static Map<String, Long> common_mdp_msp_method_timestamp_map = new ConcurrentHashMap<>();//普通的method去重

    /**
     * 处理远端推送消息
     */
    public void handleMessage(String data) {
        try {
            JsonObject rootNode;

            JsonReader reader = new JsonReader(new StringReader(data));
            reader.setLenient(true);
            rootNode = sJsonParser.parse(reader).getAsJsonObject();

            long mdpTimestamp = 0;
            JsonElement jeTimestamp = rootNode.get(Protocal.PARAMS_KEY_COMNON_TIMESTAMP);
            if (jeTimestamp != null) {
                mdpTimestamp = jeTimestamp.getAsLong();
            }

            int req_id = -1;
            JsonElement jeReqId = rootNode.get("req_id");
            if (jeReqId != null) {
                req_id = jeReqId.getAsInt();
            }

            JsonObject paramsNode = null;
            try {
                paramsNode = rootNode.getAsJsonObject("params");
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (paramsNode != null) {
                JsonObject contentNode = null;
                try {
                    contentNode = paramsNode.getAsJsonObject("content");
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (contentNode != null) {
                    String paramsString = paramsNode.toString();
                    String contentString = contentNode.toString();

                    String method = contentNode.get("method").getAsString();

                    BHLog.i(TAG, String.format("handleMessage: mdp_msg, method=%s, req_id=%d", method, req_id));

                    if (req_id == 0) {//被动mdp消息
                        if (mdpMsgFilter(mdpTimestamp, method, contentString)) {
                            BHLog.i(TAG, "handleMessage: mdp_msg has passed the mdp_msg filter");
                            EventBus.getDefault().post(new RemoteEventMsg(method, paramsString));
                        } else {
                            BHLog.i(TAG, "handleMessage: mdp_msg has not passed the mdp_msg filter");
                        }
                    } else {//mdp应答消息
                        try {
                            JsonReader contentReader = new JsonReader(new StringReader(contentString));
                            contentReader.setLenient(true);
                            CommonContentRes commonContentRes = new Gson().fromJson(contentReader, CommonContentRes.class);

                            //=============兼容问题处理============
                            JsonElement nodeidElem = contentNode.get("nodeid");
                            if (nodeidElem != null) {
                                String nodeId = nodeidElem.getAsString();
                                if (nodeId != null && method.equals("dm_set") && nodeId.equals("doorbell.main.async_call")) {
                                    if (mdpMsgFilter(mdpTimestamp, method, contentString)) {
                                        BHLog.i(TAG, "handleMessage: mdp_msg has passed the mdp_msg filter");
                                        EventBus.getDefault().post(new RemoteEventMsg(method, paramsString));
                                    } else {
                                        BHLog.i(TAG, "handleMessage: mdp_msg has not passed the mdp_msg filter");
                                    }
                                }
                            }
                            //=============兼容问题处理============

                            //查找对应req_id的请求callback，并处理回调
                            TcpCallback tcpCallback = TcpClient.getInstance().getReqTcpCallback(commonContentRes.getReq_id());
                            if (tcpCallback != null) {
                                tcpCallback.onSuccess(contentString);

                                //callback已处理，从容器中移除
                                TcpClient.getInstance().removeReqTcpCallback(commonContentRes.getReq_id());
                            }
                        } catch (Exception e) {
                            BHLog.e(TAG, e.getMessage(), e);
                        }
                    }
                } else {
                    EventBus.getDefault().post(new RemoteEventMsg(GsonUtil.getStringProp(Protocal.PARAMS_KEY_COMNON_METHOD, data), data));
                }
            } else {
                EventBus.getDefault().post(new RemoteEventMsg(GsonUtil.getStringProp(Protocal.PARAMS_KEY_COMNON_METHOD, data), data));
            }
        } catch (Exception e) {
            BHLog.e(TAG, e.getMessage(), e);

            EventBus.getDefault().post(new RemoteEventMsg(Protocal.LOCAL_NO_METHOD, data));
        }
    }

    /**
     * mdp_msg过滤器
     * @param mdpTimestamp mdp消息体中最外层的timestamp
     */
    private static boolean mdpMsgFilter(long mdpTimestamp, String method, String contentJson) {
        boolean result = true;

        BHLog.i(TAG, "[AbstractRemote] mdp_msg filter current method: " + method);

        try {
            if (method.equals(Protocal.REMOTE_REQ_METHODS_DR_REPORT_DEV_STATUS)) {
                JsonObject resultNode = GsonUtil.getResultJsonObj(contentJson);
                String deviceUUID = resultNode.get(Protocal.PARAMS_KEY_COMMON_DEVICE_UUID).getAsString();
                long optTimestamp = resultNode.get(Protocal.PARAMS_KEY_STATUS_MODIFIED_AT).getAsLong();

                BHLog.i(TAG, "[AbstractRemote] mdp_msg the current timestamp: " + optTimestamp);

                if (dr_report_dev_status_timestamp_map.containsKey(deviceUUID)) {
                    Long tempTimestamp = dr_report_dev_status_timestamp_map.get(deviceUUID);

                    BHLog.i(TAG, "[AbstractRemote] mdp_msg the last timestamp: " + tempTimestamp);

                    if (optTimestamp > tempTimestamp) {
                        dr_report_dev_status_timestamp_map.put(deviceUUID, optTimestamp);
                    } else {
                        result = false;
                    }
                } else {
                    dr_report_dev_status_timestamp_map.put(deviceUUID, optTimestamp);

                    BHLog.i(TAG, "[AbstractRemote] put a new mdp_msg timestamp tag: <" + deviceUUID + "," + optTimestamp + ">");
                }
            } else if (method.equals(Protocal.CLOUD_REQ_METHODS_DL_REPORT_UPGRADE_STA)) {
                JsonObject resultNode = GsonUtil.getJsonObjProp(Protocal.PARAMS_KEY_COMNON_PARAMS, contentJson);
                String deviceUUID = resultNode.get(Protocal.PARAMS_KEY_COMMON_UUID).getAsString();

                if (dr_report_upgrade_sta_timestamp_map.containsKey(deviceUUID)) {
                    Long tempTimestamp = dr_report_upgrade_sta_timestamp_map.get(deviceUUID);

                    BHLog.i(TAG, "[AbstractRemote] mdp_msg the last timestamp: " + tempTimestamp);

                    if (mdpTimestamp > tempTimestamp) {
                        dr_report_upgrade_sta_timestamp_map.put(deviceUUID, mdpTimestamp);
                    } else {
                        result = false;
                    }
                } else {
                    dr_report_upgrade_sta_timestamp_map.put(deviceUUID, mdpTimestamp);

                    BHLog.i(TAG, "[AbstractRemote] put a new mdp_msg timestamp tag: <" + deviceUUID + "," + mdpTimestamp + ">");
                }
            } else if (method.equals(Protocal.VISUAL_TELCOM_METHOD_CALL_FROM_DOOR)) {
                if (common_mdp_msp_method_timestamp_map.containsKey(method)) {
                    Long tempTimestamp = common_mdp_msp_method_timestamp_map.get(method);

                    BHLog.i(TAG, "[AbstractRemote] mdp_msg the last timestamp: " + tempTimestamp);

                    if (mdpTimestamp > tempTimestamp && mdpTimestamp - tempTimestamp >= 10) {
                        common_mdp_msp_method_timestamp_map.put(method, mdpTimestamp);
                    } else {
                        result = false;
                    }
                } else {
                    common_mdp_msp_method_timestamp_map.put(method, mdpTimestamp);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            BHLog.e(TAG, "[AbstractRemote] mdpMsgFilter exception: " + e.toString());
        }

        return result;
    }
}
