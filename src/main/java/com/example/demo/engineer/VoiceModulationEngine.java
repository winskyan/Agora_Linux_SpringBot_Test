package com.example.demo.engineer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.example.demo.agora.AgoraPlayEngineer;
import com.example.demo.agora.AudioConsumerUtils;

public class VoiceModulationEngine {
    private static final ExecutorService taskExecutorService = Executors
            .newSingleThreadExecutor();

    public static void handleAudio(byte[] data, String roomId, long timestamp) {
        taskExecutorService.execute(() -> {
            try {
                // 模拟audio处理时间
                Thread.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            AudioConsumerUtils audioConsumerUtils = AgoraPlayEngineer.AUDIO_CONSUMER_UTILS_MAP.get(roomId);
            if (audioConsumerUtils == null) {
                return;
            }
            audioConsumerUtils.pushPcmData(data);
        });
    }

    public void release() {
        taskExecutorService.shutdown();
    }
}
