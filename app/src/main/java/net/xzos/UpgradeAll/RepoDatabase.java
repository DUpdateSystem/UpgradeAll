package net.xzos.UpgradeAll;

import org.litepal.crud.LitePalSupport;

public class RepoDatabase extends LitePalSupport {
    private int id;
    private String api;
    private String name;
    private String owner;
    private String repo;
    private String latest_tag;
    private String latest_release;
    // 为避免 tag 版本与 release 版本不一致
    private String installed_release;
    private String apiReturnData;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getApi() {
        return api;
    }

    public void setApi(String api) {
        this.api = api;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getRepo() {
        return repo;
    }

    public void setRepo(String repo) {
        this.repo = repo;
    }

    public String getLatestTag() {
        return latest_tag;
    }

    public void setLatestTag(String latest_tag) {
        this.latest_tag = latest_tag;
    }

    public String getLatestRelease() {
        return latest_release;
    }

    public void setLatestRelease(String latest_release) {
        this.latest_release = latest_release;
    }

    public String getInstalledRelease() {
        return installed_release;
    }

    public void setInstalledRelease(String installed_release) {
        this.installed_release = installed_release;
    }

    public String getApiReturnData() {
        return apiReturnData;
    }

    public void setApiReturnData(String apiReturnData) {
        this.apiReturnData = apiReturnData;
    }
}
