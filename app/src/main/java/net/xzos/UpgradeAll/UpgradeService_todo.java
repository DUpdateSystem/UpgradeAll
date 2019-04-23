package net.xzos.UpgradeAll;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.litepal.LitePal;

import java.util.List;
import java.util.Objects;

public class UpgradeService_todo extends Service {
    private static final String TAG = "UpgradeService_todo";

    public UpgradeService_todo() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);
        int importance = NotificationManager.IMPORTANCE_MIN;
        NotificationChannel channel = new NotificationChannel("UpgradeServer", "后台升级服务（不要关闭）", importance);
        NotificationManager notificationManager = (NotificationManager) getSystemService(
                NOTIFICATION_SERVICE
        );
        notificationManager.createNotificationChannel(channel);
        Notification notification = new NotificationCompat.Builder(this, "UpgradeServer")
                .setContentTitle("Upgrade Server")
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setContentIntent(pi)
                .build();
        startForeground(1, notification);
        while (true) {
            refreshData();
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    static void refreshData() {
        // 刷新整个数据库
        // TODO: 多线程刷新
        List<RepoDatabase> repoDatabase = LitePal.findAll(RepoDatabase.class);
        for (RepoDatabase upgraadeItem : repoDatabase) {
            int id = upgraadeItem.getId();
            String api = upgraadeItem.getApi();
            String owner = upgraadeItem.getOwner();
            String repo = upgraadeItem.getRepo();
            String databaseLatestTag = upgraadeItem.getLatestTag();
            switch (api) {
                case "github":
                    JSONObject latestReleaseJson;
                    JSONArray apiReturnJsonArray;
                    // 预设返回数据
                    String api_url = "https://api.github.com/repos/"
                            + owner + "/" + repo + "/releases";
                    Log.d(TAG, "api_url: " + api_url);
                    GithubApi httpApi = new HttpApi().githubApi(api_url);
                    // 发达 API 请求
                    latestReleaseJson = httpApi.getLatestRelease();
                    apiReturnJsonArray = httpApi.getReturnJsonArray();
                    // 获取数据
                    String lastTag = null;
                    try {
                        lastTag = latestReleaseJson.getString("tag_name");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    if (!Objects.equals(lastTag, databaseLatestTag)) {
                        RepoDatabase repoToUpdate = LitePal.find(RepoDatabase.class, id);
                        repoToUpdate.setLatestTag(lastTag);
                        repoToUpdate.setApiReturnData(apiReturnJsonArray.toString());
                    }
                    // 更新数据库数据
                    break;
            }
        }
    }
}
