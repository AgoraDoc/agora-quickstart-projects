package com.example.rtequickstart;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.TypedValue;
import android.view.SurfaceView;

import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.media.projection.MediaProjectionManager;

import java.util.List;
import java.util.Random;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;

import android.os.Bundle;
import android.widget.LinearLayout;

import io.agora.rte.AgoraRteSDK;
import io.agora.rte.AgoraRteSdkConfig;

import io.agora.rte.base.AgoraRteLogConfig;

import io.agora.rte.media.AgoraRteMediaFactory;
import io.agora.rte.media.camera.AgoraRteCameraCaptureObserver;
import io.agora.rte.media.camera.AgoraRteCameraSource;
import io.agora.rte.media.camera.AgoraRteCameraState;
import io.agora.rte.media.stream.AgoraRtcStreamOptions;
import io.agora.rte.media.stream.AgoraRteMediaStreamInfo;
import io.agora.rte.media.track.AgoraRteCameraVideoTrack;
import io.agora.rte.media.track.AgoraRteMicrophoneAudioTrack;
import io.agora.rte.media.track.AgoraRteScreenVideoTrack;
import io.agora.rte.media.video.AgoraRteVideoCanvas;
import io.agora.rte.media.video.AgoraRteVideoEncoderConfiguration;
import io.agora.rte.media.video.AgoraRteVideoSubscribeOptions;

import io.agora.rte.scene.AgoraRteConnectionChangedReason;
import io.agora.rte.scene.AgoraRteScene;
import io.agora.rte.scene.AgoraRteSceneConfig;
import io.agora.rte.scene.AgoraRteSceneConnState;
import io.agora.rte.scene.AgoraRteSceneEventHandler;
import io.agora.rte.scene.AgoraRteSceneJoinOptions;

import io.agora.rte.user.AgoraRteUserInfo;


public class MainActivity extends AppCompatActivity {

    // 将 <Your App ID> 替换为你的 Agora app ID
    private String appId = "<Your App ID>";
    // 你的 Agora scene name
    private String sceneId = "testScene";
    // 自动生成随机 user ID
    private String userId = "user_" + String.valueOf(new Random().nextInt(1024));

    // 你的 Token。在本示例中设为 ""
    private String token = "";

    // 自动生成随机流 ID
    private String streamId = "stream_" + String.valueOf(new Random().nextInt(1024));
    private String screenStreamId = "screen_stream_" + String.valueOf(new Random().nextInt(1024));

    private Intent mediaProjectionIntent;
    private ActivityResultLauncher<Intent> activityResultLauncher;

    // Scene 对象
    public AgoraRteScene mScene;
    // Scene event handler 对象
    public AgoraRteSceneEventHandler mAgoraHandler;
    // 摄像头视频轨道对象
    public AgoraRteCameraVideoTrack mLocalVideoTrack;
    // 麦克风音频轨道对象
    public AgoraRteMicrophoneAudioTrack mLocalAudioTrack;
    // 屏幕录制视频轨道对象
    public AgoraRteScreenVideoTrack mScreenVideoTrack;
    // 加入 scene 选项对象
    public AgoraRteSceneJoinOptions options;

    public AgoraRteMediaFactory mMediaFactory;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void initScreenActivity() {
        mediaProjectionIntent = new Intent(this, MediaProjectionForegroundService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // this.startForegroundService(mediaProjectionIntent);
                this.startForegroundService(mediaProjectionIntent);

            } else {
                this.startService(mediaProjectionIntent);
            }

        MediaProjectionManager mgr = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        Intent intent = mgr.createScreenCaptureIntent();
        activityResultLauncher.launch(intent);
    }


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
        // 2. 初始化 AgoraRteSceneEventHandler 对象
        registerEventHandler();
        // 3. 申请设备权限。权限申请成功后：
        // createAndJoinScene 创建并加入场景, 监听远端媒体流并发送本地媒体流
        // registerScreenActivity 注册监听器，在 media projection activity 完成时创建并发布屏幕录制视频轨道
        if (checkSelfPermission(REQUESTED_PERMISSIONS[0], PERMISSION_REQ_ID) &&
                checkSelfPermission(REQUESTED_PERMISSIONS[1], PERMISSION_REQ_ID)) {
            createAndJoinScene(sceneId, userId, token);
            registerScreenActivity();
        }

