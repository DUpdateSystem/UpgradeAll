package net.xzos.UpgradeAll.database;

import org.json.JSONException;
import org.json.JSONObject;
import org.litepal.crud.LitePalSupport;

public class HubDatabase extends LitePalSupport {
    private int id;
    private String name;
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

    public JSONObject getRepoConfig() {
        try {
            return new JSONObject(repoConfig);
        } catch (JSONException e) {
            return null;
        }
    }

    public void setRepoConfig(JSONObject repoConfig) {
        this.repoConfig = repoConfig.toString();
    }

}