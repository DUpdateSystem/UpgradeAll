package net.xzos.UpgradeAll;

public class UpdateCard {
    private int databaseId;
    private String name;
    private String version;
    private String url;
    private String api;

    UpdateCard(int databaseId, String name, String url, String api) {
        this.databaseId = databaseId;
        this.name = name;
        this.url = url;
        this.api = api;
    }

    public int getDatabaseId() {
        return databaseId;
    }
    public String getName() {
        return name;
    }

    String getApi() {
        return api;
    }

    String getUrl() {
        return url;
    }

}
