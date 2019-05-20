package net.xzos.UpgradeAll.viewmodels;

public class ItemCardView {
    private int databaseId;
    private String name;
    private String desc;
    private String api;

    public ItemCardView(int databaseId, String name, String desc, String api) {
        this.databaseId = databaseId;
        this.name = name;
        this.desc = desc;
        this.api = api;
    }

    public int getDatabaseId() {
        return databaseId;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public String getApi() {
        return api;
    }


}
