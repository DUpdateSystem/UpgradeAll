package net.xzos.UpgradeAll;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

class VersionChecker {

    private static final String TAG = "VersionChecker";
    private JSONObject versionCheckerJsonObject;

    VersionChecker(JSONObject versionCheckerJsonObject) {
        this.versionCheckerJsonObject = versionCheckerJsonObject;
    }

    String getVersion() {
        JSONObject versionCheckerJsonObject = this.versionCheckerJsonObject;
        String versionCheckerApi = "";
        try {
            versionCheckerApi = versionCheckerJsonObject.getString("api");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String version = null;
        switch (versionCheckerApi.toLowerCase()) {
            case "app":
                version = getAppVersion(versionCheckerJsonObject);
                break;
            case "magisk":
                version = getMagiskModuleVersion(versionCheckerJsonObject);
                break;
        }
        return version;
    }

    private static String getAppVersion(JSONObject versionCheckerJsonObject) {
        // 获取软件版本
        String appVersion;
        String packageName = null;
        try {
            packageName = String.valueOf(versionCheckerJsonObject.get("text"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        PackageInfo packageInfo = null;
        try {
            Context context = MyApplication.getContext();
            packageInfo = context.getPackageManager().getPackageInfo(packageName, 0);
        } catch (Throwable ignored) {
        }
        appVersion = packageInfo != null ? packageInfo.versionName : null;
        return appVersion;
    }

    private static String getMagiskModuleVersion(JSONObject versionCheckerJsonObject) {
        String magiskModuleName = null;
        try {
            magiskModuleName = String.valueOf(versionCheckerJsonObject.get("text"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String modulePropFilePath = "/data/adb/modules/" + magiskModuleName + "/module.prop";
        String command = "cat " + modulePropFilePath;
        return runCmd(command);
    }

    private static String runCmd(String cmd) {
        Log.d(TAG, "runCmd:  cmd: " + cmd);
        BufferedReader bufrIn = null;
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(cmd + "\n");
            os.writeBytes("exit\n");
            os.flush();
            bufrIn = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            Log.d(TAG, "runCmd:  bufin: " + bufrIn.readLine());
        } catch (IOException e) {
            Toast.makeText(MyApplication.getContext(), "执行失败", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
        String magiskVersion = null;
        try {
            assert bufrIn != null;
            String line;
            String keyWords = "version=";
            while ((line = bufrIn.readLine()) != null) {
                if (line.indexOf(keyWords) == 0) {
                    magiskVersion = line.substring(keyWords.length());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "runCmd:  " + magiskVersion);
        return magiskVersion;
    }
}
