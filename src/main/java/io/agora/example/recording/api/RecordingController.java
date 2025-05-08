package io.agora.example.recording.api;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.Gson;
import io.agora.example.recording.agora.AgoraServiceInitializer;
import io.agora.example.recording.agora.RecorderConfig;
import io.agora.example.recording.agora.RecordingManager;
import io.agora.example.recording.utils.Utils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/recording")
public class RecordingController implements DisposableBean {

    private final RecordingManager recordingManager;
    private final AgoraServiceInitializer agoraServiceInitializer;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Gson gson = new Gson();

    public RecordingController() {
        this.recordingManager = new RecordingManager();
        this.agoraServiceInitializer = new AgoraServiceInitializer();
        log.info("RecordingController initialized.");
    }

    private String readFileFromResources(String fileName) {
        InputStream resourceStream = getClass().getClassLoader().getResourceAsStream(fileName);

        if (resourceStream == null) {
            log.error("Cannot find file in classpath: " + fileName);
            throw new RuntimeException("Cannot find file in classpath: " + fileName);
        }

        try (InputStream inputStream = resourceStream;
                InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            char[] buffer = new char[1024];
            StringBuilder stringBuilder = new StringBuilder();
            int numRead;
            while ((numRead = reader.read(buffer, 0, buffer.length)) != -1) {
                stringBuilder.append(buffer, 0, numRead);
            }
            return stringBuilder.toString();
        } catch (Exception e) {
            log.error("Error reading file from resources: " + fileName, e);
            throw new RuntimeException("Error reading file: " + fileName, e);
        }
    }

    @GetMapping("/start")
    public String startRecording(@RequestParam String configFileName) {
        String taskId = Utils.getTaskId();
        try {
            log.info("Received request to start recording for taskId: " + taskId + " with config file: "
                    + configFileName);

            String jsonConfigContent = readFileFromResources(configFileName);
            RecorderConfig config = gson.fromJson(jsonConfigContent, RecorderConfig.class);

            String[] keys = Utils.readAppIdAndToken(".keys");
            if (keys != null && keys.length == 2 && !io.agora.recording.utils.Utils.isNullOrEmpty(keys[0])
                    && !io.agora.recording.utils.Utils.isNullOrEmpty(keys[1])) {
                config.setAppId(keys[0]);
                config.setToken(keys[1]);
                log.info("AppId and Token loaded from .keys file for taskId: " + taskId);
            } else {
                log.warn("Could not load AppId and Token from .keys file for taskId: " + taskId
                        + ". Ensure .keys file is present and correctly formatted or AppId/Token are in the JSON config.");
            }

            log.info("Recording config for taskId " + taskId + ": " + config);

            agoraServiceInitializer.initService(config);

            executorService.submit(() -> {
                try {
                    log.info("Starting async recording for taskId: " + taskId);
                    recordingManager.startRecording(taskId, config);
                    log.info("Recording successfully started for taskId: " + taskId + " with config file: "
                            + configFileName);
                } catch (Exception e) {
                    log.error("Error during async recording for taskId: " + taskId, e);
                }
            });

            return "Recording process initiated for taskId: " + taskId + " with config: " + configFileName;

        } catch (Exception e) {
            log.error(
                    "Failed to start recording for taskId: " + taskId + " with config file: " + configFileName, e);
            return "Error starting recording for taskId: " + taskId + ". Reason: " + e.getMessage();
        }
    }

    @GetMapping("/stop")
    public String stopRecording(@RequestParam String taskId) {
        try {
            log.info("Received request to stop recording for taskId: " + taskId);
            recordingManager.stopRecording(taskId, false);
            log.info("Recording stopped for taskId: " + taskId);
            return "Recording stopped successfully for taskId: " + taskId;
        } catch (Exception e) {
            log.error("Failed to stop recording for taskId: " + taskId, e);
            return "Error stopping recording for taskId: " + taskId + ". Reason: " + e.getMessage();
        }
    }

    @Override
    public void destroy() throws Exception {
        log.info("Shutting down RecordingController. Cleaning up resources...");

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    log.error("ExecutorService did not terminate.");
                }
            }
        } catch (InterruptedException ie) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        if (recordingManager != null) {
            recordingManager.destroy();
            log.info("RecordingManager destroyed.");
        }
        // Assuming AgoraServiceInitializer.destroy() is a static method for global
        // cleanup
        AgoraServiceInitializer.destroy();
        log.info("AgoraServiceInitializer resources released.");

        log.info("RecordingController cleanup finished.");
    }
}