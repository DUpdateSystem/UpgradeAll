package net.xzos.UpgradeAll.server.JSEngine.api;

import androidx.annotation.NonNull;

import net.xzos.UpgradeAll.application.MyApplication;
import net.xzos.UpgradeAll.server.log.LogUtil;

import org.jetbrains.annotations.Contract;
import org.json.JSONObject;

import java.util.Calendar;

public abstract class EngineApi implements CoreApi {

    protected static final LogUtil Log = MyApplication.getServerContainer().getLog();

    private Calendar renewTime = Calendar.getInstance();

    public abstract boolean refreshData();

    public boolean isSuccessFlash() {
        return getReleaseNum() != 0;
    }

    public void setRenewTime() {
        this.renewTime = Calendar.getInstance();
    }

    public Calendar getRenewTime() {
        return renewTime;
    }

    @NonNull
    @Contract(" -> new")
    public static EmptyEngine getEmptyEngine() {
        return new EmptyEngine();
    }
}

class EmptyEngine extends EngineApi {

    @Override
    public boolean refreshData() {
        return false;
    }

    @Override
    public String getDefaultName() {
        return null;
    }

    @Override
    public int getReleaseNum() {
        return 0;
    }

    @Override
    public String getVersionNumber(int releaseNum) {
        return null;
    }

    @Override
    public JSONObject getReleaseDownload(int releaseNum) {
        return null;
    }
}
