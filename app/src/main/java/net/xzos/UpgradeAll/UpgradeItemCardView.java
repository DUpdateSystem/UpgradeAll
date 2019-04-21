package net.xzos.UpgradeAll;

public class UpgradeItemCardView {
    private String name;
    private String version;
    private String url;
    private String api;

    public UpgradeItemCardView(String name, String version, String url, String api) {
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

    public String getApi() {
        return api;
    }

    public String getUrl() {
        return url;
    }
}
