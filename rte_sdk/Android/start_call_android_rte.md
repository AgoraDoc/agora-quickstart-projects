# 五分钟实现视频通话

你可以通过在 app 客户端集成 Agora 视频 SDK 实现实时音视频互动。

本文介绍如何通过极简代码快速集成 Agora 视频 SDK ，在你的 Android app 里实现视频通话。

## 技术原理

下图展示在 app 中集成 Agora 的基本工作流程：

![]()

## 前提条件

开始前，请确保你的开发环境满足以下条件：

- Android Studio (推荐最新版本)。
- Android SDK API 等级 16 或以上。
- 可以访问互联网的计算机，且网络环境未部署防火墙。
- 运行 Android 4.1 或以上版本的移动设备或模拟器。

## 创建 Agora 项目

按照以下步骤，在控制台创建一个 Agora 项目。如果你已经创建了 Agora 项目，请确保项目的鉴权机制是 **APP ID**。

1. 登录 Agora [控制台](https://console.agora.io/)，点击左侧导航栏 ![img](https://web-cdn.agora.io/docs-files/1594283671161) **项目管理**按钮进入[项目管理](https://dashboard.agora.io/projects)页面。

2. 在**项目管理**页面，点击**创建**按钮。

   [![img](https://web-cdn.agora.io/docs-files/1594287028966)](https://dashboard.agora.io/projects)

3. 在弹出的对话框内输入**项目名称**，选择**鉴权机制**为 **APP ID**。<div class="alert note">Agora 推荐只在测试环境，或对安全要求不高的场景里使用 App ID 鉴权。</div>

4. 点击**提交**，新建的项目就会显示在**项目管理**页中。

## 获取 App ID

Agora 会给每个项目自动分配一个 App ID 作为项目唯一标识。

在 [Agora 控制台](https://console.agora.io/)的**项目管理**页面，找到你的项目，点击 App ID 右侧的眼睛图标就可以直接复制项目的 App ID。

![获取 appid](https://web-cdn.agora.io/docs-files/1631254702038)

## 创建 Android 项目

按照以下步骤准备开发环境：

1. 在 Android Studio 里，依次选择 **Phone and Tablet** > **Empty Activity**，创建 Android 项目。项目参数设置如下：

    - **Name**: RteQuickstart
    - **Package name**: com.example.rtequickstart
    - **Language**: Java
    - **Minimum SDK**: API 16: Android 4.1 (Jelly Bean)

    创建项目后，Android Studio 会自动开始同步 gradle，请确保同步成功再进行下一步操作。

2. 将视频 SDK 集成到你的项目中。

    打开 SDK 包 `libs` 文件夹，将以下文件或子文件夹复制到你的项目路径中。如果你不需要通过 C++ 接口使用 SDK，则无需 `api` 文件夹。

    | 文件或子文件夹           | 项目路径                 |
    | ------------------------ | ------------------------ |
    | `agora-rte-sdk.jar` 文件 | `/app/libs/`             |
    | `arm-v8a` 文件夹         | `/app/src/main/jniLibs/` |
    | `armeabi-v7a` 文件夹     | `/app/src/main/jniLibs/` |
    | `x86` 文件夹             | `/app/src/main/jniLibs/` |
    | `x86_64` 文件夹          | `/app/src/main/jniLibs/` |
    | `api` 文件夹（可选）      | `/app/src/main/jniLibs/` |

    在 `/Gradle Scripts/build.gradle(Module: rtequickstart.app)` 文件中， 对本地 Jar 包添加依赖：

    ```gradle
    implementation fileTree(dir: 'libs', include: [ '*.jar' ])
    ```

3. 添加网络及设备权限。

    在 `/app/Manifests/AndroidManifest.xml` 文件中，在 `</application>` 后面添加如下权限：

    ```xml
    <uses-permission android:name="android.permission.INTERNET"/>
    <uses-permission android:name="android.permission.CAMERA"/>
    <uses-permission android:name="android.permission.RECORD_AUDIO"/>
    <uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS"/>
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE"/>
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
    <uses-permission android:name="android.permission.BLUETOOTH" />
    <!-- 使用屏幕共享功能时需要 FOREGROUND_SERVICE 权限 -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    ```

4. 防止代码混淆。

    在 `/Gradle Scripts/proguard-rules.pro` 文件中添加如下代码，防止混淆 SDK 的代码：

    ```pro
    -keep class io.agora.**{*;}
    ```

## 在客户端实现视频通话

本节介绍如何使用 Agora 视频 SDK 在你的 app 里实现视频通话。

### 创建用户界面

用户界面中，通常有两个视图框，分别用于展示本地视频和远端视频。在 `/app/res/layout/activity_main.xml` 文件中，用如下代码进行替换：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/activity_main"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".MainActivity">

    <FrameLayout
        android:id="@+id/local_video_view_container"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="@android:color/white" />

    <FrameLayout
        android:id="@+id/remote_video_view_container"
        android:layout_width="160dp"
        android:layout_height="160dp"
        android:layout_alignParentEnd="true"
        android:layout_alignParentRight="true"
        android:layout_alignParentTop="true"
        android:layout_marginEnd="16dp"
        android:layout_marginRight="16dp"
        android:layout_marginTop="16dp"
        android:background="@android:color/darker_gray" />

</RelativeLayout>
```

### 处理 Android 系统逻辑

本节介绍如何导入所需的 Android Platform API 类，获取 Android 权限。

#### 导入 Android Platform API 类

在 `/app/java/com.example.rtequickstart/MainActivity.java` 文件中，在 `package com.example.rtequickstart;` 后添加如下引用：

```java
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.view.SurfaceView;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
```

#### 获取 Android 权限

启动应用程序时，检查是否已在 app 中授予了实现直播所需的权限。如果未授权，使用内置的 Android 功能申请权限；如果已授权，则返回 true。

在 `/app/java/com.example.rtequickstart/MainActivity.java` 文件中，在 `onCreate` 前添加权限申请逻辑：

```java
private static final int PERMISSION_REQ_ID = 22;

private static final String[] REQUESTED_PERMISSIONS = {
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA
};

private boolean checkSelfPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(this, permission) !=
                        PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, requestCode);
                return false;
        }
        return true;
}
```

### 实现互动直播逻辑

打开你的 app，创建 `RtcEngine` 实例，启用视频后加入频道。如果本地用户是主播，则将本地视频发布到用户界面下方的视图中。如果另一主播加入该频道，你的 app 会捕捉到这一加入事件，并将远端视频添加到用户界面右上角的视图中。

互动直播的 API 使用时序见下图：

![]()

按照以下步骤实现该逻辑：

1. 导入 Agora SDK 中的类。

    在 `/app/java/com.example.rtequickstart/MainActivity.java` 文件中，于 `import android.os.Bundle;` 后加入如下引用：

    ```java
    import io.agora.rte.AgoraRteSDK;
    import io.agora.rte.AgoraRteSdkConfig;

    import io.agora.rte.base.AgoraRteLogConfig;

    import io.agora.rte.media.stream.AgoraRtcStreamOptions;
    import io.agora.rte.media.stream.AgoraRteMediaStreamInfo;
    import io.agora.rte.media.track.AgoraRteCameraVideoTrack;
    import io.agora.rte.media.track.AgoraRteMicrophoneAudioTrack;
    import io.agora.rte.media.video.AgoraRteVideoCanvas;
    import io.agora.rte.media.video.AgoraRteVideoSubscribeOptions;

    import io.agora.rte.scene.AgoraRteConnectionChangedReason;
    import io.agora.rte.scene.AgoraRteScene;
    import io.agora.rte.scene.AgoraRteSceneConfig;
    import io.agora.rte.scene.AgoraRteSceneConnState;
    import io.agora.rte.scene.AgoraRteSceneEventHandler;
    import io.agora.rte.scene.AgoraRteSceneJoinOptions;

    import io.agora.rte.user.AgoraRteUserInfo;
    ```

2. 定义全局变量。

    在 `public class MainActivity extends AppCompatActivity {` 后定义以下全局变量：

    ```java
    // 输入你的 Agora app ID
    private String appId = "<Your App ID>";
    // 输入你的 Agora scene name
    private String sceneName = "testScene";
    // 输入你的 user ID
    private String userId = "testUser";

    // 输入你的 Token。不可填 null
    private String token = "";

    private String streamId = "";

    // Scene 对象
    public AgoraRteScene mScene;
    // Scene event handler 对象
    public AgoraRteSceneEventHandler mAgoraHandler;
    // Camera video track 对象
    public AgoraRteCameraVideoTrack mLocalVideoTrack;
    // Microphone audio track 对象
    public AgoraRteMicrophoneAudioTrack mLocalAudioTrack;
    // Scene 加入选项
    public AgoraRteSceneJoinOptions options;
    ```

3. 定义实现视频通话的基本方法。

    在 `onCreate` 方法后面依次定义以下方法：
    - `initAgoraRteSDK`： 初始化 SDK。

        ```java
        public void initAgoraRteSDK() {
        AgoraRteSdkConfig config = new AgoraRteSdkConfig();
        // 设置 Agora App ID
        config.appId = appId;
        // 获取当前 context
        config.context = getBaseContext();
        // 配置日志文件
        config.logConfig = new AgoraRteLogConfig(getBaseContext().getFilesDir().getAbsolutePath());
        // 执行初始化
        AgoraRteSDK.init(config);
        }
        ```

    - `createAndJoinScene`：创建并加入 scene。只有加入相同 scene 的用户才可以互相发送和接收媒体流。

        ```java
        public void createAndJoinScene(String sceneName, String userId, String token) {
            // 创建 scene
            AgoraRteSceneConfig config = new AgoraRteSceneConfig();
            mScene = AgoraRteSDK.createRteScene(sceneName, config);
            // 初始化 AgoraRteSceneEventHandler 对象
            initListener();

            // 注册 scene event handler
            mScene.registerSceneEventHandler(mAgoraHandler);

            options = new AgoraRteSceneJoinOptions();
            options.setUserVisibleToRemote(true);

            // 加入 scene
            mScene.join(userId, token, options);
        }
        ```

    - `initListener`：初始化 `AgoraRteSceneEventHandler` 对象。你需要在 `AgoraRteSceneEventHandler` 对象的相关回调中实现媒体流的发布与接收逻辑。
        - 媒体流发布：本地用户成功加入 scene 时（`onConnectionStateChanged` 回调返回 `CONN_STATE_CONNECTED` 时）开始发布媒体流。
        - 媒体流接收：收到远端用户发送的流时（`onRemoteStreamAdded` 被触发时）。

        ```java
        // 初始化 AgoraRteSceneEventHandler 对象
        public void initListener(){
            // 创建 AgoraRteSceneEventHandler 对象
            mAgoraHandler = new AgoraRteSceneEventHandler() {
                @Override
                public void onConnectionStateChanged(AgoraRteSceneConnState oldState, AgoraRteSceneConnState newState, AgoraRteConnectionChangedReason reason) {
                    super.onConnectionStateChanged(oldState, newState, reason);

                    if (newState == AgoraRteSceneConnState.CONN_STATE_CONNECTED) {
                        // 创建实时媒体流
                        AgoraRtcStreamOptions option = new AgoraRtcStreamOptions();
                        mScene.createOrUpdateRTCStream(userId, option);
                        FrameLayout container = findViewById(R.id.local_video_view_container);
                        // 创建摄像头视频轨道
                        mLocalVideoTrack = AgoraRteSDK.getRteMediaFactory().createCameraVideoTrack();
                        // 必须先调用 setPreviewCanvas，然后才能调用 startCapture 开始摄像头采集视频
                        SurfaceView view = new SurfaceView (getBaseContext());
                        container.addView(view);
                        AgoraRteVideoCanvas canvas = new AgoraRteVideoCanvas(view);
                        if (mLocalVideoTrack != null) {
                            // 设置本地预览的 Canvas
                            mLocalVideoTrack.setPreviewCanvas(canvas);
                            // 开始摄像头采集视频
                            mLocalVideoTrack.startCapture(null);
                        }
                        // 发布本地视频流
                        mScene.publishLocalVideoTrack(userId, mLocalVideoTrack);
                        // 创建麦克风音频轨道
                        mLocalAudioTrack = AgoraRteSDK.getRteMediaFactory().createMicrophoneAudioTrack();
                        // 开始麦克风采集音频
                        mLocalAudioTrack.startRecording();
                        // 发布本地音频流
                        mScene.publishLocalAudioTrack(userId, mLocalAudioTrack);
                    } else if (newState == AgoraRteSceneConnState.CONN_STATE_DISCONNECTED) {
                        System.out.println("连接状态已从 " + oldState.toString() + " 变更为 " + newState.toString());
                    }
                }

                @Override
                public void onRemoteStreamAdded(List<AgoraRteMediaStreamInfo> list) {
                    FrameLayout container = findViewById(R.id.remote_video_view_container);
                    SurfaceView view = new SurfaceView (getBaseContext());
                    container.addView(view);

                    // 获取远端 stream 的 ID。在该示例我们仅考虑一对一通话，因此仅获取第一个 stream ID。
                    streamId = list.get(0).getStreamId();

                    AgoraRteVideoCanvas canvas = new AgoraRteVideoCanvas(view);
                    mScene.setRemoteVideoCanvas(streamId, canvas);

                    mScene.subscribeRemoteVideo(streamId, new AgoraRteVideoSubscribeOptions());
                    mScene.subscribeRemoteAudio(streamId);
                }

            };
        }
        ```

4. 在 `onCreate` 回调中按顺序调用定义的基本方法。

    ```java
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 1. 初始化 SDK
        initAgoraRteSDK();
        // 2. 创建并加入 scene
        createAndJoinScene(sceneName, userId, token);
    }
    ```

5. 在 `onCreate` 回调后定义 `onDestroy` 回调。在 `onDestroy` 回调中离开 scene 并销毁 AgoraRteSDK 对象。

    ```java
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 3. 离开 scene
        mScene.leave();
        // 4. 销毁 AgoraRteSDK 对象
        AgoraRteSDK.deInit();
    }
    ```

## 参考

