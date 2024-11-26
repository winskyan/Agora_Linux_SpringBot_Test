package com.example.demo.api;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
import com.example.demo.service.SampleVideoEncodedFrameObserver;
import com.example.demo.service.Token;
import com.example.demo.utils.Utils;

import io.agora.rtc.AgoraLocalUser;
import io.agora.rtc.AgoraRtcConn;
import io.agora.rtc.AgoraService;
import io.agora.rtc.AgoraServiceConfig;
import io.agora.rtc.AgoraVideoEncodedFrameObserver;
import io.agora.rtc.AudioFrame;
import io.agora.rtc.AudioSubscriptionOptions;
import io.agora.rtc.EncodedVideoFrameInfo;
import io.agora.rtc.RtcConnConfig;
import io.agora.rtc.VideoSubscriptionOptions;
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

    private int MAX_USER = 8;

    private final ExecutorService testTaskExecutorService = Executors
            .newCachedThreadPool();
    private final ExecutorService logExecutorService = Executors
            .newCachedThreadPool();

    @GetMapping("start")
    public String start(@RequestParam String roomId) {
        SampleLogger.log("start roomId=" + roomId);

        String[] appIdAndToken = Utils.readAppIdAndToken(".keys");
        String appId = "";
        String token = "";
        if (null != appIdAndToken) {
            appId = appIdAndToken[0];
            token = appIdAndToken[1];// tokenSevice.buildToken(roomId, 0L);
        }
        SampleLogger.log("appId:" + appId + ",token:" + token);
        AgoraService service = new AgoraService();
        AgoraServiceConfig config = new AgoraServiceConfig();
        config.setEnableAudioProcessor(1);
        config.setEnableAudioDevice(0);
        config.setEnableVideo(1);
        config.setAppId(appId);
        // config.setLogFilePath("agora_logs/agorasdk.log");
        int ret = service.initialize(config);
        SampleLogger.log("initialize ret=" + ret);

        int numOfChannels = 1;
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

        testTaskExecutorService.execute(() -> {
            while (true) {
                try {
                    // 使用 Class.forName 方法加载类
                    Class<?> clazz = Class.forName("java.util.ArrayList");
                    SampleLogger.log("Class found: " + clazz.getName());
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }

                try {
                    Thread.sleep(2 * 1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        for (int i = 0; i < MAX_USER; i++) {
            final int index = i;
            final String conToken = token;
            testTaskExecutorService.execute(() -> {

                AgoraRtcConn conn = service.agoraRtcConnCreate(ccfg);
                // container.conns.put(roomId, conn);

                SampleLogger.log("connect conn=" + conn);

                int retR = conn.registerObserver(new ConnObserver());
                SampleLogger.log("registerObserver ret=" + retR);

                retR = conn.connect(conToken, roomId, "113" + index);
                SampleLogger.log("connect ret=" + ret);

                conn.getLocalUser().subscribeAllAudio();
                // Register local user observer
                SampleLocalUserObserver localUserObserver = new SampleLocalUserObserver(conn.getLocalUser());

                conn.getLocalUser().registerObserver(localUserObserver);

                // Register audio frame observer to receive audio stream
                retR = conn.getLocalUser().setPlaybackAudioFrameBeforeMixingParameters(numOfChannels, sampleRate);
                SampleLogger.log("setPlaybackAudioFrameBeforeMixingParameters  ret=" + retR);
                localUserObserver.setAudioFrameObserver(new SampleAudioFrameObserver() {
                    @Override
                    public int onPlaybackAudioFrameBeforeMixing(AgoraLocalUser agora_local_user, String channel_id,
                            String uid,
                            AudioFrame frame) {
                        logExecutorService.execute(() -> {
                            SampleLogger.log("onPlaybackAudioFrameBeforeMixing index:" + index);
                        });
                        return 1;
                    }
                });
                SampleLogger.log("setAudioFrameObserver ret=" + retR);

                VideoSubscriptionOptions subscriptionOptions = new VideoSubscriptionOptions();
                subscriptionOptions.setEncodedFrameOnly(1);
                subscriptionOptions.setType(Constants.VIDEO_STREAM_HIGH);

                conn.getLocalUser().subscribeAllVideo(subscriptionOptions);

                conn.getLocalUser()
                        .registerVideoEncodedFrameObserver(
                                new AgoraVideoEncodedFrameObserver(new SampleVideoEncodedFrameObserver("") {
                                    @Override
                                    public int onEncodedVideoFrame(
                                            AgoraVideoEncodedFrameObserver observer, int uid,
                                            ByteBuffer buffer, EncodedVideoFrameInfo info) {
                                        logExecutorService.execute(() -> {
                                            SampleLogger.log(
                                                    "onEncodedVideoFrame uid:" + uid + " index:" + index);
                                        });
                                        return 1;
                                    }
                                }));

            });
        }

        try {
            userLeftLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        SampleLogger.log("start end");
        return "new start";
    }
}
