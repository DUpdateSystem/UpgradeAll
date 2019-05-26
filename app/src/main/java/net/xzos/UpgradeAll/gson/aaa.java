package net.xzos.UpgradeAll.gson;

import java.util.List;

public class aaa {
    /**
     * base_version : 1
     * uuid : 1c579ade-a595-4ad2-b540-b343a035e061
     * info : {"config_name":"魔趣","config_version":1}
     * web_crawler : {"tool":"HtmlUnit","user_agent":"","app_config":{"default_name":{"text":"","search_path":{"xpath":"//span[@class='card-title'][1]/b[1]/text()","regex":""}},"release":{"release_node":"//table[@class='bordered striped highlight scrollable-table']/tbody/tr","attribute":{"version_number":{"text":"","search_path":{"xpath":"//td[5]/text()","regex":"((((19|20)\\d{2})-(0?(1|[3-9])|1[012])-(0?[1-9]|[12]\\d|30))|(((19|20)\\d{2})-(0?[13578]|1[02])-31)|(((19|20)\\d{2})-0?2-(0?[1-9]|1\\d|2[0-8]))|((((19|20)([13579][26]|[2468][048]|0[48]))|(2000))-0?2-29))$"}},"assets":{"file_name":{"text":"","link_list":[{"delay":"","xpath":"","regex":""}]},"download_url":{"text":"","link_list":[{"delay":"","xpath":"","regex":""}]}}}}}}
     */

    private int base_version;
    private String uuid;
    private InfoBean info;
    private WebCrawlerBean web_crawler;

    public int getBase_version() {
        return base_version;
    }

