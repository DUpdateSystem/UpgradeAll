package net.xzos.UpgradeAll.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.widget.Toast;

import androidx.annotation.NonNull;

import net.xzos.UpgradeAll.application.MyApplication;
import net.xzos.UpgradeAll.utils.log.LogUtil;

import org.jetbrains.annotations.Contract;
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
    private static final String[] LogObjectTag = {"Core", TAG};
    protected static final LogUtil Log = MyApplication.getLog();

    private JSONObject versionCheckerJsonObject;

    public VersionChecker(JSONObject versionCheckerJsonObject) {
        this.versionCheckerJsonObject = versionCheckerJsonObject;
    }

    public String getVersion() {
        JSONObject versionCheckerJsonObject = this.versionCheckerJsonObject;
        String versionCheckerApi = null;
        String version = null;
        try {
            versionCheckerApi = versionCheckerJsonObject.getString("api");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (versionCheckerApi != null) {
            switch (versionCheckerApi.toLowerCase()) {
                case "app":
                    version = getAppVersion(versionCheckerJsonObject);
                    break;
                case "magisk":
                    version = getMagiskModuleVersion(versionCheckerJsonObject);
                    break;
            }
        }
        return version;
    }

    private String getAppVersion(@NonNull JSONObject versionCheckerJsonObject) {
        // 获取软件版本
        String appVersion = null;
        String packageName = null;
        try {
            packageName = versionCheckerJsonObject.getString("text");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (packageName != null) {
            Context context = MyApplication.getContext();
            try {
                PackageInfo packageInfo = context.getPackageManager().getPackageInfo(packageName, 0);
                appVersion = packageInfo.versionName;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
        return appVersion;
    }

    private String getMagiskModuleVersion(@NonNull JSONObject versionCheckerJsonObject) {
        String magiskModuleVersion = null;
        String magiskModuleName = null;
        try {
            magiskModuleName = versionCheckerJsonObject.getString("text");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String modulePropFilePath = "/data/adb/modules/" + magiskModuleName + "/module.prop";
        String command = "cat " + modulePropFilePath;
        BufferedReader cmdReturn = runCmd(command);
        if (cmdReturn != null) {
            try {
                String line;
                String keyWords = "version=";
                while ((line = cmdReturn.readLine()) != null) {
                    if (line.indexOf(keyWords) == 0) {
                        magiskModuleVersion = line.substring(keyWords.length());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return magiskModuleVersion;
    }

    private BufferedReader runCmd(String cmd) {
        BufferedReader buffIn = null;
        try {
            Process su = Runtime.getRuntime().exec("su");
            DataOutputStream outputStream = new DataOutputStream(su.getOutputStream());
            outputStream.writeBytes(cmd + "\n");
            outputStream.writeBytes("exit\n");
            outputStream.flush();
            buffIn = new BufferedReader(new InputStreamReader(su.getInputStream(), StandardCharsets.UTF_8));
            su.waitFor();
            Log.d(LogObjectTag, TAG, "runCmd: Shell 命令执行完毕");
        } catch (IOException e) {
            Toast.makeText(MyApplication.getContext(), "Shell 命令执行失败", Toast.LENGTH_SHORT).show();
            Log.e(LogObjectTag, TAG, "runCmd: Shell 命令执行失败");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return buffIn;
    }

    public String getRegexMatchVersion(String versionString) {
        String regexString;
        String regexVersion = versionString;
        try {
            regexString = String.valueOf(versionCheckerJsonObject.get("regular"));
        } catch (JSONException e) {
            Log.w(LogObjectTag, TAG, "数据库项 无regular项(已套用默认配置), 请检查 versionCheckerJsonObject: " + versionCheckerJsonObject);
            regexString = "\\d+(\\.\\d+)*";
        }
        if (versionString != null) {
            Pattern p = Pattern.compile(regexString);
            Matcher m = p.matcher(versionString);
            if (m.find()) {
                regexVersion = m.group();
            }
        }
        Log.d(LogObjectTag, TAG, String.format("getRegexMatchVersion:  原版本号: %s, 处理版本号: %s, 正则规则: %s", versionString, regexVersion, regexString));
        return regexVersion;
    }

    @Contract("null, _ -> false; !null, null -> false")
    public static boolean compareVersionNumber(String versionNumber0, String versionNumber1) {
        /*
         * 对比 versionNumber0 与 versionNumber1
         * 若，前者比后者大，则返回 true*/
        if (versionNumber0 != null && versionNumber1 != null) {
            if (versionNumber0.equals((versionNumber1))) return true;  // 版本号一致
            String[] versionNumberList0 = versionNumber0.split("\\.");
            Log.d(LogObjectTag, TAG, "compareVersionNumber0: " + versionNumber0);
            String[] versionNumberList1 = versionNumber1.split("\\.");
            Log.d(LogObjectTag, TAG, "compareVersionNumber1: " + versionNumber1);
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
                    Log.e(LogObjectTag, TAG, String.format("compareVersionNumber: 数据解析错误, versionNumber0: %s, versionNumber1: %s", versionNumber0, versionNumber1));
                }
            }
        }
        return false;
    }
}