        // 4. 启动屏幕录制进程
        initScreenActivity();
        }

    protected void onDestroy() {
        super.onDestroy();

        // 1. 停止屏幕录制
        this.stopService(mediaProjectionIntent);

        // 2. 离开 scene
        /**
         * 离开 scene。
         */
        mScene.leave();
        // 3. 销毁 AgoraRteSDK 对象
        /**
         * 销毁 AgoraRteSDK 对象。
         *
         * @return
         * 0：方法调用成功。
         * <0：方法调用失败。
         */
        AgoraRteSDK.deInit();
    }

    public void initAgoraRteSDK() {
        AgoraRteSdkConfig config = new AgoraRteSdkConfig();
        // 设置 App ID
        config.appId = appId;
        // 设置 context
        config.context = getBaseContext();
        // 设置 log
        config.logConfig = new AgoraRteLogConfig(getBaseContext().getFilesDir().getAbsolutePath());
        /**
         * 初始化 SDK。
         * @param config SDK 配置。
         *
         * @return AgoraRteScene 对象。
         */
        AgoraRteSDK.init(config);
    }

    public void createAndJoinScene(String sceneId, String userId, String token) {
        // 创建 scene
        AgoraRteSceneConfig sceneConfig = new AgoraRteSceneConfig();
        /**
         * 创建 scene。
         * @param sceneId 用于标识 Scene 的 ID。
         * @param sceneConfig scene 配置。
         *
         * @return AgoraRteScene 对象。
         */
        mScene = AgoraRteSDK.createRteScene(sceneId, sceneConfig);

        // 注册场景事件监听器
        mScene.registerSceneEventHandler(mAgoraHandler);

        options = new AgoraRteSceneJoinOptions();
        options.setUserVisibleToRemote(true);

        /**
         * 加入 scene。
         * @param userId 用于标识用户的 ID。在一个 scene 中必须唯一。
         * @param token 用于鉴权的 Token。
         * @param options 加入 scene 选项。
         *
         * @return
         * 0：方法调用成功。
         * <0：方法调用失败。
         */
        mScene.join(userId, token, options);
    }

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

    // 初始化 AgoraRteSceneEventHandler 对象
    public void registerEventHandler(){
        // 创建 AgoraRteSceneEventHandler 对象
        mAgoraHandler = new AgoraRteSceneEventHandler() {
            @Override
            public void onConnectionStateChanged(AgoraRteSceneConnState oldState, AgoraRteSceneConnState newState, AgoraRteConnectionChangedReason reason) {
                super.onConnectionStateChanged(oldState, newState, reason);

                if (newState == AgoraRteSceneConnState.CONN_STATE_CONNECTED) {
                    System.out.println("连接状态已从 " + oldState.toString() + " 变更为 " + newState.toString() + "原因是： " + reason.toString());

                    // 创建实时音视频流
                    AgoraRtcStreamOptions streamOption = new AgoraRtcStreamOptions();
                    /**
                     * 创建或更新 RTC 流。
                     * @param streamId 用于标识流的 ID。在一个 scene 中必须唯一。
                     * @param streamOption 发流选项。
                     *
                     * @return
                     * 0：方法调用成功。
                     * <0：方法调用失败。
                     *
                     */
                    mScene.createOrUpdateRTCStream(streamId, streamOption);

                    FrameLayout container = findViewById(R.id.local_video_view_container);

                    mMediaFactory = AgoraRteSDK.getRteMediaFactory();

                    // 创建摄像头视频轨道
                    /**
                     * 创建摄像头采集视频轨道
                     *
                     * @return AgoraRteCameraVideoTrack 对象。
                     */
                    mLocalVideoTrack = mMediaFactory.createCameraVideoTrack();

                    // 必须先调用 setPreviewCanvas 设置预览画布，再调用 startCapture 开始摄像头采集视频
                    SurfaceView view = new SurfaceView(getBaseContext());
                    container.addView(view);

                    AgoraRteVideoCanvas canvas = new AgoraRteVideoCanvas(view);
                    if (mLocalVideoTrack != null) {
                        // 设置本地预览的 Canvas
                        /**
                         * 设置预览画布。
                         * @param canvas AgoraRteVideoCanvas 对象。
                         *
                         * @return
                         * 0：方法调用成功。
                         * <0：方法调用失败。
                         */
                        mLocalVideoTrack.setPreviewCanvas(canvas);

                        AgoraRteCameraCaptureObserver cameraCaptureObserver = new AgoraRteCameraCaptureObserver() {
                            /**
                             * 摄像头状态变更时触发。
                             * @param agoraRteCameraState 摄像头状态。
                             * @param agoraRteCameraSource 摄像头源。
                             */
                            @Override
                            public void onCameraStateChanged(AgoraRteCameraState agoraRteCameraState, AgoraRteCameraSource agoraRteCameraSource) {
                                System.out.println("Camera state: " + agoraRteCameraState.toString() + " Camera source: " + agoraRteCameraSource.toString());
                            }
                        };

                        // 开始摄像头采集视频
                        /**
                         * 开始摄像头采集。
                         * @param agoraRteCameraCaptureObserver 摄像头状态监听器。
                         *
                         * @return
                         * 0：方法调用成功。
                         * <0：方法调用失败。
                         */
                        mLocalVideoTrack.startCapture(cameraCaptureObserver);
                    }
                    // 发布本地视频轨道
                    /**
                     * 将本地视频轨道发布到指定流。
                     *
                     * 一个流最多可包含一个视频轨道。
                     *
                     * @param streamId 本地流的 ID。
                     * @param videoTrack 要发布的视频轨道。
                     *
                     * @return
                     * 0：方法调用成功。
                     * <0：方法调用失败。
                     */
                    mScene.publishLocalVideoTrack(streamId, mLocalVideoTrack);
                    // 创建麦克风音频轨道

                    mLocalAudioTrack = mMediaFactory.createMicrophoneAudioTrack();
                    // 开始麦克风采集音频
                    /**
                     * 开始录制音频。
                     *
                     * @return
                     * 0：方法调用成功。
                     * <0：方法调用失败。
                     *
                     */
                    mLocalAudioTrack.startRecording();
                    // 发布本地音频轨道
                    /**
                     * 将本地音频轨道发布到指定流。
                     *
                     * 一个流可包含多个音频轨道。
                     *
                     * @param streamId 本地流的 ID。
                     * @param audioTrack 要发布的视频轨道。
                     *
                     * @return
                     * 0：方法调用成功。
                     * <0：方法调用失败。
                     */
                    mScene.publishLocalAudioTrack(streamId, mLocalAudioTrack);


                }
            }

            // 远端用户加入 scene 时触发

            /**
             * 远端用户加入 scene 时触发。
             * @param users scene 中在线的用户列表。
             */
            @Override
            public void onRemoteUserJoined(List<AgoraRteUserInfo> users) {
                super.onRemoteUserJoined(users);
                System.out.println(users.toString());
            }

            // 远端用户离开 scene 时触发

            /**
             * 远端用户离开 scene 时触发。
             * @param users scene 中在线的用户列表。
             */
            @Override
            public void onRemoteUserLeft(List<AgoraRteUserInfo> users) {
                super.onRemoteUserLeft(users);
                System.out.println(users.toString());
            }

            // 远端用户发流时触发

            /**
             * 远端用户发流时触发。
             * @param streams scene 中的流列表。
             */
            @Override
            public void onRemoteStreamAdded(List<AgoraRteMediaStreamInfo> streams) {
                super.onRemoteStreamAdded(streams);

                for (AgoraRteMediaStreamInfo info : streams) {

                    System.out.println("新增 stream ID：" + info.getStreamId());

                    /**
                     * 订阅远端视频。
                     *
                     * @param remoteStreamId 远端流 ID。
                     * @param videoSubscribeOption 订阅选项。
                     */
                    mScene.subscribeRemoteVideo(info.getStreamId(), new AgoraRteVideoSubscribeOptions());
                    /**
                     * 订阅远端音频。
                     *
                     * @param remoteStreamId 远端流 ID。
                     */
                    mScene.subscribeRemoteAudio(info.getStreamId());

                    LinearLayout container = findViewById(R.id.remote_video_view_container);

                    SurfaceView view = new SurfaceView (getBaseContext());
                    view.setZOrderMediaOverlay(true);

                    view.setTag(info.getStreamId());

                    view.setId(ViewCompat.generateViewId());
                    view.setSaveEnabled(true);

                    System.out.println(info.getStreamId() + " 的渲染 view tag 为：" + view.getTag());

                    ViewGroup.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 120, getResources().getDisplayMetrics()));
                    container.addView(view, -1, layoutParams);
                    AgoraRteVideoCanvas canvas = new AgoraRteVideoCanvas(view);
                    /**
                     * public static final int RENDER_MODE_HIDDEN = 1;
                     * public static final int RENDER_MODE_FIT = 2;
                     * public static final int RENDER_MODE_ADAPTIVE = 3;
                     */
                    canvas.renderMode = 2;
                    /**
                     * 设置远端视频渲染画布。
                     * @param remoteStreamId 远端流的 ID。
                     * @param canvas AgoraVideoCanvas 对象。
                     *
                     * @return
                     * 0：方法调用成功。
                     * <0：方法调用失败。
                     */
                    mScene.setRemoteVideoCanvas(info.getStreamId(), canvas);
                }

            }

            // 远端用户停止发流时触发
            /**
             * 远端用户停止发流时触发。
             * @param streams scene 中的流列表。
             */
            @Override
            public void onRemoteStreamRemoved(List<AgoraRteMediaStreamInfo> streams) {
                super.onRemoteStreamRemoved(streams);

                for (AgoraRteMediaStreamInfo info : streams) {

                    System.out.println("删除 stream ID：" + info.getStreamId());

                    LinearLayout container = findViewById(R.id.remote_video_view_container);
                    View view = container.findViewWithTag(info.getStreamId());
                    container.removeView(view);

                    /**
                     * 取消订阅视频。
                     *
                     * @param remoteStreamId 远端流的 ID。
                     */
                    mScene.unsubscribeRemoteVideo(info.getStreamId());
                    /**
                     * 取消订阅音频。
                     *
                     * @param remoteStreamId 远端流的 ID。
                     */
                    mScene.unsubscribeRemoteAudio(info.getStreamId());

                }

            }
        };
    }

}
