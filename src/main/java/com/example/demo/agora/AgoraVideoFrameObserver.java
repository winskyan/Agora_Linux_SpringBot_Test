package com.example.demo.agora;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.example.demo.engineer.PlayEngineer;
import com.example.demo.model.RoomConfig;
import com.example.demo.model.RoomMember;

import io.agora.rtc.AgoraVideoEncodedFrameObserver;
import io.agora.rtc.EncodedVideoFrameInfo;
import io.agora.rtc.IVideoEncodedFrameObserver;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AgoraVideoFrameObserver implements IVideoEncodedFrameObserver {

    private final Map<Integer, RoomMember> ROOM_MEMBER_MAP = new ConcurrentHashMap<>();

    private final RoomConfig roomConfig;

    private final String appId;

    public AgoraVideoFrameObserver(RoomConfig roomConfig, String appId) {
        this.roomConfig = roomConfig;
        this.appId = appId;
    }

    @Override
    public int onEncodedVideoFrame(AgoraVideoEncodedFrameObserver agora_video_encoded_frame_observer, int uid,
            byte[] image_buffer, long length, EncodedVideoFrameInfo video_encoded_frame_info) {
        long timestamp = System.currentTimeMillis();
        boolean isKeyFrame = video_encoded_frame_info.getFrameType() == 3;
        RoomMember roomMember = ROOM_MEMBER_MAP.computeIfAbsent(uid, k -> {
            log.info("agora user video room={},uid={}", roomConfig.getRoomId(), uid);
            return new RoomMember();
        });

        PlayEngineer.handleVideo(image_buffer, roomMember, isKeyFrame, timestamp);

        return 0;
    }
}
