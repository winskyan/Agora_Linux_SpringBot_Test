package com.example.demo.agora;

import io.agora.rtc.AgoraVideoEncodedFrameObserver;
import io.agora.rtc.EncodedVideoFrameInfo;
import io.agora.rtc.IVideoEncodedFrameObserver;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.example.demo.engineer.PlayEngineer;
import com.example.demo.model.RoomConfig;
import com.example.demo.model.RoomMember;

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
    public int onEncodedVideoFrame(AgoraVideoEncodedFrameObserver agoraVideoEncodedFrameObserver, int user_id,
            ByteBuffer byteBuffer, EncodedVideoFrameInfo encodedVideoFrameInfo) {
        long timestamp = System.currentTimeMillis();
        boolean isKeyFrame = encodedVideoFrameInfo.getFrameType() == 3;
        RoomMember roomMember = ROOM_MEMBER_MAP.computeIfAbsent(user_id, k -> {
            log.info("agora user video room={},uid={}", roomConfig.getRoomId(), user_id);
            return new RoomMember();
        });

        byte[] data = new byte[byteBuffer.remaining()];
        byteBuffer.get(data);
        PlayEngineer.handleVideo(data, roomMember, isKeyFrame, timestamp);

        return 0;
    }
}
