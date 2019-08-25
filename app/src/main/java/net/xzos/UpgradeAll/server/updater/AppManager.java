package net.xzos.UpgradeAll.server.updater;

import androidx.annotation.NonNull;

import net.xzos.UpgradeAll.database.RepoDatabase;
import net.xzos.UpgradeAll.server.log.LogUtil;
import net.xzos.UpgradeAll.utils.VersionChecker;

import org.jetbrains.annotations.Contract;
import org.json.JSONObject;
import org.litepal.LitePal;

public class AppManager {

    private Updater updater;

    public AppManager(LogUtil Log) {
        updater = new Updater(Log);
    }

    public boolean isLatest(int databaseId) {
        String latestVersion = updater.getLatestVersion(databaseId);
        String installedVersion = getInstalledVersion(databaseId);
        return VersionChecker.compareVersionNumber(installedVersion, latestVersion);
    }

    public String getInstalledVersion(int databaseId) {
        // 获取已安装版本号
        VersionChecker versionChecker = getVersionChecker(databaseId);
        return versionChecker.getVersion();
    }

    @NonNull
    @Contract("_ -> new")
    private VersionChecker getVersionChecker(int databaseId) {
        RepoDatabase repoDatabase = LitePal.find(RepoDatabase.class, databaseId);
        JSONObject versionCheckerJsonObject = repoDatabase.getVersionChecker();
        // 获取数据库 VersionChecker 数据
        return new VersionChecker(versionCheckerJsonObject);
    }

    public Updater getUpdater() {
        return updater;
    }
}
