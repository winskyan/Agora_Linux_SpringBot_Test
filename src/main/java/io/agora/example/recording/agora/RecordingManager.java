package io.agora.example.recording.agora;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RecordingManager {
    private final Map<String, RecordingSession> activeRecordings = new ConcurrentHashMap<>();
    private final ThreadPoolExecutor taskExecutorService;

    public RecordingManager() {
        this.taskExecutorService = new ThreadPoolExecutor(
                0, Integer.MAX_VALUE, 1L, TimeUnit.SECONDS, new SynchronousQueue<>());
    }

    public void startRecording(String taskId, RecorderConfig config) {
        startRecording(taskId, config, "");
    }

    public synchronized void startRecording(String taskId, RecorderConfig config, String channelName) {
        RecordingSession session = new RecordingSession(taskId, config, taskExecutorService);
        activeRecordings.put(taskId, session);
        taskExecutorService.submit(() -> session.joinChannel(channelName));
    }

    public synchronized void stopRecording(String taskId, boolean async) {
        RecordingSession session = activeRecordings.get(taskId);
        if (session != null) {
            if (async) {
                taskExecutorService.submit(() -> session.stopRecording());
            } else {
                session.stopRecording();
            }
            activeRecordings.remove(taskId);
        }
    }

    public void destroy() {
        activeRecordings.clear();
        taskExecutorService.shutdown();
    }
}
