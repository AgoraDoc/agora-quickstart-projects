package com.example.rtequickstart;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.view.SurfaceView;
import android.widget.FrameLayout;

import java.util.List;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

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

public class MainActivity extends AppCompatActivity {

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

    public AgoraRteSceneJoinOptions options;

    // 处理设备权限
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 1. 初始化 SDK
        initAgoraRteSDK();
        // 2. 创建并加入 scene
        createAndJoinScene(sceneName, userId, token);
    }

    protected void onDestroy() {
        super.onDestroy();

        // 3. 离开 scene
        mScene.leave();
        // 4. 销毁 AgoraRteSDK 对象
        AgoraRteSDK.deInit();
    }

    public void initAgoraRteSDK() {
        AgoraRteSdkConfig config = new AgoraRteSdkConfig();

        config.appId = appId;
        config.context = getBaseContext();
        config.logConfig = new AgoraRteLogConfig(getBaseContext().getFilesDir().getAbsolutePath());
        AgoraRteSDK.init(config);
    }

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

    // 初始化 AgoraRteSceneEventHandler 对象
    public void initListener(){
        // 创建 AgoraRteSceneEventHandler 对象
        mAgoraHandler = new AgoraRteSceneEventHandler() {
            @Override
            public void onConnectionStateChanged(AgoraRteSceneConnState oldState, AgoraRteSceneConnState newState, AgoraRteConnectionChangedReason reason) {
                super.onConnectionStateChanged(oldState, newState, reason);

                if (newState == AgoraRteSceneConnState.CONN_STATE_CONNECTED) {
                    // 创建实时音视频流
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
            public void onRemoteUserJoined(List<AgoraRteUserInfo> users) {
                super.onRemoteUserJoined(users);
            }

            @Override
            public void onRemoteUserLeft(List<AgoraRteUserInfo> users) {
                super.onRemoteUserLeft(users);
            }

            @Override
            public void onRemoteStreamAdded(List<AgoraRteMediaStreamInfo> list) {
                FrameLayout container = findViewById(R.id.remote_video_view_container);
                SurfaceView view = new SurfaceView (getBaseContext());
                container.addView(view);

                // 获取远端 stream 的 ID。在该示例我们仅考虑一对一通话
                streamId = list.get(0).getStreamId();

                AgoraRteVideoCanvas canvas = new AgoraRteVideoCanvas(view);
                mScene.setRemoteVideoCanvas(streamId, canvas);

                mScene.subscribeRemoteVideo(streamId, new AgoraRteVideoSubscribeOptions());
                mScene.subscribeRemoteAudio(streamId);
            }

        };
    }

}