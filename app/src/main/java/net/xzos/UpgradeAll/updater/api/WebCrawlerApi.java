package net.xzos.UpgradeAll.updater.api;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
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

public class WebCrawlerApi extends Api {
    private static final String TAG = "WebCrawlerApi";

    private String url;
    private JSONObject hubConfig;
    private JXDocument doc;

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
        try {
            userAgent = hubConfig.getString("user_agent");
        } catch (JSONException e) {
            Log.w(TAG, "flashData:  未设置 user_agent");
            userAgent = null;
        }
        try {
            Document doc = Jsoup.connect(url).userAgent(userAgent).get();
            this.doc = JXDocument.create(doc);
        } catch (IOException e) {
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
        String name = null;
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
    int getReleaseNum() {
        return getReleaseNodeList().size();
    }

    @Override
    public String getVersionNumber(int releaseNum) {
        if (!isSuccessFlash()) return null;
        String versionNumber = null;
        String versionNumberXpath;
        String versionNumberRegex;
        JXNode releaseNode = getReleaseNodeList().get(releaseNum);  // 初始化 release 节点
        try {
            versionNumberXpath = this.hubConfig.getJSONObject("xpath").getJSONObject("release").getJSONObject("attribute").getString("version_number");
        } catch (JSONException e) {
            versionNumberXpath = null;
            Log.e(TAG, "getDefaultName:  version_number xpath 未设置");
        }
        try {
            versionNumberRegex = this.hubConfig.getJSONObject("regex").getJSONObject("release").getJSONObject("attribute").getString("version_number");
        } catch (JSONException e) {
            versionNumberRegex = null;
            Log.e(TAG, "getDefaultName:  version_number regex 未设置");
        }
        if (this.doc != null && versionNumberXpath != null)
            try {
                versionNumber = releaseNode.selOne(versionNumberXpath).toString();
            } catch (NullPointerException e) {
                Log.e(TAG, "getVersionNumber:  未取得数据, versionNumberXpath : " + versionNumberXpath);
                ;
            } catch (XpathSyntaxErrorException e) {
                Log.e(TAG, "getVersionNumber:  Xpath 语法有误, versionNumberXpath : " + versionNumberXpath);
            }
        versionNumber = regexMatch(versionNumber, versionNumberRegex);
        Log.d(TAG, "getVersionNumber:  version: " + versionNumber);
        return versionNumber;
    }

    @Override
    public JSONObject getReleaseDownload(int releaseNum) {
        JSONObject releaseDownloadUrlJsonObject = new JSONObject();
        if (!isSuccessFlash()) return releaseDownloadUrlJsonObject;
        String url = null;
        String fileName = null;
        String fileNameXpath;
        String fileNameRegex;
        String fileUrlXpath;
        JXNode releaseNode = getReleaseNodeList().get(releaseNum);  // 初始化 release 节点
        try {
            JSONObject releaseAssetsJsonObject = this.hubConfig.getJSONObject("xpath").getJSONObject("release").getJSONObject("attribute").getJSONObject("assets");
            fileNameXpath = releaseAssetsJsonObject.getString("file_name");
            fileUrlXpath = releaseAssetsJsonObject.getString("download_url");
        } catch (JSONException e) {
            fileNameXpath = null;
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
        if (this.doc != null && fileNameXpath != null)
            try {
                fileName = releaseNode.selOne(fileNameXpath).toString();
            } catch (NullPointerException e) {
                Log.e(TAG, "getReleaseDownload: 未取得数据, fileNameXpath: " + fileNameXpath);
            } catch (XpathSyntaxErrorException e) {
                Log.e(TAG, " getReleaseDownload: Xpath 语法有误, fileNameXpath: " + fileNameXpath);
            }
        fileName = regexMatch(fileName, fileNameRegex);
        if (this.doc != null && fileNameXpath != null)
            try {
                url = releaseNode.selOne(fileUrlXpath).toString();
            } catch (NullPointerException e) {
                Log.e(TAG, "getReleaseDownload:  未取得数据, fileUrlXpath: " + fileUrlXpath);
            } catch (XpathSyntaxErrorException e) {
                Log.e(TAG, " getReleaseDownload:  Xpath 语法有误, fileUrlXpath: " + fileUrlXpath);
            }
        Log.d(TAG, "getReleaseDownload: file_name: " + fileName);
        Log.d(TAG, "getReleaseDownload: url: " + url);
        try {
            releaseDownloadUrlJsonObject.put(fileName, url);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return releaseDownloadUrlJsonObject;
    }

    private List<JXNode> getReleaseNodeList() {
        String releaseNodeXpath;
        List<JXNode> releaseNodeList = new ArrayList<>();
        try {
            releaseNodeXpath = this.hubConfig.getJSONObject("xpath").getJSONObject("release").getString("release_node");
        } catch (JSONException e) {
            releaseNodeXpath = null;
            Log.e(TAG, "getDefaultName:  release_node 未设置");
        }
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
        if (matchString != null && regex != null) {
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
