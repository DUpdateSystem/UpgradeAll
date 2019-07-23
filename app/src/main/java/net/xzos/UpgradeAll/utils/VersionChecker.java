package net.xzos.UpgradeAll.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.util.Log;
import android.widget.Toast;

import net.xzos.UpgradeAll.application.MyApplication;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VersionChecker {

    private static final String TAG = "VersionChecker";
    private JSONObject versionCheckerJsonObject;

    public VersionChecker(JSONObject versionCheckerJsonObject) {
        this.versionCheckerJsonObject = versionCheckerJsonObject;
    }

    public String getVersion() {
        JSONObject versionCheckerJsonObject = this.versionCheckerJsonObject;
        String versionCheckerApi = "";
        String version = null;
        try {
            versionCheckerApi = versionCheckerJsonObject.getString("api");
        } catch (JSONException e) {
            e.printStackTrace();
        }
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

    private String getAppVersion(JSONObject versionCheckerJsonObject) {
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

    private String getMagiskModuleVersion(JSONObject versionCheckerJsonObject) {
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

    private String runCmd(String cmd) {
        BufferedReader buffIn = null;
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(cmd + "\n");
            os.writeBytes("exit\n");
            os.flush();
            buffIn = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            Toast.makeText(MyApplication.getContext(), "Shell 命令执行失败", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "runCmd: Shell 命令执行失败");
        }
        String magiskVersion = null;
        try {
            String line;
            String keyWords = "version=";
            if (buffIn != null) {
                while ((line = buffIn.readLine()) != null) {
                    if (line.indexOf(keyWords) == 0) {
                        magiskVersion = line.substring(keyWords.length());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return magiskVersion;
    }

    public String getRegexMatchVersion(String versionString) {
        String regexString;
        String regexVersion = versionString;
        try {
            regexString = String.valueOf(versionCheckerJsonObject.get("regular"));
        } catch (JSONException e) {
            Log.w(TAG, "数据库项 无regular项(已套用默认配置), 请检查 versionCheckerJsonObject: " + versionCheckerJsonObject);
            regexString = "\\d+(\\.\\d+)*";
        }
        if (versionString != null) {
            Pattern p = Pattern.compile(regexString);
            Matcher m = p.matcher(versionString);
            if (m.find()) {
                regexVersion = m.group();
            }
        }
        Log.d(TAG, String.format("getRegexMatchVersion:  原版本号: %s, 处理版本号: %s, 正则规则: %s", versionString, regexVersion, regexString));
        return regexVersion;
    }

    public static boolean compareVersionNumber(String versionNumber0, String versionNumber1) {
        /*
         * 对比 versionNumber0 与 versionNumber1
         * 若，前者比后者大，则返回 true*/
        if (versionNumber0 != null && versionNumber1 != null) {
            if (versionNumber0.equals((versionNumber1))) return true;  // 版本号一致
            String[] versionNumberList0 = versionNumber0.split("\\.");
            Log.d(TAG, "compareVersionNumber0: " + versionNumber0);
            String[] versionNumberList1 = versionNumber1.split("\\.");
            Log.d(TAG, "compareVersionNumber1: " + versionNumber1);
            int listLength = versionNumberList0.length < versionNumberList1.length ? versionNumberList0.length : versionNumberList1.length;  // 获取较短字符串长度
            for (int i = 0; i < listLength; i++) {
                try {
                    if (Integer.parseInt(versionNumberList0[i]) > Integer.parseInt(versionNumberList1[i])) {
                        // 若部分版本号大
                        return true;
                    } else if (Integer.parseInt(versionNumberList0[i]) < Integer.parseInt(versionNumberList1[i])) {
                        // 若部分版本号小
                        return false;
                    } else if (i == listLength - 1) {
                        // 若前缀一致，比较长度
                        return versionNumber0.length() > versionNumber1.length();
                    }
                } catch (NumberFormatException e) {
                    Log.e(TAG, String.format("compareVersionNumber: 数据解析错误, versionNumber0: %s, versionNumber1: %s", versionNumber0, versionNumber1));
                }
            }
        }
        return false;
    }
}
