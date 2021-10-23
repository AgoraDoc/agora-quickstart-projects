# 十分钟实现摄像头流和屏幕共享流的同时发送


本文介绍如何通过极简代码快速实现摄像头流和屏幕共享流的同时发送。


## 前提条件

你已经根据 [十分钟构建视频通话应用](https://gitee.com/yamasite/RTE-Workshop-Tutorial/blob/master/start_call_android_rte.md) 搭建了一个基础的视频通话应用。

> 在此教程中，Android 系统需要 Android 5 或更高版本。
> 实测在夜神模拟器/雷电模拟器/ Android Studio 自带模拟器（Android 5）环境下运行正常。

## 实现流程

对你已经搭建的应用做以下变更。

1. 在 `Gradle Scripts/build.gradle(Module: RteQuickstart.app)` 文件中将 `minSdk` 改为 `21`。这也是实现屏幕录制功能要求的最低 Android 版本。

  ```diff
  - minSdk 16
  + minSdk 21
  ```

2. 在 `Gradle Scripts/build.gradle(Module: RteQuickstart.app)` 文件中，确保以下依赖项的版本为代码所示：

  ```gradle
  implementation 'androidx.appcompat:appcompat:1.3.1'
  implementation 'androidx.core:core:1.6.0'
  implementation 'androidx.constraintlayout:constraintlayout:2.1.0'
  ```

3. 在 `/app/Manifests/AndroidManifest.xml` 文件添加如下权限：

  ```xml
  <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
  ```

4. 添加一个 service，用于运行 media projection 服务。

  在 `/app/Manifests/AndroidManifest.xml` 文件中添加 MediaProjectionForegroundService 服务。

  ```diff
  +      <service
  +      android:name=".MediaProjectionForegroundService"
  +      android:enabled="true"
  +      android:exported="true"
  +      android:foregroundServiceType="mediaProjection"></service>

    <activity
        android:name=".MainActivity"
        android:exported="true">
  ```

  在 `/app/res/drawable` 目录下添加 `ic_notification_icon.xml` 文件，作为 notification 所需的 icon。

  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <vector xmlns:android="http://schemas.android.com/apk/res/android"
      android:width="108dp"
      android:height="108dp"
      android:viewportWidth="108"
      android:viewportHeight="108">
      <path
          android:pathData="M24,54A30,30 0 0 0 84,54A30,30 0 0 0 24,54"
          android:strokeWidth="20"
          android:strokeColor="#FFFFFFFF"
          android:fillType="nonZero" />
      <path
          android:pathData="M94,92C68,90 68,20 94,16Z"
          android:fillType="nonZero"
          android:fillColor="#FFFFFFFF" />
  </vector>
  ```

  创建 `/app/java/com.example.rtequickstart/MediaProjectionForegroundService.java` 文件，作为 MediaProjectionForegroundService 服务的逻辑。

  ```java
  package com.example.rtequickstart;

  import android.app.Notification;
  import android.app.NotificationChannel;
  import android.app.NotificationManager;
  import android.app.Service;
  import android.content.Context;
  import android.content.Intent;
  import android.graphics.drawable.Icon;
  import android.os.Build;
  import android.os.IBinder;

  import androidx.annotation.Nullable;
  import androidx.core.app.NotificationCompat;
  import androidx.core.content.ContextCompat;

  import java.util.Random;
  //  targetSdkVersion 设置为 28 时,想要使用前台服务的应用必须首先请求 FOREGROUND_SERVICE 权限。
  //  这是普通权限，因此，系统会自动为请求权限的应用授予此权限。在未获得此权限的情况下启动前台服务将会引发 SecurityException。
  //  参考：https://developer.android.com/about/versions/pie/android-9.0-migration#tya
  public class MediaProjectionForegroundService extends Service {

      NotificationManager notificationManager;
      Icon icon;

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
          // 参考：https://developer.android.com/training/notify-user/channels
          if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
              int importance = NotificationManager.IMPORTANCE_HIGH;
              NotificationChannel channel = new NotificationChannel(channelId, MediaProjectionForegroundService.class.getSimpleName(), importance);
              channel.setDescription("The app is about to record the screen");
              notificationManager = (NotificationManager)
                      getSystemService(Context.NOTIFICATION_SERVICE);
              notificationManager.createNotificationChannel(channel);
          }


          // 创建 notification 并设置 notification channel
          Notification notification = new NotificationCompat.Builder(this, channelId)
                  .setContentTitle("Screen Recording")
                  .setContentText("Recording the screen")
                  // 你必须设置一个 icon
                  .setSmallIcon(R.drawable.ic_notification_icon)
                  .setWhen(System.currentTimeMillis())
                  .build();

          startForeground(new Random().nextInt(1024) + 1024, notification);
      }
  }
  ```

5. 对 `/app/java/com.example.rtequickstart/MainActivity.java` 文件做如下修改。

   a. 添加以下 import：

    ```java
    import androidx.annotation.RequiresApi;
    import androidx.activity.result.ActivityResultLauncher;
    import androidx.activity.result.contract.ActivityResultContracts;
    import android.content.Context;
    import android.content.Intent;
    import android.os.Build;
    import android.media.projection.MediaProjectionManager;
    import io.agora.rte.media.track.AgoraRteScreenVideoTrack;
    import io.agora.rte.media.video.AgoraRteVideoEncoderConfiguration;
    ```

    b. 添加以下全局变量：

    ```java
    +    private String screenStreamId = "screen_stream_" + String.valueOf(new Random().nextInt(1024));
    +    private Intent mediaProjectionIntent;
    +    private ActivityResultLauncher<Intent> activityResultLauncher;

    +    // 屏幕录制视频轨道对象
    +    public AgoraRteScreenVideoTrack mScreenVideoTrack;
    ```

    c. 在 `initAgoraRteSDK` 方法定义后面添加 `initScreenActivity` 方法定义，用于初始化屏幕共享进程。

    ```java
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void initScreenActivity() {
        mediaProjectionIntent = new Intent(this, MediaProjectionForegroundService.class);

        // 对于 LOLLIPOP 或之后的 Android 系统，必须在开启 foreground service 之后再开启 mediaProjection service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.startForegroundService(mediaProjectionIntent);
            }
        else {
            this.startService(mediaProjectionIntent);
        }
        MediaProjectionManager mgr = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        Intent intent = mgr.createScreenCaptureIntent();
        activityResultLauncher.launch(intent);
    }
    ```

    d. 在 `registerEventHandler` 方法定义后面添加 `registerScreenActivity` 方法定义。

    ```java
    // 通过 mediaProjection 返回的 activity result 创建并发布屏幕录制视频轨道
    public void registerScreenActivity(){
        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {

            if (result.getResultCode() == RESULT_OK) {
                if (mScreenVideoTrack == null) {
                    // 创建屏幕录制视频轨道
                    mScreenVideoTrack = AgoraRteSDK.getRteMediaFactory().createScreenVideoTrack();
                }
                mScene.createOrUpdateRTCStream(screenStreamId, new AgoraRtcStreamOptions());
                mScreenVideoTrack.startCaptureScreen(result.getData(), new AgoraRteVideoEncoderConfiguration.VideoDimensions());

                // 发布屏幕录制视频轨道
                mScene.publishLocalVideoTrack(screenStreamId, mScreenVideoTrack);
            }
        });
    }
    ```

    e. 在 `onCreate` 方法中按顺序调用 `registerScreenActivity` 和 `initScreenActivity` 方法。

    ```java
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. 初始化 SDK
        initAgoraRteSDK();
        // 2. 初始化 AgoraRteSceneEventHandler 对象
        registerEventHandler();
        // 3. 申请设备权限。权限申请成功后：
        // createAndJoinScene 创建并加入场景, 监听远端媒体流并发送本地媒体流
        // registerScreenActivity 注册监听器，在 media projection activity 完成时创建并发布屏幕录制视频轨道
        if (checkSelfPermission(REQUESTED_PERMISSIONS[0], PERMISSION_REQ_ID) &&
                checkSelfPermission(REQUESTED_PERMISSIONS[1], PERMISSION_REQ_ID)) {
            // 创建并加入场景
            createAndJoinScene();
            // 创建流并在场景中发布流
            createAndPublishStream();
            // 注册屏幕录制活动
            registerScreenActivity();
        }

        // 4. 启动屏幕录制进程
        initScreenActivity();
        }
    ```

    f. 在 `onRequestPermissionsResult` 回调中加入 `registerScreenActivity` 方法。

    ```java
    // 权限申请成功后，创建并加入 scene, 监听远端媒体流并发送本地媒体流
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // 创建并加入场景
        createAndJoinScene();
        // 创建流并在场景中发布流
        createAndPublishStream();
        // 注册屏幕录制活动
        registerScreenActivity();
    }
    ```


### 编译项目并运行 app

将 Android 设备或模拟器连接到你的电脑，并在 Android Studio 里点击 Run 'app'。项目安装到你的设备或模拟器之后，按照以下步骤运行 app：

- 授予你的 app 麦克风和摄像头权限。
- 授予你的 app 屏幕录制权限。
- 启动 app，你会在本地视图中看到自己。
- 在另一台设备或模拟器上运行 app，你可以看到在远端视图看到对端设备采集的视频，包括摄像头视频和屏幕录制视频。

![demo](images/screenshare_plus_camera.png.jpg)

## 常见问题

### Android gradle sync 太慢怎么办？

在 `/Gradle Scripts/build.gradle(Project: RteQuickstart)` 文件中，添加国内镜像源地址。

以阿里云云效 Maven 镜像为例：

```diff
repositories {

...

+    maven {
+      url 'https://maven.aliyun.com/repository/public/'
+    }

...
}
```
