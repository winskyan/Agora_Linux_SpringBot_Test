package com.example.demo.api;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Resource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.agora.AgoraPlayEngineer;
import com.example.demo.model.RoomConfig;
import com.example.demo.utils.Utils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api3/")
public class Api3 {
    @Resource
    private AgoraPlayEngineer agoraPlayEngineer;

    private final ExecutorService testTaskExecutorService = Executors
            .newCachedThreadPool();

    private CountDownLatch userLeftLatch = new CountDownLatch(1);

    @GetMapping("start")
    public String start(@RequestParam String roomId) {
        log.info("start roomId=" + roomId);

        String[] appIdAndToken = Utils.readAppIdAndToken("/home/yanzhennan/.keys");
        String appId = "";
        String token = "";
        if (null != appIdAndToken) {
            appId = appIdAndToken[0];
            token = appIdAndToken[1];// tokenSevice.buildToken(roomId, 0L);
        }

        agoraPlayEngineer.initialize(appId);

        final String uidString = "0";
        final String tokenString = token;
        testTaskExecutorService.execute(() -> {
            RoomConfig roomConfig = new RoomConfig();
            roomConfig.setRoomId(roomId);
            agoraPlayEngineer.joinRoom(tokenString, uidString, roomConfig);
            agoraPlayEngineer.sendAudio(roomId);
        });
        try {
            userLeftLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        testTaskExecutorService.execute(() -> {
            agoraPlayEngineer.leaveRoom(roomId);
        });
        return "new start";
    }

    @GetMapping("stop")
    public String stop(@RequestParam String roomId) {
        log.info("stop roomId=" + roomId);
        userLeftLatch.countDown();
        return "stop";
    }
}
