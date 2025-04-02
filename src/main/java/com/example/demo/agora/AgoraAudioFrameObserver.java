package com.example.demo.agora;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.example.demo.engineer.PlayEngineer;
import com.example.demo.model.RoomConfig;
import com.example.demo.model.RoomMember;

import io.agora.rtc.AgoraLocalUser;
import io.agora.rtc.AudioFrame;
import io.agora.rtc.IAudioFrameObserver;
import io.agora.rtc.VadProcessResult;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AgoraAudioFrameObserver implements IAudioFrameObserver {

    private final Map<String, RoomMember> ROOM_MEMBER_MAP = new ConcurrentHashMap<>();

    private final RoomConfig roomConfig;

    private final String appId;

    public AgoraAudioFrameObserver(RoomConfig roomConfig, String appId) {
        this.roomConfig = roomConfig;
        this.appId = appId;
    }

    @Override
    public int onRecordAudioFrame(AgoraLocalUser agoraLocalUser, String s, AudioFrame audioFrame) {
        return 0;
    }

    @Override
    public int onPlaybackAudioFrame(AgoraLocalUser agoraLocalUser, String s, AudioFrame audioFrame) {
        return 0;
    }

    @Override
    public int onMixedAudioFrame(AgoraLocalUser agoraLocalUser, String s, AudioFrame audioFrame) {
        return 0;
    }

    @Override
    public int onEarMonitoringAudioFrame(AgoraLocalUser agoraLocalUser, AudioFrame audioFrame) {
        return 0;
    }

    @Override
    public int onPlaybackAudioFrameBeforeMixing(AgoraLocalUser agoraLocalUser, String channel_id, String uid,
            AudioFrame audioFrame, VadProcessResult vadProcessResult) {
        // 处理pcm数据
        long timestamp = System.currentTimeMillis();
        RoomMember roomMember = ROOM_MEMBER_MAP.computeIfAbsent(uid, k -> {
            log.info("agora user audio room={},uid={}", channel_id, uid);
            return new RoomMember();
        });
        PlayEngineer.handleAudio(audioFrame.getBuffer(), roomMember, timestamp);
        return 0;
    }

    @Override
    public int getObservedAudioFramePosition() {
        return 0;
    }
}