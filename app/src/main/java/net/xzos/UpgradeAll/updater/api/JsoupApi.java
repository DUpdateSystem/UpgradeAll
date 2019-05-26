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
        try {
            Connection connection = Jsoup.connect(url);
            if (userAgent != null) connection.userAgent(userAgent);
            Document doc = connection.get();
            this.doc = JXDocument.create(doc);
        } catch (Throwable e) {
            Log.e(TAG, "flashData:  Jsoup 对象初始化失败");
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
        if (this.doc != null) {
            try {
                name = doc.selNOne(nameXpath).toString();
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
        JXNode releaseNode = getReleaseNodeList().get(releaseNum);  // 初始化 release 节点
        if (this.doc != null && versionNumberXpath != null)
            try {
                versionNumber = releaseNode.selOne(versionNumberXpath).toString();
            } catch (NullPointerException e) {
                Log.e(TAG, "getVersionNumber:  未取得数据, versionNumberXpath : " + versionNumberXpath);
            } catch (XpathSyntaxErrorException e) {
                Log.e(TAG, "getVersionNumber:  Xpath 语法有误, versionNumberXpath : " + versionNumberXpath);
            }
        versionNumber = regexMatch(versionNumber, versionNumberRegex);
        Log.d(TAG, "getVersionNumber:  version: " + versionNumber);
        return versionNumber;
    }

    @Override
    public JSONObject getReleaseDownload(int releaseNum) {
        if (hubConfigVersion < hubConfigVersionBase) return super.getReleaseDownload(releaseNum);
        JSONObject releaseDownloadUrlJsonObject = new JSONObject();
        if (!isSuccessFlash()) return releaseDownloadUrlJsonObject;
        JXNode releaseNode = getReleaseNodeList().get(releaseNum);  // 初始化 release 节点
        HubConfig.StringItemBean fileNameBean = this.hubConfig.getWebCrawler().getAppConfig().getRelease().getAttribute().getAssets().getFileName();
        HubConfig.DownloadItemBean downloadUrlBean = this.hubConfig.getWebCrawler().getAppConfig().getRelease().getAttribute().getAssets().getDownloadUrl();
        // 获取文件名
        String fileName = fileNameBean.getText();
        if (fileName == null || fileName.length() == 0) {
            String fileNameXpath = fileNameBean.getSearchPath().getXpath();
            String fileNameRegex = fileNameBean.getSearchPath().getRegex();
            try {
                fileName = releaseNode.selOne(fileNameXpath).toString();
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
            String downloadUrlXpath = downloadUrlBean.getSearchPath().getXpath();
            String downloadUrlRegex = downloadUrlBean.getSearchPath().getRegex();
            try {
                downloadUrl = releaseNode.selOne(downloadUrlXpath).toString();
            } catch (NullPointerException e) {
                Log.e(TAG, "getReleaseDownload:  未取得数据, downloadUrlXpath: " + downloadUrlXpath);
            } catch (XpathSyntaxErrorException e) {
                Log.e(TAG, " getReleaseDownload:  Xpath 语法有误, downloadUrlXpath: " + downloadUrlXpath);
            }
            downloadUrl = regexMatch(downloadUrl, downloadUrlRegex);
        }
        Log.d(TAG, "getReleaseDownload: file_name: " + fileName);
        Log.d(TAG, "getReleaseDownload: download_url: " + downloadUrl);
        try {
            releaseDownloadUrlJsonObject.put(fileName, downloadUrl);
        } catch (JSONException e) {
            Log.e(TAG, String.format("getReleaseDownload:  字符串为空, fileName: %s, url: %s", fileName, url));
        }
        return releaseDownloadUrlJsonObject;
    }

    private List<JXNode> getReleaseNodeList() {
        String releaseNodeXpath;
        List<JXNode> releaseNodeList = new ArrayList<>();
        releaseNodeXpath = this.hubConfig.getWebCrawler().getAppConfig().getRelease().getReleaseNode();
        if (this.doc != null && releaseNodeXpath != null)
            try {
                releaseNodeList = doc.selN(releaseNodeXpath);
            } catch (XpathSyntaxErrorException e) {
                Log.e(TAG, "getReleaseNodeList:  Xpath 语法有误, releaseNodeXpath: " + releaseNodeXpath);
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
