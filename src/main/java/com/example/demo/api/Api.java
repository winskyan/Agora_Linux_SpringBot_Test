package com.example.demo.api;

import java.util.concurrent.CountDownLatch;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.config.Config;
import com.example.demo.service.ConnObserver;
import com.example.demo.service.Container;
import com.example.demo.service.SampleAudioFrameObserver;
import com.example.demo.service.SampleLocalUserObserver;
import com.example.demo.service.SampleLogger;
import com.example.demo.service.Token;

import io.agora.rtc.AgoraLocalUser;
import io.agora.rtc.AgoraRtcConn;
import io.agora.rtc.AgoraService;
import io.agora.rtc.AgoraServiceConfig;
import io.agora.rtc.AudioFrame;
import io.agora.rtc.AudioSubscriptionOptions;
import io.agora.rtc.Constants;
import io.agora.rtc.RtcConnConfig;
import io.agora.rtc.SDK;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/")
public class Api {
    @Resource
    private Token tokenSevice;

    @Resource
    private Config liveConfig;

    @Resource
    private Container container;

    private CountDownLatch userLeftLatch = new CountDownLatch(1);

    @GetMapping("start")
    public String start(@RequestParam String roomId) {
        if (container.conns.containsKey(roomId)) {
            return "alreay running";
        }
        SampleLogger.log("start roomId=" + roomId);

        SDK.load();

        String token = "";// tokenSevice.buildToken(roomId, 0L);
        AgoraService service = new AgoraService();
        AgoraServiceConfig config = new AgoraServiceConfig();
        config.setEnableAudioProcessor(1);
        config.setEnableAudioDevice(0);
        config.setEnableVideo(1);
        config.setAppId("aab8b8f5a8cd4469a63042fcfafe7063");
        config.setLogFilePath("agora_logs/agorasdk.log");
        int ret = service.initialize(config);
        SampleLogger.log("initialize ret=" + ret);

        int numOfChannels = 1;
        // int sampleRate = 8000;
        int sampleRate = 16000;
        AudioSubscriptionOptions audioSubOpt = new AudioSubscriptionOptions();
        audioSubOpt.setBytesPerSample(2 * numOfChannels);
        audioSubOpt.setNumberOfChannels(numOfChannels);
        audioSubOpt.setSampleRateHz(sampleRate);

        RtcConnConfig ccfg = new RtcConnConfig();
        ccfg.setClientRoleType(Constants.CLIENT_ROLE_AUDIENCE);
        ccfg.setAudioSubsOptions(audioSubOpt);
        ccfg.setAutoSubscribeAudio(1);
        ccfg.setAutoSubscribeVideo(0);
        ccfg.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
        AgoraRtcConn conn = service.agoraRtcConnCreate(ccfg);
        // container.conns.put(roomId, conn);

        SampleLogger.log("connect conn=" + conn);

        ret = conn.registerObserver(new ConnObserver());
        SampleLogger.log("registerObserver ret=" + ret);

        ret = conn.connect("aab8b8f5a8cd4469a63042fcfafe7063", "test_channel", "11311");
        SampleLogger.log("connect ret=" + ret);

        conn.getLocalUser().subscribeAllAudio();
        // Register local user observer
        SampleLocalUserObserver localUserObserver = new SampleLocalUserObserver(conn.getLocalUser());

        conn.getLocalUser().registerObserver(localUserObserver);

        // Register audio frame observer to receive audio stream
        ret = conn.getLocalUser().setPlaybackAudioFrameBeforeMixingParameters(numOfChannels, sampleRate);
        SampleLogger.log("setPlaybackAudioFrameBeforeMixingParameters  ret=" + ret);
        localUserObserver.setAudioFrameObserver(new SampleAudioFrameObserver() {
            @Override
            public int onPlaybackAudioFrameBeforeMixing(AgoraLocalUser agora_local_user, String channel_id, String uid,
                    AudioFrame frame) {
                SampleLogger.log("onPlaybackAudioFrameBeforeMixing");
                return 1;
            }
        });
        SampleLogger.log("setAudioFrameObserver  ret=" + ret);

        try {
            userLeftLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        SampleLogger.log("start end");
        return "new start";
    }
}
