package net.xzos.UpgradeAll.server.hub;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import net.xzos.UpgradeAll.database.HubDatabase;
import net.xzos.UpgradeAll.json.gson.HubConfig;
import net.xzos.UpgradeAll.json.gson.HubDatabaseExtraData;
import net.xzos.UpgradeAll.server.ServerContainer;
import net.xzos.UpgradeAll.server.log.LogUtil;

import org.jetbrains.annotations.Contract;
import org.litepal.LitePal;

import java.util.List;

public class HubManager {

    private static final LogUtil Log = ServerContainer.AppServer.getLog();

    private static final String TAG = "HubManager";
    private static final String[] LogObjectTag = {"Core", TAG};

    @Contract("null, _ -> false; !null, null -> false")
    public static boolean add(HubConfig hubConfigGson, String jsCode) {
        if (hubConfigGson != null && jsCode != null) {
            String name = null;
            String uuid = null;
            try {
                name = hubConfigGson.getInfo().getHubName();
                uuid = hubConfigGson.getUuid();
            } catch (NullPointerException e) {
                Gson gson = new Gson();
                Log.e(LogObjectTag, TAG, "add: 请确认 hubConfig 包含各个必须元素 hubConfigGson: " + gson.toJson(hubConfigGson));
            }
            // 如果设置了名字与 UUID，则存入数据库
            if (name != null && uuid != null) {
                // 修改数据库
                List<HubDatabase> hubDatabases = LitePal.where("uuid = ?", uuid).find(HubDatabase.class);
                HubDatabase hubDatabase;
                if (!hubDatabases.isEmpty())
                    hubDatabase = hubDatabases.get(0);
                else
                    hubDatabase = new HubDatabase();
                // 开启数据库
                hubDatabase.setName(name);
                hubDatabase.setUuid(uuid);
                hubDatabase.setHubConfig(hubConfigGson);
                // 存储 js 代码
                HubDatabaseExtraData hubDatabaseExtraData = new HubDatabaseExtraData();
                hubDatabaseExtraData.setJavascript(jsCode);
                hubDatabase.setExtraData(hubDatabaseExtraData);
                hubDatabase.save(); // 将数据存入 HubDatabase 数据库
                return true;
            }
        }
        return false;
    }

    public static void del(String uuid) {
        LitePal.deleteAll(HubDatabase.class, "uuid = ?", uuid);
    }

    public static List<HubDatabase> getDatabases() {
        return LitePal.findAll(HubDatabase.class);  // 读取 hub 数据库
    }

    public static HubDatabase getDatabase(String uuid) {
        List<HubDatabase> hubDatabases = LitePal.where("uuid = ?", uuid).find(HubDatabase.class);
        if (!hubDatabases.isEmpty())
            return hubDatabases.get(0);
        else
            return null;
    }

    public static String getJsCode(String uuid) {
        String jsCode = null;
        List<HubDatabase> hubDatabases = LitePal.where("uuid = ?", uuid).find(HubDatabase.class);
        if (hubDatabases.size() != 0) {
            HubDatabase hubDatabase = hubDatabases.get(0);
            jsCode = getJsCodeFromHubDatabaseItem(hubDatabase);
        }
        return jsCode;
    }

    private static String getJsCodeFromHubDatabaseItem(@NonNull HubDatabase hubDatabase) {
        return hubDatabase.getExtraData().getJavascript();
    }
}
