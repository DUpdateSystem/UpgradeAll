package net.xzos.UpgradeAll.server.app.engine.api;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.xzos.UpgradeAll.server.ServerContainer;
import net.xzos.UpgradeAll.server.log.LogUtil;

import org.jetbrains.annotations.Contract;
import org.json.JSONObject;

import java.util.Calendar;

public abstract class EngineApi implements CoreApi {

    protected static final LogUtil Log = ServerContainer.AppServer.getLog();

    private Calendar renewTime;

    public abstract void refreshData();

    public void setRenewTime() {
        this.renewTime = Calendar.getInstance();
    }

    @Nullable
    public Calendar getRenewTime() {
        return renewTime;
    }

    @NonNull
    @Contract(" -> new")
    public static EmptyEngine getEmptyEngine() {
        return new EmptyEngine();
    }
}

/**
 * 生成空 engine 避免错误
 * TODO: 0.1.0 前核实去除方法
 */
class EmptyEngine extends EngineApi {

    @Override
    public void refreshData() {
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
    public String getVersioning(int releaseNum) {
        return null;
    }

    @Override
    public JSONObject getReleaseDownload(int releaseNum) {
        return null;
    }
}
