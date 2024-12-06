package com.example.demo.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.agora.rtc.AgoraVideoEncodedFrameObserver;
import io.agora.rtc.EncodedVideoFrameInfo;
import io.agora.rtc.IVideoEncodedFrameObserver;

public class SampleVideoEncodedFrameObserver implements IVideoEncodedFrameObserver {
    protected final ExecutorService writeFileExecutorService = Executors.newSingleThreadExecutor();
    private String outputFilePath = "";

    public SampleVideoEncodedFrameObserver(String outputFilePath) {
        this.outputFilePath = outputFilePath;
    }

    @Override
    public int onEncodedVideoFrame(AgoraVideoEncodedFrameObserver agora_video_encoded_frame_observer, int uid,
    byte[] image_buffer, long length, EncodedVideoFrameInfo video_encoded_frame_info) {
        return 1;
    }

}
