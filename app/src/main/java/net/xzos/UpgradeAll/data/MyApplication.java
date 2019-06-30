package net.xzos.UpgradeAll.data;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

import net.xzos.UpgradeAll.server.updater.Updater;
import net.xzos.UpgradeAll.utils.LogUtil;

import org.litepal.LitePal;

@SuppressLint("Registered")
public class MyApplication extends Application {

    @SuppressLint("StaticFieldLeak")
    private static Context context;

    final private static LogUtil Log = new LogUtil();
    final private static Updater updater = new Updater();

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        LitePal.initialize(context);
    }

    public static Context getContext() {
        return context;
    }

    public static LogUtil getLog() {
        return Log;
    }

    public static Updater getUpdater() {
        return updater;
    }
}
