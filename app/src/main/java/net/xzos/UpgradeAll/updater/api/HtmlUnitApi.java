package net.xzos.UpgradeAll.updater.api;

import android.content.res.Resources;
import android.util.Log;

import com.gargoylesoftware.htmlunit.AjaxController;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.ScriptException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.DomText;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.gargoylesoftware.htmlunit.javascript.background.JavaScriptJobManager;

import net.xzos.UpgradeAll.R;
import net.xzos.UpgradeAll.data.MyApplication;
import net.xzos.UpgradeAll.gson.HubConfig;

import org.json.JSONException;
import org.json.JSONObject;
import org.seimicrawler.xpath.exception.XpathSyntaxErrorException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlUnitApi extends Api {

    private static final String TAG = "JsoupApi";

    private String url;
    private HubConfig hubConfig;
    private HtmlPage page;
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
        final WebClient webClient;
        if (userAgent != null && userAgent.length() != 0) {
            final BrowserVersion browserVersion = new BrowserVersion.BrowserVersionBuilder(BrowserVersion.FIREFOX_52)
                    .setUserAgent(userAgent)
                    .build();
            Log.d(TAG, "flashData:  HtmlUnit FireFox");
            webClient = new WebClient(browserVersion);
        } else {
            webClient = new WebClient(BrowserVersion.BEST_SUPPORTED);
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
        String nameXpath = defaultNameBean.getSearchPath().getXpath();
        String nameRegex = defaultNameBean.getSearchPath().getRegex();
        if (this.page != null) {
            try {
                DomElement element = page.getFirstByXPath(nameXpath);
                name = element.getNodeValue();
            } catch (XpathSyntaxErrorException e) {
                Log.e(TAG, "getDefaultName:  Xpath 语法有误, nameXpath: " + nameXpath);
            }
        }
        name = regexMatch(name, nameRegex);
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
        String versionNumber = versionNumberBean.getText();
        if (versionNumber != null && versionNumber.length() != 0) return versionNumber;
        String versionNumberXpath = versionNumberBean.getSearchPath().getXpath();
        String versionNumberRegex = versionNumberBean.getSearchPath().getRegex();
        final DomElement releaseNode = getReleaseNodeList().get(releaseNum);  // 初始化 release 节点
        if (this.page != null && versionNumberXpath != null)
            try {
                DomText domText = releaseNode.getFirstByXPath(versionNumberXpath);
                versionNumber = domText.getNodeValue();
            } catch (NullPointerException e) {
                Log.e(TAG, "getVersionNumber: 未取得数据, versionNumberXpath : " + versionNumberXpath);
            } catch (XpathSyntaxErrorException e) {
                Log.e(TAG, "getVersionNumber: Xpath 语法有误, versionNumberXpath : " + versionNumberXpath);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        versionNumber = regexMatch(versionNumber, versionNumberRegex);
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
        HubConfig.DownloadItemBean downloadUrlBean = this.hubConfig.getWebCrawler().getAppConfig().getRelease().getAttribute().getAssets().getDownloadUrl();
        // 获取文件名
        String fileName = fileNameBean.getText();
        if (fileName == null || fileName.length() == 0) {
            String fileNameXpath = fileNameBean.getSearchPath().getXpath();
            String fileNameRegex = fileNameBean.getSearchPath().getRegex();
            try {
                DomText domText = releaseNode.getFirstByXPath(fileNameXpath);
                fileName = domText.getNodeValue();
            } catch (NullPointerException e) {
                Log.e(TAG, "getReleaseDownload: 未取得数据, fileNameXpath: " + fileNameXpath);
            } catch (XpathSyntaxErrorException e) {
                Log.e(TAG, " getReleaseDownload: Xpath 语法有误, fileNameXpath: " + fileNameXpath);
            }
            fileName = regexMatch(fileName, fileNameRegex);
        }
        // 获取下载链接
        String downloadUrl = downloadUrlBean.getText();
        if (downloadUrl == null || downloadUrl.length() == 0) {
            boolean isDownloadButton = downloadUrlBean.getSearchPath().getIsButton();
            String downloadUrlXpath = downloadUrlBean.getSearchPath().getXpath();
            String downloadUrlRegex = downloadUrlBean.getSearchPath().getRegex();
            try {
                if (!isDownloadButton) {
                    DomElement element = releaseNode.getFirstByXPath(downloadUrlXpath);
                    downloadUrl = element.getNodeValue();
                } else {
                    final HtmlAnchor button = releaseNode.getFirstByXPath(downloadUrlXpath);
                    final HtmlPage page2 = button.click();
                    if (page2.isHtmlPage()) {
                        downloadUrl = page2.getUrl().toString();
                    }
                }
            } catch (NullPointerException e) {
                Log.e(TAG, "getReleaseDownload:  未取得数据, downloadUrlXpath: " + downloadUrlXpath);
            } catch (XpathSyntaxErrorException e) {
                Log.e(TAG, " getReleaseDownload:  Xpath 语法有误, downloadUrlXpath: " + downloadUrlXpath);
            } catch (IOException | ScriptException e) {
                Log.d(TAG, "getReleaseDownload:  按钮点击事件失败, downloadUrlXpath: " + downloadUrlXpath);
            }
            downloadUrl = regexMatch(downloadUrl, downloadUrlRegex);
        }
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

