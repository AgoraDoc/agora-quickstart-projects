//
//  ViewController.swift
//  RteQuickstart
//
//  Created by 王璐 on 2021/10/6.
//

import UIKit
import AgoraRTE

class ViewController: UIViewController {
    
    // Defines localView
    var localView: UIView!
    // Defines remoteView
    var remoteView: UIView!
    
    @IBOutlet weak var remoteStackView: UIStackView!
    // Defines instance of agoraRteSdk
    var agoraRteSdk: AgoraRteSdk!
    // Defines instance of AgoraRteSceneProtocol
    var scene: AgoraRteSceneProtocol!
    // Defines instance of AgoraRteMicrophoneAudioTrackProtocol
    var microphoneTrack: AgoraRteMicrophoneAudioTrackProtocol!
    // Defines instance of AgoraRteMicrophoneAudioTrackProtocol
    var cameraTrack: AgoraRteCameraVideoTrackProtocol!
    // Local stream ID
    let localStreamId = String(UInt.random(in: 1000...2000))
    // Local user ID
    let localUserId = String(UInt.random(in: 1000...2000))
    
    let sceneId = "testScene"
    
    let appId = "<Your app ID>"

    override func viewDidLoad() {
        super.viewDidLoad()
        // Do any additional setup after loading the view.
        joinAndPublish()
    }


    override func viewDidLayoutSubviews(){
        super.viewDidLayoutSubviews()
        localView.frame = self.view.bounds
        
    }

    
    func joinAndPublish(){
        let profile = AgoraRteSdkProfile()
        profile.appid = appId
        agoraRteSdk = AgoraRteSdk.sharedEngine(with: profile)
        
        
        let config = AgoraRteSceneConfg()
        scene = agoraRteSdk.createRteScene(withSceneId: sceneId, sceneConfig: config)
        scene?.setSceneDelegate(self)
        
        localView = UIView()
        localView.frame = CGRect(x: self.view.bounds.width - 90, y: 0, width: 90, height: 130)
        self.view.addSubview(localView)
        
        
        
        let options = AgoraRteJoinOptions()
        scene.joinScene(withUserId: localUserId, token: "", joinOptions: options)
        
    }

}

extension ViewController: AgoraRteSceneDelegate {
    
    func agoraRteScene(_ rteScene: AgoraRteSceneProtocol, connectionStateDidChangeFromOldState oldState: AgoraConnectionState, toNewState state: AgoraConnectionState, with reason: AgoraConnectionChangedReason) {
        
        if state == AgoraConnectionState.connected {
            let mediaFactory = agoraRteSdk.rteMediaFactory()
            cameraTrack = mediaFactory?.createCameraVideoTrack()
                    
            let videoCanvas = AgoraRtcVideoCanvas()
            videoCanvas.userId = localUserId
            videoCanvas.view = localView
            videoCanvas.renderMode = .hidden
            
            cameraTrack?.setPreviewCanvas(videoCanvas)
            
            cameraTrack?.startCapture()
            
            microphoneTrack = mediaFactory?.createMicrophoneAudioTrack()
            microphoneTrack?.startRecording()
                    
            let streamOption = AgoraRteRtcStreamOptions()
            streamOption.token = ""
            scene?.createOrUpdateRTCStream(localStreamId, rtcStreamOptions: streamOption)
            scene?.publishLocalAudioTrack(localStreamId, rteAudioTrack: microphoneTrack!)
            scene?.publishLocalVideoTrack(localStreamId, rteVideoTrack: cameraTrack!)
        }
    }
    
    func agoraRteScene(_ rteScene: AgoraRteSceneProtocol, remoteStreamesDidAddWith streamInfos: [AgoraRteStreamInfo]?) {
        guard let infos = streamInfos else { return }
        for info in infos {
            rteScene.subscribeRemoteAudio(info.streamId!)
            let option = AgoraRteVideoSubscribeOptions()
            rteScene.subscribeRemoteVideo(info.streamId!, videoSubscribeOptions: option)
            
            DispatchQueue.main.async { [weak self] in
                guard let strongSelf = self else {
                    return
                }
                
                self!.remoteView = UIView()
                self!.remoteView.frame = self!.view.bounds
                self!.remoteView.tag = Int(info.streamId!)!
                self!.remoteStackView.addSubview(self!.remoteView)
                
                let videoCanvas = AgoraRtcVideoCanvas()
                videoCanvas.userId = info.userId!
                videoCanvas.view = strongSelf.remoteView
                videoCanvas.renderMode = .hidden
                rteScene.setRemoteVideoCanvas(info.streamId!, videoCanvas: videoCanvas)
            }
        }
    }
    
    func agoraRteScene(_ rteScene: AgoraRteSceneProtocol, remoteStreamDidRemoveWith streamInfos: [AgoraRteStreamInfo]?) {
        guard let infos = streamInfos else { return }
        for info in infos {
            rteScene.unsubscribeRemoteAudio(info.streamId!)
            rteScene.unsubscribeRemoteAudio(info.streamId!)
            
            let viewToRemove = self.remoteStackView.viewWithTag(Int(info.streamId!)!)!
            self.remoteStackView.removeArrangedSubview(viewToRemove)
            
            }
        }
        
    func agoraRteScene(_ rteScene: AgoraRteSceneProtocol, didConnectionStateChanged oldState: AgoraConnectionState, newState state: AgoraConnectionState, changedReason reason: AgoraConnectionChangedReason) {
        print("Connection state has changed to:\(state.rawValue) reason:\(reason.rawValue)")
    }
    
}

