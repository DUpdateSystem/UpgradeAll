package net.xzos.UpgradeAll.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;

import com.jaredrummler.android.shell.CommandResult;
import com.jaredrummler.android.shell.Shell;

import net.xzos.UpgradeAll.application.MyApplication;
import net.xzos.UpgradeAll.server.log.LogUtil;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VersionChecker {

    private static final String TAG = "VersionChecker";
    private static final String[] LogObjectTag = {"Core", TAG};
    protected static final LogUtil Log = MyApplication.getServerContainer().getLog();

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
            String shellCommand;
            try {
                shellCommand = versionCheckerJsonObject.getString("text");
            } catch (JSONException e) {
                Log.e(LogObjectTag, TAG, "getVersion: JSONObject解析出错,  versionCheckerJsonObject: " + versionCheckerJsonObject.toString());
                shellCommand = "";
            }
            switch (versionCheckerApi.toLowerCase()) {
                case "app":
                    version = getAppVersion(versionCheckerJsonObject);
                    break;
                case "magisk":
                    version = getMagiskModuleVersion(versionCheckerJsonObject);
                    break;
                case "shell":
                    version = Shell.run(shellCommand).getStdout();
                    break;
                case "shell_root":
                    version = Shell.SU.run(shellCommand).getStdout();
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
                appVersion = null;
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
        CommandResult result = Shell.SU.run(command);
        String[] resultList = result.getStdout().split("\n");
        String keyWords = "version=";
        for (String resultLine : resultList) {
            if (resultLine.indexOf(keyWords) == 0) {
                magiskModuleVersion = resultLine.substring(keyWords.length());
            }
        }
        return magiskModuleVersion;
    }

    private static boolean isVersion(String versionString) {
        boolean isVersion = false;
        String regexString = "\\d+(\\.\\d+)*";
        if (versionString != null) {
            Pattern p = Pattern.compile(regexString);
            Matcher m = p.matcher(versionString);
            if (m.find()) {
                isVersion = true;
            }
        }
        return isVersion;
    }

    private static String getVersionNumberString(@NonNull String versionString) {
        String versionMatchString = versionString;
        String[] versionStringList = versionString.split(" ");
        for (String temp : versionStringList) {
            if (isVersion(temp))
                versionMatchString = temp;
        }
        return versionMatchString;
    }

    public static boolean compareVersionNumber(String versionNumber0, String versionNumber1) {
        /*
         * 对比 versionNumber0 与 versionNumber1
         * 若，前者比后者大，则返回 true*/
        Log.i(LogObjectTag, TAG, String.format("compareVersionNumber: versionNumber0: %s , versionNumber1: %s", versionNumber0, versionNumber1));
        versionNumber0 = getVersionNumberString(versionNumber0);
        versionNumber1 = getVersionNumberString(versionNumber1);
        DefaultArtifactVersion version0 = new DefaultArtifactVersion(versionNumber0);
        DefaultArtifactVersion version1 = new DefaultArtifactVersion(versionNumber1);
        return version0.compareTo(version1) >= 0;
    }
}
