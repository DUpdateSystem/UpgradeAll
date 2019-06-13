package net.xzos.UpgradeAll.updater.api;

import android.content.res.Resources;
import android.util.Log;

import net.xzos.UpgradeAll.R;
import net.xzos.UpgradeAll.data.MyApplication;
import net.xzos.UpgradeAll.gson.HubConfig;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.seimicrawler.xpath.JXDocument;
import org.seimicrawler.xpath.JXNode;
import org.seimicrawler.xpath.exception.XpathSyntaxErrorException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsoupApi extends Api {
    private static final String TAG = "JsoupApi";

    private String url;
    private HubConfig hubConfig;
    private JXDocument doc;
    private int hubConfigVersion;
    private int hubConfigVersionBase;

    public JsoupApi(String url, HubConfig hubConfig) {
        this.url = url;
        this.hubConfig = hubConfig;
        Resources resources = MyApplication.getContext().getResources();
        hubConfigVersionBase = resources.getInteger(R.integer.hub_config_version_base);
        hubConfigVersion = this.hubConfig.getBaseVersion();
    }

    @Override
    public void flashData() {
        if (hubConfigVersion < hubConfigVersionBase) return;
        String userAgent = hubConfig.getWebCrawler().getUserAgent();
        try {
            Connection connection = Jsoup.connect(url);
            if (userAgent != null) connection.userAgent(userAgent);
            Document doc = connection.get();
            this.doc = JXDocument.create(doc);
        } catch (Throwable e) {
            Log.e(TAG, "flashData: Jsoup 对象初始化失败");
            e.printStackTrace();
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
        JXNode rootNode = this.doc.selN("//body").get(0);
        String name = getDomString(rootNode, defaultNameBean);
        Log.d(TAG, "getDefaultName: name: " + name);
        return name;
    }

    @Override
    public String getVersionNumber(int releaseNum) {
        if (hubConfigVersion < hubConfigVersionBase) return super.getVersionNumber(releaseNum);
        if (!isSuccessFlash()) return null;
        HubConfig.StringItemBean versionNumberBean = this.hubConfig.getWebCrawler().getAppConfig().getRelease().getAttribute().getVersion_number();
        JXNode releaseNode = getReleaseNodeList().get(releaseNum);  // 初始化 release 节点
        String versionNumber = getDomString(releaseNode, versionNumberBean);
        Log.d(TAG, "getVersionNumber: version: " + versionNumber);
        return versionNumber;
    }

    @Override
    public JSONObject getReleaseDownload(int releaseNum) {
        if (hubConfigVersion < hubConfigVersionBase) return super.getReleaseDownload(releaseNum);
        JSONObject releaseDownloadUrlJsonObject = new JSONObject();
        if (!isSuccessFlash()) return releaseDownloadUrlJsonObject;
        JXNode releaseNode = getReleaseNodeList().get(releaseNum);  // 初始化 release 节点
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
            Log.e(TAG, String.format("getReleaseDownload: 字符串为空, fileName: %s, downloadUrl: %s", fileName, downloadUrl));
        }
        return releaseDownloadUrlJsonObject;
    }

    @Override
    public List<JXNode> getReleaseNodeList() {
        String releaseNodeXpath;
        List<JXNode> releaseNodeList = new ArrayList<>();
        releaseNodeXpath = this.hubConfig.getWebCrawler().getAppConfig().getRelease().getReleaseNode();
        if (this.doc != null && releaseNodeXpath != null)
            try {
                releaseNodeList = doc.selN(releaseNodeXpath);
            } catch (XpathSyntaxErrorException e) {
                Log.e(TAG, "getReleaseNodeList: Xpath 语法有误, releaseNodeXpath: " + releaseNodeXpath);
            }
        Log.d(TAG, "getReleaseNodeList: Node Num: " + releaseNodeList.size());
        return releaseNodeList;
    }

    private String getDomString(JXNode node, HubConfig.StringItemBean stringItemBeans) {
        String returnString = stringItemBeans.getText();
        if (returnString != null && returnString.length() != 0) return returnString;
        else returnString = null;
        String regex = stringItemBeans.getSearchPath().getRegex();
        List<HubConfig.StringItemBean.SearchPathBean.XpathListBean> xpathList = stringItemBeans.getSearchPath().getXpathList();
        if (xpathList != null) {
            for (int i = 0; i < xpathList.size(); i++) {
                HubConfig.StringItemBean.SearchPathBean.XpathListBean xpathListBean = xpathList.get(i);
                String xpath = xpathListBean.getXpath();
                try {
                    returnString = node.selOne(xpath).toString();
                    if (i != xpathList.size() - 1) {
                        String userAgent = hubConfig.getWebCrawler().getUserAgent();
                        Connection connection = Jsoup.connect(returnString);
                        if (userAgent != null) connection.userAgent(userAgent);
                        Document doc = connection.get();
                        JXDocument jxDocument = JXDocument.create(doc);
                        node = jxDocument.selN("//body").get(0);
                    }
                } catch (NullPointerException e) {
                    Log.e(TAG, "getDomString: 未取得数据, xpath: " + xpath);
                } catch (XpathSyntaxErrorException e) {
                    Log.e(TAG, "getDomString: Xpath 语法有误, xpath: " + xpath);
                } catch (IOException e) {
                    Log.e(TAG, "getDomString: Jsoup 对象初始化失败");
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
