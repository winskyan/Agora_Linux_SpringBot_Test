package io.agora.example.server.agora;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.stereotype.Component;
import io.agora.example.server.model.RoomConfig;
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
    private final Map<String, AgoraRtcConn> RTC_CONN_MAP = new ConcurrentHashMap<>();
    private final Map<String, AgoraVideoEncodedFrameObserver> RTC_OBSERVER_MAP = new ConcurrentHashMap<>();

    private static AgoraService agoraService;

    private static RtcConnConfig rtcConnConfig;

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
            int ret = agoraService.initialize(agoraServiceConfig);
            if (ret != 0) {
                log.error("agora initialize fail ret:" + ret);
            }
            rtcConnConfig = getRtcConnConfig();
            agoraService.setLogFile("logs/agora_sdk.log", 10 * 1024);
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
                    log.info("agora joinRoom roomId={},userId={}", roomId, userId);

                    return agoraRtcConn;
                });
    }

    public void leaveRoom(String roomId) {
        AgoraRtcConn agoraRtcConn = RTC_CONN_MAP.remove(roomId);
        if (agoraRtcConn == null) {
            return;
        }
        log.info("agora leaveRoom roomId:{}", roomId);
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
        rtcConnConfig.setClientRoleType(Constants.CLIENT_ROLE_AUDIENCE);
        rtcConnConfig.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
        rtcConnConfig.setAudioSubsOptions(audioSubOpt);
        rtcConnConfig.setAutoSubscribeAudio(0);
        rtcConnConfig.setAutoSubscribeVideo(0);
        rtcConnConfig.setEnableAudioRecordingOrPlayout(0);
        return rtcConnConfig;
    }

}
