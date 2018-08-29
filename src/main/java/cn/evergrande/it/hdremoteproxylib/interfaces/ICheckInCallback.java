package cn.evergrande.it.hdremoteproxylib.interfaces;

/**
 * Created by wangyuhang@evergrande.cn on 2017/7/25.
 */

public interface ICheckInCallback {
    void onSuccess(CheckinResult checkinResult);
    void onError(int errorCode);

    public class CheckinResult {
        private String bizServerAddress;
        private String bizServerPort;

        public String getBizServerAddress() {
            return bizServerAddress;
        }

        public void setBizServerAddress(String bizServerAddress) {
            this.bizServerAddress = bizServerAddress;
        }

        public String getBizServerPort() {
            return bizServerPort;
        }

        public void setBizServerPort(String bizServerPort) {
            this.bizServerPort = bizServerPort;
        }
    }
}
