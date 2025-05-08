package io.agora.example.server.agora;

import io.agora.example.server.engineer.PlayWorker;
import io.agora.rtc.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AgoraUserObserver extends DefaultLocalUserObserver {

    private final String roomId;

    public AgoraUserObserver(String roomId) {
        this.roomId = roomId;
    }

    @Override
    public void onUserAudioTrackStateChanged(AgoraLocalUser agora_local_user, String user_id,
            AgoraRemoteAudioTrack agora_remote_audio_track, int state, int reason, int elapsed) {
        if (state == 0) {
            log.info("agora onUserAudioTrackStateChanged roomId:{},userId:{},state:{},reason:{},elapse:{}", roomId,
                    user_id, state, reason, elapsed);
            PlayWorker.removeAudioSegment(roomId, user_id);
        }
    }

    @Override
    public void onUserVideoTrackStateChanged(AgoraLocalUser agoraLocalUser, String user_id,
            AgoraRemoteVideoTrack agoraRemoteVideoTrack, int state, int reason, int elapse) {
        // state 是 0, 表示视频停止
        if (state == 0) {
            log.info("agora onUserVideoTrackStateChanged roomId:{},userId:{},state:{},reason:{},elapse:{}", roomId,
                    user_id, state, reason, elapse);
            PlayWorker.removeVideoSegment(roomId, user_id);
        }
    }

}