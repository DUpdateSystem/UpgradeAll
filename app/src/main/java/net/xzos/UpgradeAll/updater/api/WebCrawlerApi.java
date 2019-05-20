package net.xzos.UpgradeAll.updater.api;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import us.codecraft.xsoup.Xsoup;

public class WebCrawlerApi extends Api {
    private static final String TAG = "WebCrawlerApi";

    private String url;
    private JSONObject hubConfig;
    private Document doc;

    public WebCrawlerApi(String url, JSONObject hubConfig) {
        JSONObject webCrawlerConfig = new JSONObject();
        try {
            webCrawlerConfig = hubConfig.getJSONObject("WebCrawler");
        } catch (JSONException e) {
            Log.e(TAG, "WebCrawlerApi:  未找到 WebCrawler 项, hubConfig: " + hubConfig);
        }
        this.hubConfig = webCrawlerConfig;
        this.url = url;
    }

    @Override
    public void flashData() {
        String userAgent;
        Document tmpDoc = this.doc;
        try {
            userAgent = hubConfig.getString("user_agent");
        } catch (JSONException e) {
            Log.w(TAG, "flashData:  未设置 user_agent");
            userAgent = null;
        }
        try {
            this.doc = Jsoup.connect(url).userAgent(userAgent).get();
        } catch (IOException e) {
            this.doc = tmpDoc;
            Log.e(TAG, "flashData:  Jsoup 对象初始化失败");
        }
    }

    @Override
    public String getDefaultName() {
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
        String name;
        String nameXpath;
        String nameRegex;
        try {
            nameXpath = this.hubConfig.getJSONObject("xpath").getString("name");
        } catch (JSONException e) {
            nameXpath = "";
            Log.e(TAG, "getDefaultName:  name xpath 未设置");
        }
        try {
            nameRegex = this.hubConfig.getJSONObject("regex").getString("name");
        } catch (JSONException e) {
            nameRegex = null;
            Log.e(TAG, "getDefaultName:  name regex 未设置");
        }
        name = Xsoup.compile(nameXpath).evaluate(doc).get();
        name = regexMatch(name, nameRegex);
        Log.d(TAG, "getDefaultName:  name: " + name);
        return name;
    }

    @Override
    int getReleaseNum() {
        return getReleaseNodeList().size();
    }

    @Override
    public String getVersionNumber(int releaseNum) {
        if (!isSuccessFlash()) return null;
        String versionNumber;
        String versionNumberXpath;
        String versionNumberRegex;
        String releaseNode = getReleaseNodeList().get(releaseNum);
        Document doc = Jsoup.parse(releaseNode);  // 初始化 release 节点
        try {
            versionNumberXpath = "/html/body" + this.hubConfig.getJSONObject("xpath").getJSONObject("release").getJSONObject("attribute").getString("version_number");
        } catch (JSONException e) {
            versionNumberXpath = "";
            Log.e(TAG, "getDefaultName:  version_number xpath 未设置");
        }
        try {
            versionNumberRegex = this.hubConfig.getJSONObject("regex").getJSONObject("release").getJSONObject("attribute").getString("version_number");
        } catch (JSONException e) {
            versionNumberRegex = null;
            Log.e(TAG, "getDefaultName:  version_number regex 未设置");
        }
        versionNumber = Xsoup.compile(versionNumberXpath).evaluate(doc).get();
        versionNumber = regexMatch(versionNumber, versionNumberRegex);
        Log.d(TAG, "getVersionNumber:  version: " + versionNumber);
        return versionNumber;
    }

    @Override
    public JSONObject getReleaseDownload(int releaseNum) {
        JSONObject releaseDownloadUrlJsonObject = new JSONObject();
        if (!isSuccessFlash()) return releaseDownloadUrlJsonObject;
        String url;
        String fileName;
        String fileNameXpath;
        String fileNameRegex;
        String fileUrlXpath;
        String releaseNode = getReleaseNodeList().get(releaseNum);
        Document doc = Jsoup.parse(releaseNode);  // 初始化 release 节点
        try {
            JSONObject releaseAssetsJsonObject = this.hubConfig.getJSONObject("xpath").getJSONObject("release").getJSONObject("attribute").getJSONObject("assets");
            fileNameXpath = "/html/body" + releaseAssetsJsonObject.getString("file_name");
            fileUrlXpath = "/html/body" + releaseAssetsJsonObject.getString("download_url");
        } catch (JSONException e) {
            fileNameXpath = "";
            fileUrlXpath = "";
            Log.e(TAG, String.format("getReleaseDownload: hubConfig: %s", this.hubConfig));
        }
        try {
            JSONObject releaseAssetsJsonObject = this.hubConfig.getJSONObject("regex").getJSONObject("release").getJSONObject("attribute").getJSONObject("assets");
            fileNameRegex = releaseAssetsJsonObject.getString("file_name");
        } catch (JSONException e) {
            fileNameRegex = null;
            Log.e(TAG, "getReleaseDownload: file_name regex 未设置");
        }
        fileName = Xsoup.compile(fileNameXpath).evaluate(doc).get();
        fileName = regexMatch(fileName, fileNameRegex);
        url = Xsoup.compile(fileUrlXpath).evaluate(doc).get();
        Log.d(TAG, "getReleaseDownload: file_name: " + fileName);
        Log.d(TAG, "getReleaseDownload: url: " + url);
        try {
            releaseDownloadUrlJsonObject.put(fileName, url);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return releaseDownloadUrlJsonObject;
    }

    private List<String> getReleaseNodeList() {
        String releaseNodeXpath;
        List<String> releaseNodeList;
        try {
            releaseNodeXpath = this.hubConfig.getJSONObject("xpath").getJSONObject("release").getString("release_node");
        } catch (JSONException e) {
            releaseNodeXpath = "";
            Log.e(TAG, "getDefaultName:  release_node 未设置");
        }
        releaseNodeList = Xsoup.compile(releaseNodeXpath).evaluate(doc).list();
        return releaseNodeList;
    }

    private String regexMatch(String matchString, String regex) {
        Log.d(TAG, "regexMatch:  matchString: " + matchString);
        String regexString = matchString;
        if (regex != null) {
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
