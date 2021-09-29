package com.example.rtequickstart;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.Random;
//  targetSdkVersion 设置为 28 时,想要使用前台服务的应用必须首先请求 FOREGROUND_SERVICE 权限。
//  这是普通权限，因此，系统会自动为请求权限的应用授予此权限。在未获得此权限的情况下启动前台服务将会引发 SecurityException。
//  参考：https://developer.android.com/about/versions/pie/android-9.0-migration#tya
public class MediaProjectionForegroundService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
    }

    private void createNotificationChannel() {

        String channelId = "screen_share_id";

        // 从 API 26 开始必须有 NotificationChannel 才能发送 Notification
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(channelId, MediaProjectionForegroundService.class.getSimpleName(), importance);
            channel.setDescription("即将开始屏幕录制");
            channel.enableLights(true);
            channel.setLightColor(Color.RED);
            channel.enableVibration(true);
            NotificationManager notificationManager = (NotificationManager)
                    getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
        }

        // 创建 notification 并设置 notification channel
        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setContentText("正在进行屏幕录制")
                .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
                .setColor(getResources().getColor(R.color.teal_200))
                .setWhen(System.currentTimeMillis())
                .build();
        startForeground(new Random().nextInt(1024) + 1024, notification);
    }
}