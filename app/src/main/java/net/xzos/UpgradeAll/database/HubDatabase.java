package net.xzos.UpgradeAll.database;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import net.xzos.UpgradeAll.gson.HubConfig;

import org.litepal.crud.LitePalSupport;

public class HubDatabase extends LitePalSupport {
    private int id;
    private String name;
    private String uuid;
    private String repoConfig;

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

    public HubConfig getRepoConfig() {
        Gson gson = new Gson();
        try {
            return gson.fromJson(repoConfig, HubConfig.class);
        } catch (JsonSyntaxException e) {
            return null;
        }
    }

    public void setRepoConfig(HubConfig repoConfig) {
        Gson gson = new Gson();
        this.repoConfig = gson.toJson(repoConfig);
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid.toLowerCase();
    }
}