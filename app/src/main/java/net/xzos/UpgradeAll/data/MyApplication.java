package net.xzos.UpgradeAll.data;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

import net.xzos.UpgradeAll.updater.Updater;

import org.litepal.LitePal;

@SuppressLint("Registered")
public class MyApplication extends Application {

    final private static Updater updater = new Updater();

    @SuppressLint("StaticFieldLeak")
    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        LitePal.initialize(context);
    }

    public static Context getContext() {
        return context;
    }

    public static Updater getUpdater() {
        return updater;
    }
}