    public void setBase_version(int base_version) {
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

    public WebCrawlerBean getWeb_crawler() {
        return web_crawler;
    }

    public void setWeb_crawler(WebCrawlerBean web_crawler) {
        this.web_crawler = web_crawler;
    }

    public static class InfoBean {
        /**
         * config_name : 魔趣
         * config_version : 1
         */

        private String config_name;
        private int config_version;

        public String getConfig_name() {
            return config_name;
        }

        public void setConfig_name(String config_name) {
            this.config_name = config_name;
        }

        public int getConfig_version() {
            return config_version;
        }

        public void setConfig_version(int config_version) {
            this.config_version = config_version;
        }
    }

    public static class WebCrawlerBean {
        /**
         * tool : HtmlUnit
         * user_agent :
         * app_config : {"default_name":{"text":"","search_path":{"xpath":"//span[@class='card-title'][1]/b[1]/text()","regex":""}},"release":{"release_node":"//table[@class='bordered striped highlight scrollable-table']/tbody/tr","attribute":{"version_number":{"text":"","search_path":{"xpath":"//td[5]/text()","regex":"((((19|20)\\d{2})-(0?(1|[3-9])|1[012])-(0?[1-9]|[12]\\d|30))|(((19|20)\\d{2})-(0?[13578]|1[02])-31)|(((19|20)\\d{2})-0?2-(0?[1-9]|1\\d|2[0-8]))|((((19|20)([13579][26]|[2468][048]|0[48]))|(2000))-0?2-29))$"}},"assets":{"file_name":{"text":"","link_list":[{"delay":"","xpath":"","regex":""}]},"download_url":{"text":"","link_list":[{"delay":"","xpath":"","regex":""}]}}}}}
         */

        private String tool;
        private String user_agent;
        private AppConfigBean app_config;

        public String getTool() {
            return tool;
        }

        public void setTool(String tool) {
            this.tool = tool;
        }

        public String getUser_agent() {
            return user_agent;
        }

        public void setUser_agent(String user_agent) {
            this.user_agent = user_agent;
        }

        public AppConfigBean getApp_config() {
            return app_config;
        }

        public void setApp_config(AppConfigBean app_config) {
            this.app_config = app_config;
        }

        public static class AppConfigBean {
            /**
             * default_name : {"text":"","search_path":{"xpath":"//span[@class='card-title'][1]/b[1]/text()","regex":""}}
             * release : {"release_node":"//table[@class='bordered striped highlight scrollable-table']/tbody/tr","attribute":{"version_number":{"text":"","search_path":{"xpath":"//td[5]/text()","regex":"((((19|20)\\d{2})-(0?(1|[3-9])|1[012])-(0?[1-9]|[12]\\d|30))|(((19|20)\\d{2})-(0?[13578]|1[02])-31)|(((19|20)\\d{2})-0?2-(0?[1-9]|1\\d|2[0-8]))|((((19|20)([13579][26]|[2468][048]|0[48]))|(2000))-0?2-29))$"}},"assets":{"file_name":{"text":"","link_list":[{"delay":"","xpath":"","regex":""}]},"download_url":{"text":"","link_list":[{"delay":"","xpath":"","regex":""}]}}}}
             */

            private DefaultNameBean default_name;
            private ReleaseBean release;

            public DefaultNameBean getDefault_name() {
                return default_name;
            }

            public void setDefault_name(DefaultNameBean default_name) {
                this.default_name = default_name;
            }

            public ReleaseBean getRelease() {
                return release;
            }

            public void setRelease(ReleaseBean release) {
                this.release = release;
            }

            public static class DefaultNameBean {
                /**
                 * text :
                 * search_path : {"xpath":"//span[@class='card-title'][1]/b[1]/text()","regex":""}
                 */

                private String text;
                private SearchPathBean search_path;

                public String getText() {
                    return text;
                }

                public void setText(String text) {
                    this.text = text;
                }

                public SearchPathBean getSearch_path() {
                    return search_path;
                }

                public void setSearch_path(SearchPathBean search_path) {
                    this.search_path = search_path;
                }

                public static class SearchPathBean {
                    /**
                     * xpath : //span[@class='card-title'][1]/b[1]/text()
                     * regex :
                     */

                    private String xpath;
                    private String regex;

                    public String getXpath() {
                        return xpath;
                    }

                    public void setXpath(String xpath) {
                        this.xpath = xpath;
                    }

                    public String getRegex() {
                        return regex;
                    }

                    public void setRegex(String regex) {
                        this.regex = regex;
                    }
                }
            }

            public static class ReleaseBean {
                /**
                 * release_node : //table[@class='bordered striped highlight scrollable-table']/tbody/tr
                 * attribute : {"version_number":{"text":"","search_path":{"xpath":"//td[5]/text()","regex":"((((19|20)\\d{2})-(0?(1|[3-9])|1[012])-(0?[1-9]|[12]\\d|30))|(((19|20)\\d{2})-(0?[13578]|1[02])-31)|(((19|20)\\d{2})-0?2-(0?[1-9]|1\\d|2[0-8]))|((((19|20)([13579][26]|[2468][048]|0[48]))|(2000))-0?2-29))$"}},"assets":{"file_name":{"text":"","link_list":[{"delay":"","xpath":"","regex":""}]},"download_url":{"text":"","link_list":[{"delay":"","xpath":"","regex":""}]}}}
                 */

                private String release_node;
                private AttributeBean attribute;

                public String getRelease_node() {
                    return release_node;
                }

                public void setRelease_node(String release_node) {
                    this.release_node = release_node;
                }

                public AttributeBean getAttribute() {
                    return attribute;
                }

                public void setAttribute(AttributeBean attribute) {
                    this.attribute = attribute;
                }

                public static class AttributeBean {
                    /**
                     * version_number : {"text":"","search_path":{"xpath":"//td[5]/text()","regex":"((((19|20)\\d{2})-(0?(1|[3-9])|1[012])-(0?[1-9]|[12]\\d|30))|(((19|20)\\d{2})-(0?[13578]|1[02])-31)|(((19|20)\\d{2})-0?2-(0?[1-9]|1\\d|2[0-8]))|((((19|20)([13579][26]|[2468][048]|0[48]))|(2000))-0?2-29))$"}}
                     * assets : {"file_name":{"text":"","link_list":[{"delay":"","xpath":"","regex":""}]},"download_url":{"text":"","link_list":[{"delay":"","xpath":"","regex":""}]}}
                     */

                    private VersionNumberBean version_number;
                    private AssetsBean assets;

                    public VersionNumberBean getVersion_number() {
                        return version_number;
                    }

                    public void setVersion_number(VersionNumberBean version_number) {
                        this.version_number = version_number;
                    }

                    public AssetsBean getAssets() {
                        return assets;
                    }

                    public void setAssets(AssetsBean assets) {
                        this.assets = assets;
                    }

                    public static class VersionNumberBean {
                        /**
                         * text :
                         * search_path : {"xpath":"//td[5]/text()","regex":"((((19|20)\\d{2})-(0?(1|[3-9])|1[012])-(0?[1-9]|[12]\\d|30))|(((19|20)\\d{2})-(0?[13578]|1[02])-31)|(((19|20)\\d{2})-0?2-(0?[1-9]|1\\d|2[0-8]))|((((19|20)([13579][26]|[2468][048]|0[48]))|(2000))-0?2-29))$"}
                         */

                        private String text;
                        private SearchPathBeanX search_path;

                        public String getText() {
                            return text;
                        }

                        public void setText(String text) {
                            this.text = text;
                        }

                        public SearchPathBeanX getSearch_path() {
                            return search_path;
                        }

                        public void setSearch_path(SearchPathBeanX search_path) {
                            this.search_path = search_path;
                        }

                        public static class SearchPathBeanX {
                            /**
                             * xpath : //td[5]/text()
                             * regex : ((((19|20)\d{2})-(0?(1|[3-9])|1[012])-(0?[1-9]|[12]\d|30))|(((19|20)\d{2})-(0?[13578]|1[02])-31)|(((19|20)\d{2})-0?2-(0?[1-9]|1\d|2[0-8]))|((((19|20)([13579][26]|[2468][048]|0[48]))|(2000))-0?2-29))$
                             */

                            private String xpath;
                            private String regex;

                            public String getXpath() {
                                return xpath;
                            }

                            public void setXpath(String xpath) {
                                this.xpath = xpath;
                            }

                            public String getRegex() {
                                return regex;
                            }

                            public void setRegex(String regex) {
                                this.regex = regex;
                            }
                        }
                    }

                    public static class AssetsBean {
                        /**
                         * file_name : {"text":"","link_list":[{"delay":"","xpath":"","regex":""}]}
                         * download_url : {"text":"","link_list":[{"delay":"","xpath":"","regex":""}]}
                         */

                        private FileNameBean file_name;
                        private DownloadUrlBean download_url;

                        public FileNameBean getFile_name() {
                            return file_name;
                        }

                        public void setFile_name(FileNameBean file_name) {
                            this.file_name = file_name;
                        }

                        public DownloadUrlBean getDownload_url() {
                            return download_url;
                        }

                        public void setDownload_url(DownloadUrlBean download_url) {
                            this.download_url = download_url;
                        }

                        public static class FileNameBean {
                            /**
                             * text :
                             * link_list : [{"delay":"","xpath":"","regex":""}]
                             */

                            private String text;
                            private List<LinkListBean> link_list;

                            public String getText() {
                                return text;
                            }

                            public void setText(String text) {
                                this.text = text;
                            }

                            public List<LinkListBean> getLink_list() {
                                return link_list;
                            }

                            public void setLink_list(List<LinkListBean> link_list) {
                                this.link_list = link_list;
                            }

                            public static class LinkListBean {
                                /**
                                 * delay :
                                 * xpath :
                                 * regex :
                                 */

                                private String delay;
                                private String xpath;
                                private String regex;

                                public String getDelay() {
                                    return delay;
                                }

                                public void setDelay(String delay) {
                                    this.delay = delay;
                                }

                                public String getXpath() {
                                    return xpath;
                                }

                                public void setXpath(String xpath) {
                                    this.xpath = xpath;
                                }

                                public String getRegex() {
                                    return regex;
                                }

                                public void setRegex(String regex) {
                                    this.regex = regex;
                                }
                            }
                        }

                        public static class DownloadUrlBean {
                            /**
                             * text :
                             * link_list : [{"delay":"","xpath":"","regex":""}]
                             */

                            private String text;
                            private List<LinkListBeanX> link_list;

                            public String getText() {
                                return text;
                            }

                            public void setText(String text) {
                                this.text = text;
                            }

                            public List<LinkListBeanX> getLink_list() {
                                return link_list;
                            }

                            public void setLink_list(List<LinkListBeanX> link_list) {
                                this.link_list = link_list;
                            }

                            public static class LinkListBeanX {
                                /**
                                 * delay :
                                 * xpath :
                                 * regex :
                                 */

                                private String delay;
                                private String xpath;
                                private String regex;

                                public String getDelay() {
                                    return delay;
                                }

                                public void setDelay(String delay) {
                                    this.delay = delay;
                                }

                                public String getXpath() {
                                    return xpath;
                                }

                                public void setXpath(String xpath) {
                                    this.xpath = xpath;
                                }

                                public String getRegex() {
                                    return regex;
                                }

                                public void setRegex(String regex) {
                                    this.regex = regex;
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
