package io.agora.example.recording.agora;

import io.agora.example.recording.model.RecordingUserInfo;
import io.agora.example.recording.utils.Utils;
import io.agora.recording.AgoraMediaComponentFactory;
import io.agora.recording.AgoraMediaRtcRecorder;
import io.agora.recording.AgoraService;
import io.agora.recording.Constants;
import io.agora.recording.Constants.WatermarkSourceType;
import io.agora.recording.EncryptionConfig;
import io.agora.recording.IAgoraMediaRtcRecorderEventHandler;
import io.agora.recording.MediaRecorderConfiguration;
import io.agora.recording.RecorderInfo;
import io.agora.recording.RemoteAudioStatistics;
import io.agora.recording.RemoteVideoStatistics;
import io.agora.recording.SpeakVolumeInfo;
import io.agora.recording.VideoSubscriptionOptions;
import io.agora.recording.WatermarkConfig;
import io.agora.recording.WatermarkLitera;
import io.agora.recording.WatermarkOptions;
import io.agora.recording.WatermarkTimestamp;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RecordingSession implements IAgoraMediaRtcRecorderEventHandler {
    private final String taskId;
    private final RecorderConfig recorderConfig;
    private final ThreadPoolExecutor taskExecutorService;

    private AgoraMediaRtcRecorder agoraMediaRtcRecorder;
    private AgoraService agoraService = null;
    private AgoraMediaComponentFactory factory = null;
    private final List<String> singleRecordingUserList = new CopyOnWriteArrayList<>();
    private VideoLayoutManager videoLayoutManager;
    private Constants.RecorderState recorderState = io.agora.recording.Constants.RecorderState.RECORDER_STATE_ERROR;
    private final AtomicBoolean startRecordingDone = new AtomicBoolean(false);
    private final List<RecordingUserInfo> waitForUpdateUiUserInfos = new CopyOnWriteArrayList<>();

    private String channelNameInternal;

    public RecordingSession(String taskId, RecorderConfig recorderConfig, ThreadPoolExecutor taskExecutorService) {
        this.taskId = taskId;
        this.recorderConfig = recorderConfig;
        this.agoraService = AgoraServiceInitializer.getAgoraService();
        this.factory = AgoraServiceInitializer.getFactory();
        this.taskExecutorService = taskExecutorService;
    }

    public void joinChannel(String channelName) {
        log.info("[" + taskId + "]joinChannel channelName:" + channelName);
        if (null == agoraService || null == factory || null == recorderConfig) {
            log.info("createRtcRecorder agoraService or factory  or eventHandler or recordingConfig is null");
            return;
        }

        if (null == agoraService || null == factory || null == recorderConfig) {
            log.info("createRtcRecorder agoraService or factory  or eventHandler or recordingConfig is null");
            return;
        }

        agoraMediaRtcRecorder = factory.createMediaRtcRecorder();
        agoraMediaRtcRecorder.initialize(agoraService, recorderConfig.isMix());
        agoraMediaRtcRecorder.registerRecorderEventHandler(this);
        agoraMediaRtcRecorder.setAudioVolumeIndicationParameters(500);

        this.videoLayoutManager = new VideoLayoutManager(agoraMediaRtcRecorder, recorderConfig);

        boolean enableEncryption = !io.agora.recording.utils.Utils
                .isNullOrEmpty(recorderConfig.getEncryption().getMode());

        if (enableEncryption) {
            EncryptionConfig encryptionConfig = new EncryptionConfig();
            encryptionConfig.setEncryptionMode(Utils
                    .convertToEncryptionMode(recorderConfig.getEncryption().getMode()));
            encryptionConfig.setEncryptionKey(recorderConfig.getEncryption().getKey());
            if (!io.agora.recording.utils.Utils.isNullOrEmpty(recorderConfig.getEncryption().getSalt())) {
                encryptionConfig.setEncryptionKdfSalt(
                        io.agora.recording.utils.Utils.byteStringToByteArray(recorderConfig.getEncryption().getSalt()));
            }
            log.info("[" + taskId + "]joinChannel enableEncryption encryptionConfig:" + encryptionConfig);
            int ret = agoraMediaRtcRecorder.enableEncryption(true, encryptionConfig);
            log.info("[" + taskId + "]joinChannel enableEncryption ret:" + ret);
        }

        if (recorderConfig.isSubAllAudio()) {
            agoraMediaRtcRecorder.subscribeAllAudio();
        } else {
            for (String userId : recorderConfig.getSubAudioUserList()) {
                agoraMediaRtcRecorder.subscribeAudio(userId);
            }
        }
        VideoSubscriptionOptions options = new VideoSubscriptionOptions();
        options.setEncodedFrameOnly(false);
        options.setType(
                Utils.convertToVideoStreamType(recorderConfig.getSubStreamType()));
        if (recorderConfig.isSubAllVideo()) {
            agoraMediaRtcRecorder.subscribeAllVideo(options);
        } else {
            for (String userId : recorderConfig.getSubVideoUserList()) {
                agoraMediaRtcRecorder.subscribeVideo(userId, options);
            }
        }
        channelNameInternal = recorderConfig.getChannelName();
        if (!io.agora.recording.utils.Utils.isNullOrEmpty(channelName)) {
            channelNameInternal = channelName;
        }
        agoraMediaRtcRecorder.joinChannel(recorderConfig.getToken(),
                channelNameInternal,
                recorderConfig.getUserId());

        startRecordingDone.set(false);
    }

    private void setConfigBeforeStartRecording(String userId) {
        if (null != recorderConfig.getWaterMark() && recorderConfig.getWaterMark().size() > 0) {
            WatermarkConfig[] watermarks = new WatermarkConfig[recorderConfig.getWaterMark().size()];
            for (int i = 0; i < recorderConfig.getWaterMark().size(); i++) {
                watermarks[i] = new WatermarkConfig();
                watermarks[i].setIndex(i + 1);
                WatermarkSourceType watermarkSourceType = Utils
                        .convertToWatermarkSourceType(recorderConfig.getWaterMark().get(i).getType());
                watermarks[i].setType(watermarkSourceType);
                if (watermarkSourceType == WatermarkSourceType.LITERA) {
                    WatermarkLitera watermarkLitera = new WatermarkLitera();
                    watermarkLitera.setWmLitera(recorderConfig.getWaterMark().get(i).getLitera());
                    watermarkLitera.setFontFilePath(recorderConfig.getWaterMark().get(i).getFontFilePath());
                    watermarkLitera.setFontSize(recorderConfig.getWaterMark().get(i).getFontSize());
                    watermarks[i].setLiteraSource(watermarkLitera);
                } else if (watermarkSourceType == WatermarkSourceType.TIMESTAMPS) {
                    WatermarkTimestamp watermarkTimestamp = new WatermarkTimestamp();
                    watermarkTimestamp.setFontFilePath(recorderConfig.getWaterMark().get(i).getFontFilePath());
                    watermarkTimestamp.setFontSize(recorderConfig.getWaterMark().get(i).getFontSize());
                    watermarks[i].setTimestampSource(watermarkTimestamp);
                } else if (watermarkSourceType == WatermarkSourceType.PICTURE) {
                    watermarks[i].setImageUrl(recorderConfig.getWaterMark().get(i).getImgUrl());
                }

                WatermarkOptions watermarkOptions = new WatermarkOptions();
                watermarkOptions.setMode(Constants.WaterMaskFitMode.FIT_MODE_COVER_POSITION);
                watermarkOptions.setZOrder(recorderConfig.getWaterMark().get(i).getZorder());

                io.agora.recording.Rectangle positionInPortraitMode = new io.agora.recording.Rectangle();
                positionInPortraitMode.setX(recorderConfig.getWaterMark().get(i).getX());
                positionInPortraitMode.setY(recorderConfig.getWaterMark().get(i).getY());
                positionInPortraitMode.setWidth(recorderConfig.getWaterMark().get(i).getWidth());
                positionInPortraitMode.setHeight(recorderConfig.getWaterMark().get(i).getHeight());

                io.agora.recording.Rectangle positionInLandscapeMode = new io.agora.recording.Rectangle();
                positionInLandscapeMode.setX(recorderConfig.getWaterMark().get(i).getX());
                positionInLandscapeMode.setY(recorderConfig.getWaterMark().get(i).getY());
                positionInLandscapeMode.setWidth(recorderConfig.getWaterMark().get(i).getWidth());
                positionInLandscapeMode.setHeight(recorderConfig.getWaterMark().get(i).getHeight());

                watermarkOptions.setPositionInLandscapeMode(positionInLandscapeMode);
                watermarkOptions.setPositionInPortraitMode(positionInPortraitMode);
                watermarks[i].setOptions(watermarkOptions);
            }

            log.info("[" + taskId + "] enableAndUpdateVideoWatermarks watermarks:" + Arrays.toString(watermarks));

            int ret = -1;

            if (io.agora.recording.utils.Utils.isNullOrEmpty(userId)) {
                ret = agoraMediaRtcRecorder.enableAndUpdateVideoWatermarks(watermarks);
            } else {
                ret = agoraMediaRtcRecorder.enableAndUpdateVideoWatermarksByUid(watermarks, userId);
            }
            log.info("[" + taskId + "] enableAndUpdateVideoWatermarks userId " + userId + " ret:" + ret);
        }

    }

    public void startRecording(String userId, int width, int height) {
        if (null == agoraMediaRtcRecorder) {
            log.info("startRecording agoraMediaRtcRecorder is null");
            return;
        }
        log.info("[" + taskId + "]startRecording channelName:" + channelNameInternal + " userId:" + userId
                + " width:" + width + " height:" + height);
        if (singleRecordingUserList.contains(userId)) {
            log.info("[" + taskId + "]startRecording userId:" + userId + " is already recording");
            return;
        }

        MediaRecorderConfiguration mediaRecorderConfiguration = new MediaRecorderConfiguration();
        mediaRecorderConfiguration.setWidth(width != 0 ? width : recorderConfig.getVideo().getWidth());
        mediaRecorderConfiguration.setHeight(height != 0 ? height : recorderConfig.getVideo().getHeight());
        mediaRecorderConfiguration.setFps(recorderConfig.getVideo().getFps());
        mediaRecorderConfiguration.setMaxDurationMs(recorderConfig.getMaxDuration() * 1000);
        if (io.agora.recording.utils.Utils.isNullOrEmpty(userId)) {
            mediaRecorderConfiguration.setStoragePath(
                    recorderConfig.getRecorderPath().substring(0, recorderConfig.getRecorderPath().lastIndexOf(".mp4"))
                            + "_" + channelNameInternal + "_"
                            + io.agora.recording.utils.Utils.formatTimestamp(System.currentTimeMillis(),
                                    "yyyyMMdd-HHmmss")
                            + ".mp4");
        } else {
            mediaRecorderConfiguration
                    .setStoragePath(recorderConfig.getRecorderPath() + userId + "_" + channelNameInternal
                            + "_" + io.agora.recording.utils.Utils
                                    .formatTimestamp(System.currentTimeMillis(), "yyyyMMdd-HHmmss")
                            + ".mp4");
        }
        mediaRecorderConfiguration.setSampleRate(recorderConfig.getAudio().getSampleRate());
        mediaRecorderConfiguration.setChannelNum(recorderConfig.getAudio().getNumOfChannels());
        mediaRecorderConfiguration
                .setStreamType(Utils
                        .convertToMediaRecorderStreamType(recorderConfig.getRecorderStreamType()));
        mediaRecorderConfiguration
                .setVideoSourceType(io.agora.recording.Constants.VideoSourceType.VIDEO_SOURCE_CAMERA_SECONDARY);

        setConfigBeforeStartRecording(userId);

        log.info("[" + taskId + "]startRecording mediaRecorderConfiguration:" + mediaRecorderConfiguration);

        int ret = -1;
        if (io.agora.recording.utils.Utils.isNullOrEmpty(userId)) {
            ret = agoraMediaRtcRecorder.setRecorderConfig(mediaRecorderConfiguration);
            log.info("[" + taskId + "]startRecording setRecorderConfig ret:" + ret);
            ret = agoraMediaRtcRecorder.startRecording();
            log.info("[" + taskId + "]startRecording  ret:" + ret);
        } else {
            ret = agoraMediaRtcRecorder.setRecorderConfigByUid(mediaRecorderConfiguration, userId);
            log.info("[" + taskId + "]startRecording setRecorderConfigByUid ret:" + ret);
            ret = agoraMediaRtcRecorder.startSingleRecordingByUid(userId);
            singleRecordingUserList.add(userId);
            log.info("[" + taskId + "]startRecordingByUserId userId:" + userId + " ret:"
                    + ret);
        }
        startRecordingDone.set(true);

        if (!waitForUpdateUiUserInfos.isEmpty()) {
            for (RecordingUserInfo waitUserId : waitForUpdateUiUserInfos) {
                taskExecutorService.submit(() -> videoLayoutManager.addRecordingUserInfo(waitUserId.getUserId(),
                        waitUserId.getVideoWidth(),
                        waitUserId.getVideoHeight()));
            }
            waitForUpdateUiUserInfos.clear();
        }

    }

    public void stopRecording() {
        log.info("[" + taskId + "]stopRecording");
        if (null == agoraMediaRtcRecorder) {
            log.info("stopRecording agoraMediaRtcRecorder is null");
            return;
        }

        if (recorderConfig.isSubAllAudio()) {
            agoraMediaRtcRecorder.unsubscribeAllAudio();
        } else {
            for (String userId : recorderConfig.getSubAudioUserList()) {
                agoraMediaRtcRecorder.unsubscribeAudio(userId);
            }
        }
        if (recorderConfig.isSubAllVideo()) {
            agoraMediaRtcRecorder.unsubscribeAllVideo();
        } else {
            for (String userId : singleRecordingUserList) {
                agoraMediaRtcRecorder.unsubscribeVideo(userId);
            }
        }
        if (recorderState == io.agora.recording.Constants.RecorderState.RECORDER_STATE_START) {
            if (recorderConfig.isMix()) {
                agoraMediaRtcRecorder.stopRecording();
            } else {
                for (String userId : singleRecordingUserList) {
                    stopRecordingByUserId(userId);
                }
            }
        }

        int ret = agoraMediaRtcRecorder.leaveChannel();
        log.info("[" + taskId + "]stopRecording leaveChannel ret:" + ret);

        try {
            Thread.sleep(1 * 1000);
        } catch (InterruptedException e) {
            log.error("stopRecording Thread.sleep failed");
        }
        agoraMediaRtcRecorder.unregisterRecorderEventHandle(this);

        agoraMediaRtcRecorder.release();
        agoraMediaRtcRecorder = null;

        if (null != videoLayoutManager) {
            videoLayoutManager.release();
            videoLayoutManager = null;
        }

        startRecordingDone.set(false);
        waitForUpdateUiUserInfos.clear();
    }

    public void stopRecordingByUserId(String userId) {
        log.info("[" + taskId + "]stopRecordingByUserId userId:" + userId);
        if (null == agoraMediaRtcRecorder) {
            log.info("stopRecordingByUserId agoraMediaRtcRecorder is null");
            return;
        }

        if (singleRecordingUserList.contains(userId)) {
            agoraMediaRtcRecorder.stopSingleRecordingByUid(userId);
            singleRecordingUserList.remove(userId);
        }
    }

    @Override
    public void onConnected(String channelId, String userId) {
        log.info("[" + taskId + "]onConnected channelId:" + channelId + " userId:" + userId);
        taskExecutorService.submit(() -> {
            if (recorderConfig.isMix()) {
                startRecording("", 0, 0);
            }
        });

    }

    @Override
    public void onDisconnected(String channelId, String userId, Constants.ConnectionChangedReasonType reason) {
        log.info(
                "[" + taskId + "]onDisconnected channelId:" + channelId + " userId:" + userId + " reason:" + reason);
    }

    @Override
    public void onReconnected(String channelId, String userId, Constants.ConnectionChangedReasonType reason) {
        log.info(
                "[" + taskId + "]onReconnected channelId:" + channelId + " userId:" + userId + " reason:" + reason);
    }

    @Override
    public void onConnectionLost(String channelId, String userId) {
        log.info("[" + taskId + "]onConnectionLost channelId:" + channelId + " userId:" + userId);
        taskExecutorService.submit(() -> stopRecording());
    }

    @Override
    public void onUserJoined(String channelId, String userId) {
        log.info("[" + taskId + "]onUserJoined channelId:" + channelId + " userId:" + userId);
        if (!recorderConfig.isSubAllVideo() && !recorderConfig.getSubVideoUserList().contains(userId)) {
            return;
        }

    }

    @Override
    public void onUserLeft(String channelId, String userId, Constants.UserOfflineReasonType reason) {
        log.info("[" + taskId + "]onUserLeft channelId:" + channelId + " userId:" + userId + " reason:" + reason);
        if (!recorderConfig.isMix()) {
            if (!singleRecordingUserList.isEmpty() && singleRecordingUserList.contains(userId)) {
                taskExecutorService.submit(() -> stopRecordingByUserId(userId));
            }
        } else {
            if (!recorderConfig.isSubAllVideo() && !recorderConfig.getSubVideoUserList().contains(userId)) {
                return;
            }
            if (Utils.recorderIsVideo(Utils.convertToMediaRecorderStreamType(recorderConfig.getRecorderStreamType()))
                    && null != videoLayoutManager) {
                taskExecutorService.submit(() -> videoLayoutManager.removeRecordingUserInfo(userId));
            }
        }
    }

    @Override
    public void onFirstRemoteVideoDecoded(String channelId, String userId, int width, int height,
            int elapsed) {
        log.info("[" + taskId + "]onFirstRemoteVideoDecoded channelId:" + channelId + " userId:" + userId
                + " width:" + width
                + " height:" + height + " elapsed:" + elapsed);
        if (!recorderConfig.isMix() && Utils
                .recorderIsVideo(Utils.convertToMediaRecorderStreamType(recorderConfig.getRecorderStreamType()))) {
            if (recorderConfig.isSubAllVideo()
                    || (!recorderConfig.isSubAllVideo() && recorderConfig.getSubVideoUserList().contains(userId))) {
                taskExecutorService.submit(() -> startRecording(userId, width, height));
            }
        }
        if (recorderConfig.isMix()) {
            if (Utils.recorderIsVideo(Utils.convertToMediaRecorderStreamType(recorderConfig.getRecorderStreamType()))
                    && null != videoLayoutManager) {
                if (startRecordingDone.get()) {
                    taskExecutorService.submit(() -> videoLayoutManager.addRecordingUserInfo(userId, width, height));
                } else {
                    RecordingUserInfo recordingUserInfo = new RecordingUserInfo();
                    recordingUserInfo.setUserId(userId);
                    recordingUserInfo.setVideoWidth(width);
                    recordingUserInfo.setVideoHeight(height);
                    waitForUpdateUiUserInfos.add(recordingUserInfo);
                }
            }
        }
    }

    @Override
    public void onFirstRemoteAudioDecoded(String channelId, String userId, int elapsed) {
        log.info(
                "[" + taskId + "]onFirstRemoteAudioDecoded channelId:" + channelId + " userId:" + userId + " elapsed:"
                        + elapsed);
        if (!recorderConfig.isMix() && Utils.convertToMediaRecorderStreamType(
                recorderConfig.getRecorderStreamType()) == Constants.MediaRecorderStreamType.STREAM_TYPE_AUDIO) {
            if (recorderConfig.isSubAllAudio()
                    || (!recorderConfig.isSubAllAudio() && recorderConfig.getSubAudioUserList().contains(userId))) {
                taskExecutorService.submit(() -> startRecording(userId, 0, 0));
            }
        }
    }

    @Override
    public void onAudioVolumeIndication(String channelId, SpeakVolumeInfo[] speakers, int speakerNumber) {
        log.info("[" + taskId + "]onAudioVolumeIndication channelId:" + channelId + " speakerNumber:"
                + speakerNumber);
        if (speakers != null && speakers.length > 0) {
            for (SpeakVolumeInfo speaker : speakers) {
                log.info("[" + taskId + "]onAudioVolumeIndication speaker:" + speaker.getUserId()
                        + " volume:" + speaker.getVolume());
            }
        }
    }

    @Override
    public void onActiveSpeaker(String channelId, String userId) {
        log.info("[" + taskId + "]onActiveSpeaker channelId:" + channelId + " userId:" + userId);
    }

    @Override
    public void onUserVideoStateChanged(String channelId, String userId, Constants.RemoteVideoState state,
            Constants.RemoteVideoStateReason reason, int elapsed) {
        log.info("[" + taskId + "]onUserVideoStateChanged channelId:" + channelId + " userId:" + userId
                + " state:" + state
                + " reason:" + reason + " elapsed:" + elapsed);
    }

    @Override
    public void onUserAudioStateChanged(String channelId, String userId, Constants.RemoteAudioState state,
            Constants.RemoteAudioStateReason reason, int elapsed) {
        log.info("[" + taskId + "]onUserAudioStateChanged channelId:" + channelId + " userId:" + userId
                + " state:" + state
                + " reason:" + reason + " elapsed:" + elapsed);
    }

    @Override
    public void onRemoteVideoStats(String channelId, String userId, RemoteVideoStatistics stats) {
        log.info("onRemoteVideoStats channelId:" + channelId + " userId:" + userId + " stats:" + stats);
    }

    @Override
    public void onRemoteAudioStats(String channelId, String userId, RemoteAudioStatistics stats) {
        log.info("onRemoteAudioStats channelId:" + channelId + " userId:" + userId + " stats:" + stats);
    }

    @Override
    public void onRecorderStateChanged(String channelId, String userId, Constants.RecorderState state,
            Constants.RecorderReasonCode reason, String fileName) {
        log.info("[" + taskId + "]onRecorderStateChanged channelId:" + channelId + " userId:" + userId
                + " state:" + state
                + " reason:" + reason + " fileName:" + fileName);
        recorderState = state;
    }

    @Override
    public void onRecorderInfoUpdated(String channelId, String userId, RecorderInfo info) {
        log.info(
                "[" + taskId + "]onRecorderInfoUpdated channelId:" + channelId + " userId:" + userId + " info:" + info);
    }

    @Override
    public void onEncryptionError(String channelId, Constants.EncryptionErrorType errorType) {
        log.info("[" + taskId + "]onEncryptionError channelId:" + channelId + " errorType:" + errorType);
    }
}
