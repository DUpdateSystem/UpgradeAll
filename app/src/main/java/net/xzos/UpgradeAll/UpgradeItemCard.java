package net.xzos.UpgradeAll;

public class UpgradeItemCard {
    private String name;
    private String version;
    private String url;
    private String api;

    UpgradeItemCard(String name, String version, String url, String api) {
        this.name = name;
        this.version = version;
        this.url = url;
        this.api = api;
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
