package net.xzos.UpgradeAll.gson;

public class HubConfig {
    /**
     * base_version : 1
     * uuid :
     * info : {"hub_name":"","config_version":1}
     * web_crawler : {"tool":"","file_path":""}
     */

    private int base_version;
    private String uuid;
    private InfoBean info;
    private WebCrawlerBean web_crawler;

    public int getBaseVersion() {
        return base_version;
    }

    public void setBaseVersion(int base_version) {
        this.base_version = base_version;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public InfoBean getInfo() {
        return info;
    }

    public void setInfo(InfoBean info) {
        this.info = info;
    }

    public WebCrawlerBean getWebCrawler() {
        return web_crawler;
    }

    public void setWebCrawler(WebCrawlerBean web_crawler) {
        this.web_crawler = web_crawler;
    }

    public static class InfoBean {
        /**
         * hub_name :
         * config_version : 1
         */

        private String hub_name;
        private int config_version;

        public String getHubName() {
            return hub_name;
        }

        public void setHubName(String hub_nare) {
            this.hub_name = hub_nare;
        }

        public int getConfigVersion() {
            return config_version;
        }

        public void setConfigVersion(int config_version) {
            this.config_version = config_version;
        }
    }

    public static class WebCrawlerBean {
        /**
         * tool :
         * file_path :
         */

        private String tool;
        private String file_path;

        public String getTool() {
            return tool;
        }

        public void setTool(String tool) {
            this.tool = tool;
        }

        public String getFilePath() {
            return file_path;
        }

        public void setFilePath(String file_path) {
            this.file_path = file_path;
        }
    }
}