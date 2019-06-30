package net.xzos.UpgradeAll.gson;

import java.util.List;

public class CloudConfig {
    /**
     * list_url : {"app_list_raw_url":"","hub_list_raw_url":""}
     * app_list : [{"package_name":""}]
     * hub_list : [{"hub_config_name":"","hub_config_uuid":""}]
     */

    private ListUrlBean list_url;
    private List<AppListBean> app_list;
    private List<HubListBean> hub_list;

    public ListUrlBean getListUrl() {
        return list_url;
    }

    public void setListUrl(ListUrlBean list_url) {
        this.list_url = list_url;
    }

    public List<AppListBean> getAppList() {
        return app_list;
    }

    public void setAppList(List<AppListBean> app_list) {
        this.app_list = app_list;
    }

    public List<HubListBean> getHubList() {
        return hub_list;
    }

    public void setHubList(List<HubListBean> hub_list) {
        this.hub_list = hub_list;
    }

    public static class ListUrlBean {
        /**
         * app_list_raw_url :
         * hub_list_raw_url :
         */

        private String app_list_raw_url;
        private String hub_list_raw_url;

        public String getAppListRawUrl() {
            return app_list_raw_url;
        }

        public void setAppListRawUrl(String app_list_url) {
            this.app_list_raw_url = app_list_url;
        }

        public String getHubListRawUrl() {
            return hub_list_raw_url;
        }

        public void setHubListRawUrl(String hub_list_url) {
            this.hub_list_raw_url = hub_list_url;
        }
    }

    public static class AppListBean {
        /**
         * package_name :
         */

        private String package_name;

        public String getPackageName() {
            return package_name;
        }

        public void setPackageName(String package_name) {
            this.package_name = package_name;
        }
    }

    public static class HubListBean {
        /**
         * hub_config_name :
         * hub_config_uuid :
         */

        private String hub_config_name;
        private String hub_config_uuid;

        public String getHubConfigName() {
            return hub_config_name;
        }

        public void setHubConfigName(String hub_config_name) {
            this.hub_config_name = hub_config_name;
        }

        public String getHubConfigUuid() {
            return hub_config_uuid;
        }

        public void setHubConfigUuid(String hub_config_uuid) {
            this.hub_config_uuid = hub_config_uuid;
        }
    }
}
