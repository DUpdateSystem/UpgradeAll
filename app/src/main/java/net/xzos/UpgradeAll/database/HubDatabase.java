package net.xzos.UpgradeAll.database;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import net.xzos.UpgradeAll.gson.HubConfig;
import net.xzos.UpgradeAll.gson.HubItemExtraData;

import org.litepal.annotation.Column;
import org.litepal.crud.LitePalSupport;

public class HubDatabase extends LitePalSupport {
    private int id;
    private String name;
    @Column(unique = true)
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

    public void setUuid(@NonNull String uuid) {
        this.uuid = uuid.toLowerCase();
    }

    public HubItemExtraData getExtraData() {
        Gson gson = new Gson();
        try {
            return gson.fromJson(extra_data, HubItemExtraData.class);
        } catch (JsonSyntaxException e) {
            return null;
        }
    }

    public void setExtraData(HubItemExtraData extra_data) {
        Gson gson = new Gson();
        this.extra_data = gson.toJson(extra_data);
    }
}