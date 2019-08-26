package net.xzos.UpgradeAll.server.app.manager.api;

import androidx.annotation.NonNull;

import net.xzos.UpgradeAll.database.RepoDatabase;
import net.xzos.UpgradeAll.utils.VersionChecker;

import org.json.JSONObject;
import org.litepal.LitePal;

public class App {

    private int appDatabaseId;
    private Updater updater;

    public App(int appDatabaseId) {
        this.appDatabaseId = appDatabaseId;
        updater = new Updater(appDatabaseId);
    }

    public boolean isLatest() {
        String latestVersion = updater.getLatestVersion();
        String installedVersion = getInstalledVersion();
        return VersionChecker.compareVersionNumber(installedVersion, latestVersion);
    }

    public String getInstalledVersion() {
        // 获取已安装版本号
        VersionChecker versionChecker = getVersionChecker();
        return versionChecker.getVersion();
    }

    @NonNull
    private VersionChecker getVersionChecker() {
        RepoDatabase repoDatabase = LitePal.find(RepoDatabase.class, appDatabaseId);
        JSONObject versionCheckerJsonObject = repoDatabase.getVersionChecker();
        // 获取数据库 VersionChecker 数据
        return new VersionChecker(versionCheckerJsonObject);
    }

    public Updater getUpdater() {
        return updater;
    }
}
