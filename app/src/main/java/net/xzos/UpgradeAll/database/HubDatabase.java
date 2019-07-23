package net.xzos.UpgradeAll.database;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import net.xzos.UpgradeAll.gson.HubConfig;

import org.json.JSONException;
import org.json.JSONObject;
import org.litepal.crud.LitePalSupport;

public class HubDatabase extends LitePalSupport {
    private int id;
    private String name;
    private String uuid;
    private String hub_config;
    private String extra_data;  // JSON形式 存储JavaScript 和 额外数据

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public HubConfig getHubConfig() {
        Gson gson = new Gson();
        try {
            return gson.fromJson(hub_config, HubConfig.class);
        } catch (JsonSyntaxException e) {
            return null;
        }
    }

    public void setHubConfig(HubConfig repo_config) {
        Gson gson = new Gson();
        this.hub_config = gson.toJson(repo_config);
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid.toLowerCase();
    }

    public JSONObject getExtraData() {
        try {
            return new JSONObject(extra_data);
        } catch (JSONException e) {
            return null;
        }
    }

    public void setExtraData(JSONObject extra_data) {
        this.extra_data = extra_data.toString();
    }
}