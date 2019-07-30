package net.xzos.UpgradeAll.server.hub;

import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import net.xzos.UpgradeAll.R;
import net.xzos.UpgradeAll.application.MyApplication;
import net.xzos.UpgradeAll.gson.CloudConfig;
import net.xzos.UpgradeAll.gson.HubConfig;
import net.xzos.UpgradeAll.server.JSEngine.JSUtils.OkHttpApi;
import net.xzos.UpgradeAll.utils.FileUtil;
import net.xzos.UpgradeAll.utils.LogUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class CloudHub {

    private static final String TAG = "CloudHub";
    private static final String[] LogObjectTag = {"Core", TAG};

    private static final LogUtil Log = MyApplication.getLog();
    private String rulesListJsonFileRawUrl;
    private CloudConfig cloudConfig;

    public CloudHub() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext());
        String defCloudRulesHubUrl = MyApplication.getContext().getResources().getString(R.string.default_cloud_rules_hub_url);
        String gitUrl = sharedPref.getString("cloud_rules_hub_url", defCloudRulesHubUrl);
        String baseRawUrl = getRawRootUrl(Objects.requireNonNull(gitUrl));
        rulesListJsonFileRawUrl = baseRawUrl + "rules/rules_list.json";
    }

    public boolean flashCloudConfigList() {
        boolean isSuccess = false;
        if (rulesListJsonFileRawUrl != null) {
            String jsonText = OkHttpApi.getHttpResponse(LogObjectTag, rulesListJsonFileRawUrl);
            // 如果刷新失败，则不记录数据
            if (jsonText != null && jsonText.length() != 0) {
                Gson gson = new Gson();
                try {
                    cloudConfig = gson.fromJson(jsonText, CloudConfig.class);
                    isSuccess = true;
                } catch (JsonSyntaxException e) {
                    Log.e(LogObjectTag, TAG, "refreshData: ERROR_MESSAGE: " + e.toString());
                }
            }
        }
        return isSuccess;
    }

    public List<CloudConfig.AppListBean> getAppList() {
        if (cloudConfig != null) return cloudConfig.getAppList();
        else return new ArrayList<>();
    }

    public List<CloudConfig.HubListBean> getHubList() {
        if (cloudConfig != null) return cloudConfig.getHubList();
        else return new ArrayList<>();
    }

    public String getAppConfig(String packageName) {
        String appConfigRawUrl = cloudConfig.getListUrl().getAppListRawUrl() + packageName + ".json";
        return OkHttpApi.getHttpResponse(LogObjectTag, appConfigRawUrl);
    }

    public HubConfig getHubConfig(String hubConfigName) {
        String hubConfigRawUrl = cloudConfig.getListUrl().getHubListRawUrl() + hubConfigName + ".json";
        String hubConfigString = OkHttpApi.getHttpResponse(LogObjectTag, hubConfigRawUrl);
        Gson gson = new Gson();
        try {
            return gson.fromJson(hubConfigString, HubConfig.class);
        } catch (JsonSyntaxException e) {
            return null;
        }
    }

    public String getHubConfigJS(final String filePath) {
        String hubListRawUrl = cloudConfig.getListUrl().getHubListRawUrl();
        String hubConfigJSRawUrl = FileUtil.pathTransformRelativeToAbsolute(hubListRawUrl, filePath);
        return OkHttpApi.getHttpResponse(LogObjectTag, hubConfigJSRawUrl);
    }

    private String getRawRootUrl(String gitUrl) {
        String[] temp = gitUrl.split("github\\.com");
        temp = temp[temp.length - 1].split("/");
        List<String> list = new ArrayList<>(Arrays.asList(temp));
        list.removeAll(Arrays.asList("", null));
        if (list.size() >= 2) {
            String owner = list.get(0);
            String repo = list.get(1);
            String branch = null;
            if (list.size() == 2) {
                branch = "master";
                // 分割网址
            } else if (list.size() == 4 && list.contains("tree")) {
                branch = list.get(3);
                // 分割网址
            }
            if (branch != null)
                return "https://raw.githubusercontent.com/" + owner + "/" + repo + "/" + branch + "/";
        }
        return null;
    }
}
