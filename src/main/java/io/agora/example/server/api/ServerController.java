package io.agora.example.server.api;

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.Resource;

import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.agora.example.server.agora.AgoraPlayEngineer;
import io.agora.example.server.model.RoomConfig;
import io.agora.example.utils.Utils;
import lombok.extern.slf4j.Slf4j;

@Lazy
@Slf4j
@RestController
@RequestMapping("/api/server")
public class ServerController {
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
                getCpuTime();

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

    private void getCpuTime() {
        try {
            // 获取 ThreadImpl 类的 Class 对象
            Class<?> threadImplClass = Class.forName("sun.management.ThreadImpl");

            // 获取 getThreadTotalCpuTime0 方法
            Method getThreadTotalCpuTime0Method = threadImplClass.getDeclaredMethod("getThreadTotalCpuTime0",
                    long.class);

            // 设置方法为可访问
            getThreadTotalCpuTime0Method.setAccessible(true);

            // 获取当前线程的ID
            long threadId = Thread.currentThread().getId();

            // 调用 getThreadTotalCpuTime0 方法
            long cpuTime = (long) getThreadTotalCpuTime0Method.invoke(null, threadId);

            log.info("Current thread CPU time: " + cpuTime + " ns");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
