package cn.evergrande.it.hdremoteproxylib.interfaces;

/**
 * Created by hefeng on 2018/6/12.
 */
public interface ITCPController {

    void connect();

    void disconnect();

    void login();

    void loginOut();

    void activate();

    void deactivate();

    void sendEvent(int event, Object[] args);
}
