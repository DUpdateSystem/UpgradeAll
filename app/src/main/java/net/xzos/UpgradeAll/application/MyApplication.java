package net.xzos.UpgradeAll.application;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

import net.xzos.UpgradeAll.server.ServerContainer;

import org.jetbrains.annotations.Contract;
import org.litepal.LitePal;

@SuppressLint("Registered")
public class MyApplication extends Application {

    @SuppressLint("StaticFieldLeak")
    private static Context context;

    final private static ServerContainer serverContainer = new ServerContainer();

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        LitePal.initialize(context);
    }

    @Contract(pure = true)
    public static Context getContext() {
        return context;
    }

    @Contract(pure = true)
    public static ServerContainer getServerContainer() {
        return serverContainer;
    }
}
