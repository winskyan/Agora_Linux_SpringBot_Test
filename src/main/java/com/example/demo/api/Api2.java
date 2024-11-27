package com.example.demo.api;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

// import jakarta.annotation.Resource;
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
@RequestMapping("/api2/")
public class Api2 {
    @Resource
    private AgoraPlayEngineer agoraPlayEngineer;

    private CountDownLatch userLeftLatch = new CountDownLatch(1);

    private int MAX_USER = 50;

    private final ExecutorService testTaskExecutorService = Executors
            .newCachedThreadPool();

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

        testTaskExecutorService.execute(() -> {
            while (true) {
                try {
                    // 使用 Class.forName 方法加载类
                    Class<?> clazz = Class.forName("java.util.ArrayList");
                    // log.info("Class found: " + clazz.getName());
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

        agoraPlayEngineer.initialize(appId);

        final int baseUid = ThreadLocalRandom.current().nextInt(1, 100);
        for (int i = 0; i < MAX_USER; i++) {
            final int index = i;
            final String conToken = token;
            testTaskExecutorService.execute(() -> {
                while (true) {
                    RoomConfig roomConfig = new RoomConfig();
                    roomConfig.setRoomId(roomId + index);
                    agoraPlayEngineer.joinRoom(conToken, baseUid + "" + index, roomConfig);

                    try {
                        int randomMinutes = ThreadLocalRandom.current().nextInt(10, 21);
                        Thread.sleep(randomMinutes * 60 * 1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    userLeftLatch.countDown();
                    agoraPlayEngineer.leaveRoom(roomConfig.getRoomId());
                }
            });
        }

        try {
            userLeftLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        log.info("start end");
        return "new start";
    }
}
