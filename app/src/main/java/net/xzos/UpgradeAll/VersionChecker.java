package net.xzos.UpgradeAll;

import android.content.Context;
import android.content.pm.PackageInfo;

import org.json.JSONException;
import org.json.JSONObject;

class VersionChecker {
    static String getAppVersion(JSONObject versionChecker) {
        String packageName = null;
        try {
            packageName = String.valueOf(versionChecker.get("text"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        PackageInfo packageInfo = null;
        try {
            Context context = MyApplication.getContext();
            packageInfo = context.getPackageManager().getPackageInfo(packageName, 0);
        } catch (Throwable ignored) {
        }
        return packageInfo != null ? packageInfo.versionName : null;
    }
}
