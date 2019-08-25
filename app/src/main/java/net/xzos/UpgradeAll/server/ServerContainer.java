package net.xzos.UpgradeAll.server;

import net.xzos.UpgradeAll.server.log.LogUtil;
import net.xzos.UpgradeAll.server.updater.AppManager;

import org.jetbrains.annotations.Contract;

public class ServerContainer {
    final private static LogUtil Log = new LogUtil();
    final private static AppManager AppManager = new AppManager(Log);

    @Contract(pure = true)
    public LogUtil getLog() {
        return Log;
    }

    @Contract(pure = true)
    public AppManager getAppManager() {
        return AppManager;
    }
}
