package io.agora.example.server.engineer;

import io.agora.example.server.model.RoomMember;
import java.nio.ByteBuffer;

public class PlayEngineer {
    public static void handleVideo(byte[] data, RoomMember roomMember, boolean isKeyFrame, long timestamp) {
    }

    public static void handleAudio(ByteBuffer buffer, RoomMember roomMember, long timestamp) {
    }
}
