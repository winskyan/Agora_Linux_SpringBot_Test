package com.example.demo.service;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import com.example.demo.config.Config;

import io.agora.rtc.AgoraService;
import io.agora.rtc.AgoraServiceConfig;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class Context {
    @Resource
    Config liveConfig;

    @Bean
    public Token tokenService() {
        log.info("liveConfig: {}", liveConfig);
        Token tokenSevice = new Token(liveConfig.getAppId(), liveConfig.getAppToken(), 86400,
                86400);
        return tokenSevice;
    }

    @Bean
    public AgoraService agoraService() {
        log.info("Init AgoraService {}", liveConfig);
        AgoraServiceConfig config = new AgoraServiceConfig();
        config.setAppId(config.getAppId());
        config.setEnableAudioProcessor(1);
        config.setEnableAudioDevice(0);
        config.setEnableVideo(1);
        AgoraService agoraService = new AgoraService();
        agoraService.initialize(config);
        agoraService.setLogFile("sdk.runtime.log", Integer.MAX_VALUE);
        return agoraService;
    }

}
