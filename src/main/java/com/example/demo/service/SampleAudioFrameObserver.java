package com.example.demo.service;

import io.agora.rtc.AgoraLocalUser;
import io.agora.rtc.AudioFrame;
import io.agora.rtc.IAudioFrameObserver;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SampleAudioFrameObserver implements IAudioFrameObserver {
    protected final ExecutorService writeFileExecutorService = Executors.newSingleThreadExecutor();

    public SampleAudioFrameObserver() {

    }

    @Override
    public int onRecordAudioFrame(AgoraLocalUser agora_local_user, String channel_id, AudioFrame frame) {
        return 1;
    }

    @Override
    public int onPlaybackAudioFrame(AgoraLocalUser agora_local_user, String channel_id, AudioFrame frame) {
        return 1;
    }

    @Override
    public int onMixedAudioFrame(AgoraLocalUser agora_local_user, String channel_id, AudioFrame frame) {
        return 1;
    }

    @Override
    public int onEarMonitoringAudioFrame(AgoraLocalUser agora_local_user, AudioFrame frame) {
        return 1;
    }

    @Override
    public int onPlaybackAudioFrameBeforeMixing(AgoraLocalUser agora_local_user, String channel_id, String uid,
            AudioFrame frame) {
        return 0;
    }

    @Override
    public int getObservedAudioFramePosition() {
        return 15;
    }

}
