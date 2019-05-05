package net.xzos.UpgradeAll;

public class upgradeCard {
    private int databaseId;
    private String name;
    private String version;
    private String url;
    private String api;

    upgradeCard(int databaseId, String name, String version, String url, String api) {
        this.databaseId = databaseId;
        this.name = name;
        this.version = version;
        this.url = url;
        this.api = api;
    }

    public int getDatabaseId() {
        return databaseId;
    }
    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    String getApi() {
        return api;
    }

    String getUrl() {
        return url;
    }

}
