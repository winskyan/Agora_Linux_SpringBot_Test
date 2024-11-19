package com.example.demo.service;

import java.nio.ByteBuffer;
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
    public int onEncodedVideoFrame(AgoraVideoEncodedFrameObserver observer, int uid,
            ByteBuffer buffer, EncodedVideoFrameInfo info) {
        return 1;
    }

}
