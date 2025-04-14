package com.example.demo.agora;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.stereotype.Component;

import com.example.demo.model.RoomConfig;

import io.agora.rtc.AgoraAudioPcmDataSender;
import io.agora.rtc.AgoraLocalAudioTrack;
import io.agora.rtc.AgoraMediaNodeFactory;
import io.agora.rtc.AgoraRtcConn;
import io.agora.rtc.AgoraService;
import io.agora.rtc.AgoraServiceConfig;
import io.agora.rtc.AgoraVideoEncodedFrameObserver;
import io.agora.rtc.AudioSubscriptionOptions;
import io.agora.rtc.Constants;
import io.agora.rtc.RtcConnConfig;
import io.agora.rtc.SDK;
import io.agora.rtc.VideoSubscriptionOptions;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AgoraPlayEngineer {

    static {
        SDK.load();
    }

    private final Map<String, AgoraRtcConn> RTC_CONN_MAP = new ConcurrentHashMap<>();
    private final Map<String, AgoraVideoEncodedFrameObserver> RTC_OBSERVER_MAP = new ConcurrentHashMap<>();

    private static AgoraService agoraService;
    private static AgoraMediaNodeFactory mediaNodeFactory;

    private static AgoraAudioPcmDataSender audioFrameSender;
    private static AgoraLocalAudioTrack customAudioTrack;

    private static RtcConnConfig rtcConnConfig;

    public static final Map<String, AudioConsumerUtils> AUDIO_CONSUMER_UTILS_MAP = new ConcurrentHashMap<>();

    private static final AtomicBoolean init = new AtomicBoolean(false);

    private String appId;

    public void initialize(String appId) {
        log.info("agora engineer initialize appId:{}", appId);

        if (init.compareAndSet(false, true)) {
            agoraService = new AgoraService();
            AgoraServiceConfig agoraServiceConfig = new AgoraServiceConfig();
            agoraServiceConfig.setEnableAudioProcessor(1);
            agoraServiceConfig.setEnableAudioDevice(0);
            agoraServiceConfig.setEnableVideo(1);
            agoraServiceConfig.setContext(0);
            agoraServiceConfig.setAppId(appId);
            agoraServiceConfig.setAudioScenario(Constants.AUDIO_SCENARIO_CHORUS);
            int ret = agoraService.initialize(agoraServiceConfig);
            if (ret != 0) {
                log.info("agora initialize fail ret:" + ret);
            }
            rtcConnConfig = getRtcConnConfig();
            agoraService.setLogFile("agora_sdk.log", 10 * 1024);
            agoraService.setLogFilter(Constants.LOG_FILTER_INFO);
        }
    }

    public void joinRoom(String token, String userId, RoomConfig roomConfig) {
        RTC_CONN_MAP.computeIfAbsent(roomConfig.getRoomId(),
                roomId -> {
                    AgoraRtcConn agoraRtcConn = agoraService.agoraRtcConnCreate(rtcConnConfig);
                    if (agoraRtcConn == null) {
                        throw new RuntimeException("create AgoraRtcConn fail! roomId:" + roomId);
                    }

                    agoraRtcConn.getAgoraParameter().setParameters("{\"rtc.enable_nasa2\":false}");
                    // agoraRtcConn.getAgoraParameter().setParameters("{\"rtc.audio.enable_user_silence_packet\":true}");

                    AgoraRtcConnObserver iRtcConnObserver = new AgoraRtcConnObserver(roomId);
                    agoraRtcConn.registerObserver(iRtcConnObserver);

                    AgoraUserObserver iLocalUserObserver = new AgoraUserObserver(roomId);
                    agoraRtcConn.getLocalUser().registerObserver(iLocalUserObserver);

                    agoraRtcConn.getLocalUser().subscribeAllAudio();
                    int ret = agoraRtcConn.getLocalUser().setPlaybackAudioFrameBeforeMixingParameters(1, 16000);
                    if (ret != 0) {
                        log.error("agora setPlaybackAudioFrameBeforeMixingParameters fail ret:{}", ret);
                    }
                    AgoraAudioFrameObserver iAudioFrameObserver = new AgoraAudioFrameObserver(roomConfig, appId);
                    ret = agoraRtcConn.getLocalUser().registerAudioFrameObserver(iAudioFrameObserver);
                    if (ret != 0) {
                        throw new RuntimeException(
                                "agora registerAudioFrameObserver fail ret:" + ret + " roomId:" + roomId);
                    }

                    VideoSubscriptionOptions videoSubOptions = new VideoSubscriptionOptions();
                    videoSubOptions.setEncodedFrameOnly(1);
                    videoSubOptions.setType(Constants.VIDEO_STREAM_HIGH);
                    agoraRtcConn.getLocalUser().subscribeAllVideo(videoSubOptions);

                    AgoraVideoFrameObserver videoFrameObserver = new AgoraVideoFrameObserver(roomConfig, appId);
                    AgoraVideoEncodedFrameObserver agoraVideoEncodedFrameObserver = new AgoraVideoEncodedFrameObserver(
                            videoFrameObserver);
                    agoraRtcConn.getLocalUser()
                            .registerVideoEncodedFrameObserver(agoraVideoEncodedFrameObserver);
                    RTC_OBSERVER_MAP.put(roomId, agoraVideoEncodedFrameObserver);

                    ret = agoraRtcConn.connect(token, roomId, userId);
                    if (ret != 0) {
                        throw new RuntimeException("connect agora room fail ret:" + ret + " roomId:" + roomId
                                + " userId:" + userId);
                    }

                    // Create audio track
                    mediaNodeFactory = agoraService.createMediaNodeFactory();
                    audioFrameSender = mediaNodeFactory.createAudioPcmDataSender();
                    customAudioTrack = agoraService.createCustomAudioTrackPcm(audioFrameSender);
                    agoraRtcConn.getLocalUser().publishAudio(customAudioTrack);

                    AudioConsumerUtils audioConsumerUtils = new AudioConsumerUtils(audioFrameSender, 1, 16000);
                    AUDIO_CONSUMER_UTILS_MAP.put(roomId, audioConsumerUtils);

                    log.info("agora joinRoom roomId={},userId={}", roomId, userId);

                    return agoraRtcConn;
                });
    }

    public void sendAudio(String roomId) {
        log.info("agora sendAudio roomId={}", roomId);
        AgoraRtcConn agoraRtcConn = RTC_CONN_MAP.get(roomId);
        if (agoraRtcConn == null) {
            return;
        }
        while (true) {
            AudioConsumerUtils audioConsumerUtils = AUDIO_CONSUMER_UTILS_MAP.get(roomId);
            if (audioConsumerUtils == null) {
                return;
            }
            int sendCount = audioConsumerUtils.consume();
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void leaveRoom(String roomId) {
        AgoraRtcConn agoraRtcConn = RTC_CONN_MAP.remove(roomId);
        if (agoraRtcConn == null) {
            return;
        }
        log.info("agora leaveRoom roomId:{}", roomId);

        AudioConsumerUtils audioConsumerUtils = AUDIO_CONSUMER_UTILS_MAP.remove(roomId);
        if (audioConsumerUtils != null) {
            audioConsumerUtils.release();
        }

        if (null != mediaNodeFactory) {
            mediaNodeFactory.destroy();
            mediaNodeFactory = null;
        }
        if (null != audioFrameSender) {
            audioFrameSender.destroy();
            audioFrameSender = null;
        }

        if (null != customAudioTrack) {
            agoraRtcConn.getLocalUser().unpublishAudio(customAudioTrack);
            customAudioTrack.destroy();
            customAudioTrack = null;
        }

        agoraRtcConn.getLocalUser().unsubscribeAllAudio();
        agoraRtcConn.getLocalUser().unsubscribeAllVideo();
        agoraRtcConn.getLocalUser().unregisterAudioFrameObserver();
        AgoraVideoEncodedFrameObserver observer = RTC_OBSERVER_MAP.get(roomId);
        agoraRtcConn.getLocalUser().unregisterVideoEncodedFrameObserver(observer);
        observer.destroy();
        RTC_OBSERVER_MAP.remove(roomId);
        agoraRtcConn.unregisterObserver();
        agoraRtcConn.disconnect();
        agoraRtcConn.destroy();
    }

    public void destroy() {
        log.info("agora engineer destroy appId:{}", appId);
        for (Map.Entry<String, AgoraRtcConn> entry : RTC_CONN_MAP.entrySet()) {
            leaveRoom(entry.getKey());
        }
        if (init.compareAndSet(true, false)) {
            agoraService.destroy();
            agoraService = null;
        }
    }

    private static RtcConnConfig getRtcConnConfig() {
        AudioSubscriptionOptions audioSubOpt = new AudioSubscriptionOptions();
        audioSubOpt.setBytesPerSample(2);
        audioSubOpt.setNumberOfChannels(1);
        audioSubOpt.setSampleRateHz(16000);

        RtcConnConfig rtcConnConfig = new RtcConnConfig();
        rtcConnConfig.setClientRoleType(Constants.CLIENT_ROLE_BROADCASTER);
        rtcConnConfig.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
        rtcConnConfig.setAudioSubsOptions(audioSubOpt);
        rtcConnConfig.setAutoSubscribeAudio(0);
        rtcConnConfig.setAutoSubscribeVideo(0);
        rtcConnConfig.setEnableAudioRecordingOrPlayout(0);
        return rtcConnConfig;
    }

}
