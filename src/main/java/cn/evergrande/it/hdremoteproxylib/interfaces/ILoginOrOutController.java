package cn.evergrande.it.hdremoteproxylib.interfaces;

/**
 * Created by tanguilong@evergrande.cn on 2018/6/19.
 */
public interface ILoginOrOutController {

    public void login(int remoteType, ILoginResult loginResult);

    public void loginOut(int remoteType, ILogoutResult logoutResult);
}
