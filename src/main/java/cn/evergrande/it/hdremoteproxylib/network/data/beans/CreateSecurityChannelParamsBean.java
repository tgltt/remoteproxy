package cn.evergrande.it.hdremoteproxylib.network.data.beans;

import android.os.Parcel;

import cn.evergrande.it.hdnetworklib.network.common.model.protocal.ParamsBean;

/**
 * Created by lxl on 2017/7/14.
 */

public class CreateSecurityChannelParamsBean extends ParamsBean {
    private String uuid;
    private String key;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(this.uuid);
        dest.writeString(this.key);
    }

    public CreateSecurityChannelParamsBean() {
    }

    protected CreateSecurityChannelParamsBean(Parcel in) {
        super(in);
        this.uuid = in.readString();
        this.key = in.readString();
    }

    public static final Creator<CreateSecurityChannelParamsBean> CREATOR = new Creator<CreateSecurityChannelParamsBean>() {
        @Override
        public CreateSecurityChannelParamsBean createFromParcel(Parcel source) {
            return new CreateSecurityChannelParamsBean(source);
        }

        @Override
        public CreateSecurityChannelParamsBean[] newArray(int size) {
            return new CreateSecurityChannelParamsBean[size];
        }
    };
}
