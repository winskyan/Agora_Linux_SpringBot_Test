package com.example.demo.agora;

import com.example.demo.engineer.PlayWorker;

import io.agora.rtc.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AgoraRtcConnObserver extends DefaultRtcConnObserver {

    private final String roomId;

    public AgoraRtcConnObserver(String roomId) {
        this.roomId = roomId;
    }

    @Override
    public void onUserJoined(AgoraRtcConn agoraRtcConn, String user_id) {
        log.info("agora onUserJoined roomId:{},userId:{}", roomId, user_id);
        PlayWorker.incUserCount(roomId);
    }

    public void onUserLeft(AgoraRtcConn agoraRtcConn, String user_id, int reason) {
        log.info("agora onUserLeft roomId:{},userId:{},reason:{}", roomId, user_id, reason);
        PlayWorker.decUserCount(roomId);
        PlayWorker.removeUserSegment(roomId, user_id);
    }

}