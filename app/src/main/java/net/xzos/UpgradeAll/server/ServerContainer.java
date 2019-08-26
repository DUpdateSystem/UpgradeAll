package net.xzos.UpgradeAll.server;

import net.xzos.UpgradeAll.server.log.LogUtil;
import net.xzos.UpgradeAll.server.app.manager.AppManager;

import org.jetbrains.annotations.Contract;

public class ServerContainer {
    public static class AppServer {
        final private static LogUtil Log = new LogUtil();
        final private static AppManager AppManager = new AppManager();

        @Contract(pure = true)
        public static LogUtil getLog() {
            return Log;
        }

        @Contract(pure = true)
        public static AppManager getAppManager() {
            return AppManager;
        }
    }
}
