//
//  ViewController.swift
//  RteQuickstart
//
//  Created by macoscatalina on 2021/10/8.
//  Copyright © 2021 macoscatalina. All rights reserved.
//

import UIKit
import AgoraRTE

class ViewController: UIViewController {

    // 定义全局变量
    // 用于渲染本地视频的 UIView 对象
    var localView: UIView!
    // 用于渲染远端视频的 UIView 对象
    var remoteStackView: UIStackView!
    // SDK 对象
    var agoraRteSdk: AgoraRteSdk!

    // 场景对象
    var scene: AgoraRteSceneProtocol!
    // 麦克风音频轨道对象
    var microphoneTrack: AgoraRteMicrophoneAudioTrackProtocol!
    // 摄像头视频轨道对象
    var cameraTrack: AgoraRteCameraVideoTrackProtocol!
    // 本地流 ID。本示例自动生成随机流 ID
    let localStreamId = String(UInt.random(in: 1000...2000))
    // 本地用户 ID。本示例自动生成随机用户 ID
    let localUserId = String(UInt.random(in: 1000...2000))
    // 场景 ID
    let sceneId = "testScene"
    // 你的 Agora App ID
    let appId = "<Your App ID>"


    override func viewDidLoad() {
        super.viewDidLoad()
        // Do any additional setup after loading the view.
        initSdk()
        createAndJoinScene()
        createAndPublishStream()
    }


    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        // 离开场景
        scene?.leave()
        // 销毁 SDK 对象
        AgoraRteSdk.destroy()
    }

    func initSdk(){
        // 初始化 AgoraRteSdk 对象
        let profile = AgoraRteSdkProfile()
        profile.appid = appId
        /**
        * 初始化 SDK。
        * @param profile SDK 配置。
        *
        * @return AgoraRteSdk 对象。
        */
        agoraRteSdk = AgoraRteSdk.sharedEngine(with: profile)

        localView = UIView()
        localView.frame = self.view.bounds

        remoteStackView = UIStackView()
        remoteStackView.frame = CGRect(x: self.view.bounds.width - 180, y: 0, width: 180, height: 360)
        remoteStackView.axis = .vertical
        remoteStackView.distribution = .fillEqually
        self.view.addSubview(remoteStackView)

    }

    func createAndJoinScene(){
        // 创建场景
        let config = AgoraRteSceneConfg()
        scene = agoraRteSdk.createRteScene(withSceneId: sceneId, sceneConfig: config)

        // 设置场景事件 delegate
        scene?.setSceneDelegate(self)

        // 创建并添加本地 view

        self.view.addSubview(localView)

        // 加入场景
        let options = AgoraRteJoinOptions()
        scene.joinScene(withUserId: localUserId, token: "", joinOptions: options)
    }

    func createAndPublishStream(){
        let mediaFactory = agoraRteSdk.rteMediaFactory()
            // 创建摄像头视频轨道
            /**
            * 创建摄像头采集视频轨道
            *
            * @return AgoraRteCameraVideoTrack 对象。
            */
            cameraTrack = mediaFactory?.createCameraVideoTrack()

            let videoCanvas = AgoraRtcVideoCanvas()
            videoCanvas.userId = localUserId
            videoCanvas.view = localView
            videoCanvas.renderMode = .hidden

            // 必须先调用 setPreviewCanvas 设置预览画布，再调用 startCapture 开始摄像头采集视频
            // 设置预览画布
            /**
            * 设置预览画布。
            * @param canvas AgoraRteVideoCanvas 对象。
            *
            * @return
            * 0：方法调用成功。
            * <0：方法调用失败。
            */
            cameraTrack?.setPreviewCanvas(videoCanvas)

            // 摄像头开始捕获视频
            /**
            * 开始摄像头采集。
            *
            * @return
            * 0：方法调用成功。
            * <0：方法调用失败。
            */
            cameraTrack?.startCapture()

            // 创建麦克风音频轨道
            microphoneTrack = mediaFactory?.createMicrophoneAudioTrack()
            // 麦克风开始录制音频
            microphoneTrack?.startRecording()

            let streamOption = AgoraRteRtcStreamOptions()
            streamOption.token = ""

            /**
            * 创建或更新 RTC 流。
            * @param localStreamId 用于标识流的 ID。在一个场景中必须唯一。
            * @param streamOption 发流选项。
            *
            * @return
            * 0：方法调用成功。
            * <0：方法调用失败。
            *
            */
            scene?.createOrUpdateRTCStream(localStreamId, rtcStreamOptions: streamOption)

            // 发布本地音频轨道
            /**
            * 将本地音频轨道发布到指定流。
            *
            * 一个流可包含多个音频轨道。
            *
            * @param localStreamId 本地流的 ID。
            * @param rteAudioTrack 要发布的视频轨道。
            *
            * @return
            * 0：方法调用成功。
            * <0：方法调用失败。
            */
            scene?.publishLocalAudioTrack(localStreamId, rteAudioTrack: microphoneTrack!)

            // 发布本地视频轨道
            /**
            * 将本地视频轨道发布到指定流。
            *
            * 一个流最多可包含一个视频轨道。
            *
            * @param localStreamId 本地流的 ID。
            * @param rteVideoTrack 要发布的视频轨道。
            *
            * @return
            * 0：方法调用成功。
            * <0：方法调用失败。
            */
            scene?.publishLocalVideoTrack(localStreamId, rteVideoTrack: cameraTrack!)
    }


}

