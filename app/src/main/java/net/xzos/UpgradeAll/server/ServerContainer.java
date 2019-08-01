package net.xzos.UpgradeAll.server;

import net.xzos.UpgradeAll.server.log.LogUtil;
import net.xzos.UpgradeAll.server.updater.Updater;

import org.jetbrains.annotations.Contract;

public class ServerContainer {
    final private LogUtil Log = new LogUtil();
    final private Updater updater = new Updater(Log);

    @Contract(pure = true)
    public LogUtil getLog() {
        return Log;
    }

    @Contract(pure = true)
    public Updater getUpdater() {
        return updater;
    }
}
