package net.xzos.UpgradeAll.server.hub;

import com.google.gson.Gson;

import net.xzos.UpgradeAll.application.MyApplication;
import net.xzos.UpgradeAll.database.HubDatabase;
import net.xzos.UpgradeAll.gson.HubConfig;
import net.xzos.UpgradeAll.server.log.LogUtil;

import org.json.JSONException;
import org.json.JSONObject;
import org.litepal.LitePal;

import java.util.List;

public class HubManager {

    private static final LogUtil Log = MyApplication.getServerContainer().getLog();

    private static final String TAG = "HubManager";
    private static final String[] LogObjectTag = {"Core", TAG};

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
                JSONObject extraData = new JSONObject();
                try {
                    extraData.put("javascript", jsCode);
                } catch (JSONException e) {
                    e.printStackTrace();
                    return false;
                }
                hubDatabase.setExtraData(extraData);
                hubDatabase.save(); // 将数据存入 HubDatabase 数据库
                return true;
            }
        }
        return false;
    }

    public String getHubJsCode(String uuid) {
        String jsCode = null;
        List<HubDatabase> hubDatabases = LitePal.where("uuid = ?", uuid).find(HubDatabase.class);
        if (hubDatabases.size() != 0) {
            HubDatabase hubDatabase = hubDatabases.get(0);
            JSONObject extraData = hubDatabase.getExtraData();
            try {
                jsCode = extraData.getString("javascript");
            } catch (JSONException e) {
                Log.e(LogObjectTag, TAG, "未取得 JS 代码，extraData: " + extraData);
                e.printStackTrace();
            }
        }
        return jsCode;
    }
}