extension ViewController: AgoraRteSceneDelegate {

    // 当连接状态为 connected 时，开始发流
    func agoraRteScene(_ rteScene: AgoraRteSceneProtocol, connectionStateDidChangeFromOldState oldState: AgoraConnectionState, toNewState state: AgoraConnectionState, with reason: AgoraConnectionChangedReason) {

        print("Connection state has changed to:\(state.rawValue) reason:\(reason.rawValue)")

    }       

    // 远端发流时，订阅流并创建相应的 UIView 在本地进行渲染
    func agoraRteScene(_ rteScene: AgoraRteSceneProtocol, remoteStreamesDidAddWith streamInfos: [AgoraRteStreamInfo]?) {
        guard let infos = streamInfos else { return }
        for info in infos {

            rteScene.subscribeRemoteAudio(info.streamId!)
            let option = AgoraRteVideoSubscribeOptions()
            /**
            * 订阅远端视频。
            *
            * @param remoteStreamId 远端流 ID。
            * @param videoSubscribeOptions 订阅选项。
            */
            rteScene.subscribeRemoteVideo(info.streamId!, videoSubscribeOptions: option)

            DispatchQueue.main.async { [weak self] in
                guard let strongSelf = self else {
                    return
                }

                let remoteView = UIView()
                remoteView.tag = Int(info.streamId!)!
                strongSelf.remoteStackView.addArrangedSubview(remoteView)

                let videoCanvas = AgoraRtcVideoCanvas()
                // videoCanvas.userId = info.userId!
                videoCanvas.view = remoteView
                videoCanvas.renderMode = .fit
                /**
                * 设置远端视频渲染画布。
                * @param remoteStreamId 远端流的 ID。
                * @param videoCanvas AgoraVideoCanvas 对象。
                *
                * @return
                * 0：方法调用成功。
                * <0：方法调用失败。
                */
                rteScene.setRemoteVideoCanvas(info.streamId!, videoCanvas: videoCanvas)
            }
        }
    }

    // 远端停止发流时，停止订阅流并删除相关的 UIView
    func agoraRteScene(_ rteScene: AgoraRteSceneProtocol, remoteStreamDidRemoveWith streamInfos: [AgoraRteStreamInfo]?) {
        guard let infos = streamInfos else { return }
        for info in infos {
            /**
            * 取消订阅视频。
            *
            * @param remoteStreamId 远端流的 ID。
            */
            rteScene.unsubscribeRemoteAudio(info.streamId!)

            /**
            * 取消订阅音频。
            *
            * @param remoteStreamId 远端流的 ID。
            */
            rteScene.unsubscribeRemoteAudio(info.streamId!)

            let viewToRemove = self.remoteStackView.viewWithTag(Int(info.streamId!)!)!
            self.remoteStackView.removeArrangedSubview(viewToRemove)
            }
        }

}
