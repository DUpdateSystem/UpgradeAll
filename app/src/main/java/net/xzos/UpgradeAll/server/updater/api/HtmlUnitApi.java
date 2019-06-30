package net.xzos.UpgradeAll.server.updater.api;

import android.content.res.Resources;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.ScriptException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.google.gson.Gson;

import net.xzos.UpgradeAll.R;
import net.xzos.UpgradeAll.data.MyApplication;
import net.xzos.UpgradeAll.gson.HubConfig;

import org.json.JSONException;
import org.json.JSONObject;
import org.seimicrawler.xpath.exception.XpathSyntaxErrorException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlUnitApi extends Api {

    private static final String TAG = "HtmlUnitApi";
    private String APITAG;

    private String URL;
    private HubConfig hubConfig;
    private HtmlPage page;
    private WebClient webClient;
    private int hubConfigVersion;
    private int hubConfigVersionBase;

    public HtmlUnitApi(String URL, HubConfig hubConfig) {
        this.APITAG = URL;
        this.hubConfig = hubConfig;
        hubConfigVersion = this.hubConfig.getBaseVersion();
        Resources resources = MyApplication.getContext().getResources();
        hubConfigVersionBase = resources.getInteger(R.integer.hub_config_version_base);
        this.URL = URL;
        //java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(Level.OFF);
        //java.util.logging.Logger.getLogger("org.apache.commons.httpclient").setLevel(Level.OFF);
        //java.util.logging.Logger.getLogger("org.apache.http.client.protocol.ResponseProcessCookies").setLevel(Level.OFF);
    }

    @Override
    public void flashData() {
        if (hubConfigVersion < hubConfigVersionBase) return;
        String userAgent = hubConfig.getWebCrawler().getUserAgent();
        if (userAgent != null && userAgent.length() != 0) {
            final BrowserVersion browserVersion = new BrowserVersion.BrowserVersionBuilder(BrowserVersion.FIREFOX_52)
                    .setUserAgent(userAgent)
                    .build();
            Log.d(APITAG, TAG, "flashData:  HtmlUnit FireFox");
            webClient = new WebClient(browserVersion);
        } else {
            webClient = new WebClient(BrowserVersion.CHROME);
            Log.d(APITAG, TAG, "flashData:  HtmlUnit Chrome");
        }
        try {
            this.page = webClient.getPage(URL);
        } catch (Throwable e) {
            Log.e(APITAG, TAG, "flashData: HtmlUnit 对象初始化失败，ERROR_MESSAGE: " + e.toString());
        }
    }

    @Override
    public String getDefaultName() {
        if (hubConfigVersion < hubConfigVersionBase) return super.getDefaultName();
        class FlashDataThread extends Thread {
            public void run() {
                flashData();  // 获取数据
            }
        }
        Thread thread = new FlashDataThread();
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            Log.e(APITAG, TAG, "getDefaultName: ERROR_MESSAGE: " + e.toString());
        }
        // 刷新数据
        HubConfig.StringItemBean defaultNameBean = this.hubConfig.getWebCrawler().getAppConfig().getDefaultName();
        String name = defaultNameBean.getText();
        if (name != null && name.length() != 0) return name;
        DomElement rootDom = page.getFirstByXPath("//body");
        name = getDomString(rootDom, defaultNameBean);
        Log.d(APITAG, TAG, "getDefaultName:  name: " + name);
        return name;
    }

    @Override
    public String getVersionNumber(int releaseNum) {
        if (hubConfigVersion < hubConfigVersionBase) return super.getVersionNumber(releaseNum);
        if (!isSuccessFlash()) return null;
        HubConfig.StringItemBean versionNumberBean = this.hubConfig.getWebCrawler().getAppConfig().getRelease().getAttribute().getVersion_number();
        final DomElement releaseNode = getReleaseNodeList().get(releaseNum);  // 初始化 release 节点
        String versionNumber = getDomString(releaseNode, versionNumberBean);
        Log.d(APITAG, TAG, "getVersionNumber: version: " + versionNumber);
        return versionNumber;
    }

    @Override
    public JSONObject getReleaseDownload(int releaseNum) {
        if (hubConfigVersion < hubConfigVersionBase) return super.getReleaseDownload(releaseNum);
        JSONObject releaseDownloadUrlJsonObject = new JSONObject();
        if (!isSuccessFlash()) return releaseDownloadUrlJsonObject;
        final DomElement releaseNode = getReleaseNodeList().get(releaseNum);  // 初始化 release 节点
        Log.d(APITAG, TAG, "getReleaseDownload: release node: " + releaseNode.toString());
        HubConfig.StringItemBean fileNameBean = this.hubConfig.getWebCrawler().getAppConfig().getRelease().getAttribute().getAssets().getFileName();
        HubConfig.StringItemBean downloadUrlBean = this.hubConfig.getWebCrawler().getAppConfig().getRelease().getAttribute().getAssets().getDownloadUrl();
        // 获取文件名
        String fileName = getDomString(releaseNode, fileNameBean);
        // 获取下载链接
        String downloadUrl = getDomString(releaseNode, downloadUrlBean);
        if (downloadUrl == null) {
            fileName = fileName + "(获取下载地址失败，点击跳转主页)";
            downloadUrl = this.URL;
        }
        Log.d(APITAG, TAG, "getReleaseDownload: file_name: " + fileName);
        Log.d(APITAG, TAG, "getReleaseDownload: download_url: " + downloadUrl);
        try {
            releaseDownloadUrlJsonObject.put(fileName, downloadUrl);
        } catch (JSONException e) {
            Log.e(APITAG, TAG, String.format("getReleaseDownload:  字符串为空, fileName: %s, downloadUrl: %s", fileName, downloadUrl));
        }
        return releaseDownloadUrlJsonObject;
    }

    @Override
    protected List<DomElement> getReleaseNodeList() {
        List<DomElement> releaseNodeList = new ArrayList<>();
        String releaseNodeXpath = this.hubConfig.getWebCrawler().getAppConfig().getRelease().getReleaseNode();
        if (this.page != null && releaseNodeXpath != null) {
            try {
                releaseNodeList = page.getByXPath(releaseNodeXpath);
            } catch (XpathSyntaxErrorException e) {
                Log.e(APITAG, TAG, "getReleaseNodeList:  Xpath 语法有误, releaseNodeXpath: " + releaseNodeXpath);
            }
        }
        Log.d(APITAG, TAG, "getReleaseNodeList:  Node Num: " + releaseNodeList.size());
        return releaseNodeList;
    }

    private String getDomString(DomElement domElement, HubConfig.StringItemBean stringItemBeans) {
        String returnString = stringItemBeans.getText();
        // 初始化 returnString
        if (returnString != null && returnString.length() != 0) return returnString;
        else returnString = null;
        HtmlPage domPage = this.page;
        // 打印 Json
        Gson gson = new Gson();
        Log.d(APITAG, TAG, "getDomString: stringItemBeans: " + gson.toJson(stringItemBeans.toString()));
        String regex = stringItemBeans.getSearchPath().getRegex();
        List<HubConfig.StringItemBean.SearchPathBean.XpathListBean> xpathList = stringItemBeans.getSearchPath().getXpathList();
        if (xpathList != null) {
            for (int i = 0; i < xpathList.size(); i++) {
                Log.d(APITAG, TAG, "getDomString: domElement: " + domElement);
                HubConfig.StringItemBean.SearchPathBean.XpathListBean xpathListBean = xpathList.get(i);
                int delay = xpathListBean.getDelay() * 1000;
                int defaultDelay = 3 * 1000;
                String xpath = xpathListBean.getXpath();
                try {
                    Log.d(APITAG, TAG, "getDomString: webClient wait " + delay);
                    this.webClient.waitForBackgroundJavaScript(delay);
                    for (int j = 0; j < 5; j++) {
                        Log.d(APITAG, TAG, String.format("getDomString: 循环第 %s 次", j));
                        if (i != xpathList.size() - 1) {
                            // 界面跳转逻辑
                            DomElement dom = domElement.getFirstByXPath(xpath);
                            if (dom != null) {
                                Log.d(APITAG, TAG, "getDomString: dom: " + dom);
                                domPage = dom.click();
                                Log.d(APITAG, TAG, "getDomString: clicked, domPage URL to " + domPage.getUrl());
                                domElement = domPage.getFirstByXPath("//body");
                                break;  // 跳出 for 循环
                            }
                        } else {
                            // 获取返回的字符串
                            if (xpath.equals("get_page_url")) {
                                returnString = domPage.getUrl().toString();
                            } else {
                                DomNode dom = domElement.getFirstByXPath(xpath);
                                if (dom != null) {
                                    Log.d(APITAG, TAG, "getDomString: dom: " + dom);
                                    // 获取文本链接信息
                                    returnString = dom.getNodeValue();
                                    if (returnString == null)
                                        returnString = dom.getTextContent();
                                }
                            }
                            if (returnString != null) break;  // 如果获取到字符串，终止重试
                        }
                        this.webClient.waitForBackgroundJavaScript(defaultDelay);
                    }
                } catch (NullPointerException e) {
                    Log.e(APITAG, TAG, "getDomString: 未取得数据, Xpath: " + xpath);
                } catch (XpathSyntaxErrorException e) {
                    Log.e(APITAG, TAG, "getDomString: Xpath 语法有误, Xpath: " + xpath);
                } catch (ScriptException e) {
                    Log.e(APITAG, TAG, "getDomString: 按钮点击事件失败, Xpath: " + xpath);
                } catch (Throwable e) {
                    Log.e(APITAG, TAG, "getDomString: ERROR_MESSAGE: " + e.toString());
                }
            }
        }
        returnString = regexMatch(returnString, regex);
        return returnString;
    }

    private String regexMatch(String matchString, String regex) {
        Log.d(APITAG, TAG, "regexMatch: matchString: " + matchString);
        String regexString = matchString;
        if (matchString != null && regex != null && regex.length() != 0) {
            Pattern p = Pattern.compile(regex);
            Matcher m = p.matcher(matchString);
            if (m.find()) {
                regexString = m.group();
            }
        }
        Log.d(APITAG, TAG, "regexMatch: regexString: " + regexString);
        return regexString;
    }
}

