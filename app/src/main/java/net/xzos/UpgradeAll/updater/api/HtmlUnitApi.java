package net.xzos.UpgradeAll.updater.api;

import android.content.res.Resources;
import android.util.Log;

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

    private static final String TAG = "JsoupApi";

    private String url;
    private HubConfig hubConfig;
    private HtmlPage page;
    private WebClient webClient;
    private int hubConfigVersion;
    private int hubConfigVersionBase;

    public HtmlUnitApi(String url, HubConfig hubConfig) {
        this.hubConfig = hubConfig;
        hubConfigVersion = this.hubConfig.getBaseVersion();
        Resources resources = MyApplication.getContext().getResources();
        hubConfigVersionBase = resources.getInteger(R.integer.hub_config_version_base);
        this.url = url;
    }

    @Override
    public void flashData() {
        if (hubConfigVersion < hubConfigVersionBase) return;
        String userAgent = hubConfig.getWebCrawler().getUserAgent();
        if (userAgent != null && userAgent.length() != 0) {
            final BrowserVersion browserVersion = new BrowserVersion.BrowserVersionBuilder(BrowserVersion.FIREFOX_52)
                    .setUserAgent(userAgent)
                    .build();
            Log.d(TAG, "flashData:  HtmlUnit FireFox");
            webClient = new WebClient(browserVersion);
        } else {
            webClient = new WebClient(BrowserVersion.CHROME);
            Log.d(TAG, "flashData:  HtmlUnit Chrome");
        }
        try {
            this.page = webClient.getPage(url);
        } catch (Throwable e) {
            Log.e(TAG, "flashData:  HtmlUnit 对象初始化失败");
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
            e.printStackTrace();
        }
        // 刷新数据
        HubConfig.StringItemBean defaultNameBean = this.hubConfig.getWebCrawler().getAppConfig().getDefaultName();
        String name = defaultNameBean.getText();
        if (name != null && name.length() != 0) return name;
        DomElement rootDom = page.getFirstByXPath("//body");
        name = getDomString(rootDom, defaultNameBean);
        Log.d(TAG, "getDefaultName:  name: " + name);
        return name;
    }

    @Override
    public int getReleaseNum() {
        if (hubConfigVersion < hubConfigVersionBase) return super.getReleaseNum();
        return getReleaseNodeList().size();
    }

    @Override
    public String getVersionNumber(int releaseNum) {
        if (hubConfigVersion < hubConfigVersionBase) return super.getVersionNumber(releaseNum);
        if (!isSuccessFlash()) return null;
        HubConfig.StringItemBean versionNumberBean = this.hubConfig.getWebCrawler().getAppConfig().getRelease().getAttribute().getVersion_number();
        final DomElement releaseNode = getReleaseNodeList().get(releaseNum);  // 初始化 release 节点
        String versionNumber = getDomString(releaseNode, versionNumberBean);
        Log.d(TAG, "getVersionNumber: version: " + versionNumber);
        return versionNumber;
    }

    @Override
    public JSONObject getReleaseDownload(int releaseNum) {
        if (hubConfigVersion < hubConfigVersionBase) return super.getReleaseDownload(releaseNum);
        JSONObject releaseDownloadUrlJsonObject = new JSONObject();
        if (!isSuccessFlash()) return releaseDownloadUrlJsonObject;
        final DomElement releaseNode = getReleaseNodeList().get(releaseNum);  // 初始化 release 节点
        Log.d(TAG, "getReleaseDownload: release node: " + releaseNode.toString());
        HubConfig.StringItemBean fileNameBean = this.hubConfig.getWebCrawler().getAppConfig().getRelease().getAttribute().getAssets().getFileName();
        HubConfig.StringItemBean downloadUrlBean = this.hubConfig.getWebCrawler().getAppConfig().getRelease().getAttribute().getAssets().getDownloadUrl();
        // 获取文件名
        String fileName = getDomString(releaseNode, fileNameBean);
        // 获取下载链接
        String downloadUrl = getDomString(releaseNode, downloadUrlBean);
        Log.d(TAG, "getReleaseDownload: file_name: " + fileName);
        Log.d(TAG, "getReleaseDownload: download_url: " + downloadUrl);
        try {
            releaseDownloadUrlJsonObject.put(fileName, downloadUrl);
        } catch (JSONException e) {
            Log.e(TAG, String.format("getReleaseDownload:  字符串为空, fileName: %s, downloadUrl: %s", fileName, downloadUrl));
        }
        return releaseDownloadUrlJsonObject;
    }

    private List<DomElement> getReleaseNodeList() {
        List<DomElement> releaseNodeList = new ArrayList<>();
        String releaseNodeXpath = this.hubConfig.getWebCrawler().getAppConfig().getRelease().getReleaseNode();
        if (this.page != null && releaseNodeXpath != null) {
            try {
                releaseNodeList = page.getByXPath(releaseNodeXpath);
            } catch (XpathSyntaxErrorException e) {
                Log.e(TAG, "getReleaseNodeList:  Xpath 语法有误, releaseNodeXpath: " + releaseNodeXpath);
            }
        }
        Log.d(TAG, "getReleaseNodeList:  Node Num: " + releaseNodeList.size());
        return releaseNodeList;
    }

    private String getDomString(DomElement domElement, HubConfig.StringItemBean stringItemBeans) {
        String returnString = stringItemBeans.getText();
        if (returnString != null && returnString.length() != 0) return returnString;
        Log.d(TAG, "getDomString: domElement: " + domElement);
        Gson gson = new Gson();
        Log.d(TAG, "getDomString: stringItemBeans: " + gson.toJson(stringItemBeans.toString()));
        HtmlPage page = this.page; // 继承全局根网页
        String regex = stringItemBeans.getSearchPath().getRegex();
        List<HubConfig.StringItemBean.SearchPathBean.XpathListBean> xpathList = stringItemBeans.getSearchPath().getXpathList();
        if (xpathList != null) {
            for (int i = 0; i < xpathList.size(); i++) {
                HubConfig.StringItemBean.SearchPathBean.XpathListBean xpathListBean = xpathList.get(i);
                int delay = xpathListBean.getDelay();
                delay *= 1000;
                String xpath = xpathListBean.getXpath();
                try {
                    Log.d(TAG, "getDomString:  webClient wait " + delay);
                    this.webClient.waitForBackgroundJavaScript(delay);
                    if (i != xpathList.size() - 1) {
                        try {
                            DomElement dom = domElement.getFirstByXPath(xpath);
                            Log.d(TAG, "getDomString: dom: " + dom);
                            page = dom.click();
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                        domElement = page.getFirstByXPath("//body");
                    } else {
                        try {
                            DomNode dom = domElement.getFirstByXPath(xpath);
                            Log.d(TAG, "getDomString: dom: " + dom);
                            returnString = dom.getNodeValue();
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    }
                } catch (NullPointerException e) {
                    Log.e(TAG, " getDomString: 未取得数据, Xpath: " + xpath);
                } catch (XpathSyntaxErrorException e) {
                    Log.e(TAG, " getDomString: Xpath 语法有误, Xpath: " + xpath);
                } catch (ScriptException e) {
                    Log.d(TAG, " getDomString: 按钮点击事件失败, Xpath: " + xpath);
                }
            }
        }
        returnString = regexMatch(returnString, regex);
        return returnString;
    }

    private String regexMatch(String matchString, String regex) {
        Log.d(TAG, "regexMatch:  matchString: " + matchString);
        String regexString = matchString;
        if (matchString != null && regex != null && regex.length() != 0) {
            Pattern p = Pattern.compile(regex);
            Matcher m = p.matcher(matchString);
            if (m.find()) {
                regexString = m.group();
            }
        }
        Log.d(TAG, "regexMatch: regexString: " + regexString);
        return regexString;
    }
}

