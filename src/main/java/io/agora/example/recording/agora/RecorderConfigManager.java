package io.agora.example.recording.agora;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import io.agora.example.recording.utils.Utils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RecorderConfigManager {
    private static RecorderConfig config;
    private static int sleepTime;
    private static int threadNum = 1;
    private static int testTime = 0;
    private static int oneTestTime = 0;

    public static void parseArgs(String[] args) {
        log.info("parseArgs args:" + Arrays.toString(args));
        if (args == null || args.length == 0) {
            return;
        }

        Map<String, String> params = new HashMap<>();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("-")) {
                String key = arg.substring(1);
                if (i + 1 < args.length) {
                    String value = args[i + 1];
                    params.put(key, value);
                    i++;
                }
            }
        }

        Gson gson = new Gson();

        if (params.containsKey("config")) {
            config = gson.fromJson(io.agora.recording.utils.Utils.readFile(params.get("config")),
                    RecorderConfig.class);
        } else {
            config = new RecorderConfig();
        }

        if (params.containsKey("sleepTime")) {
            sleepTime = Integer.parseInt(params.get("sleepTime"));
        }

        if (params.containsKey("threadNum")) {
            threadNum = Integer.parseInt(params.get("threadNum"));
        }

        if (params.containsKey("testTime")) {
            testTime = Integer.parseInt(params.get("testTime"));
        }

        if (params.containsKey("oneTestTime")) {
            oneTestTime = Integer.parseInt(params.get("oneTestTime"));
        }

        String[] keys = Utils.readAppIdAndToken(".keys");
        if (keys != null && keys.length == 2 && !io.agora.recording.utils.Utils.isNullOrEmpty(keys[0])
                && !io.agora.recording.utils.Utils.isNullOrEmpty(keys[1])) {
            config.setAppId(keys[0]);
            config.setToken(keys[1]);
        }
    }

    public static RecorderConfig getConfig() {
        return config;
    }

    public static int getSleepTime() {
        return sleepTime;
    }

    public static int getThreadNum() {
        return threadNum;
    }

    public static int getTestTime() {
        return testTime;
    }

    public static int getOneTestTime() {
        return oneTestTime;
    }
}
