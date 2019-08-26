package net.xzos.UpgradeAll.server.hub;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import net.xzos.UpgradeAll.database.HubDatabase;
import net.xzos.UpgradeAll.gson.HubConfig;
import net.xzos.UpgradeAll.gson.HubItemExtraData;
import net.xzos.UpgradeAll.server.ServerContainer;
import net.xzos.UpgradeAll.server.log.LogUtil;

import org.jetbrains.annotations.Contract;
import org.litepal.LitePal;

import java.util.List;

public class HubManager {

    private static final LogUtil Log = ServerContainer.AppServer.getLog();

    private static final String TAG = "HubManager";
    private static final String[] LogObjectTag = {"Core", TAG};

    @Contract("_, null, _ -> false; _, !null, null -> false")
    public static boolean addHubDatabase(int databaseId, HubConfig hubConfigGson, String jsCode) {
        if (hubConfigGson != null && jsCode != null) {
            String name = null;
            String uuid = null;
            try {
                name = hubConfigGson.getInfo().getHubName();
                uuid = hubConfigGson.getUuid();
            } catch (NullPointerException e) {
                Gson gson = new Gson();
                Log.e(LogObjectTag, TAG, "addHubDatabase: 请确认 hubConfig 包含各个必须元素 hubConfigGson: " + gson.toJson(hubConfigGson));
            }
            // 如果设置了名字与 UUID，则存入数据库
            if (name != null && uuid != null) {
                // 修改数据库
                HubDatabase hubDatabase = LitePal.find(HubDatabase.class, databaseId);
                if (hubDatabase == null) {
                    LitePal.deleteAll(HubDatabase.class, "uuid = ?", uuid);
                    hubDatabase = new HubDatabase();
                }
                // 开启数据库
                hubDatabase.setName(name);
                hubDatabase.setUuid(uuid);
                hubDatabase.setHubConfig(hubConfigGson);
                // 存储 js 代码
                HubItemExtraData hubItemExtraData = new HubItemExtraData();
                hubItemExtraData.setJavascript(jsCode);
                hubDatabase.setExtraData(hubItemExtraData);
                hubDatabase.save(); // 将数据存入 HubDatabase 数据库
                return true;
            }
        }
        return false;
    }

    public static HubConfig getHubConfigByDatabaseId(int databaseId) {
        HubDatabase hubDatabase = LitePal.find(HubDatabase.class, databaseId);
        return hubDatabase.getHubConfig();
    }

    public static String getHubJsCodeByUuid(String uuid) {
        String jsCode = null;
        List<HubDatabase> hubDatabases = LitePal.where("uuid = ?", uuid).find(HubDatabase.class);
        if (hubDatabases.size() != 0) {
            HubDatabase hubDatabase = hubDatabases.get(0);
            jsCode = getJsCodeFromHubDatabaseItem(hubDatabase);
        }
        return jsCode;
    }

    public static String getHubJsCodeByDatabaseId(int databaseId) {
        HubDatabase hubDatabase = LitePal.find(HubDatabase.class, databaseId);
        return getJsCodeFromHubDatabaseItem(hubDatabase);
    }

    private static String getJsCodeFromHubDatabaseItem(@NonNull HubDatabase hubDatabase) {
        return hubDatabase.getExtraData().getJavascript();
    }
}
