package io.agora.example.recording.utils;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.agora.recording.Constants;
import io.agora.recording.Constants.MediaRecorderStreamType;
import io.agora.recording.Constants.VideoStreamType;
import io.agora.recording.Constants.WatermarkSourceType;

public class Utils {
    private static final DateTimeFormatter DATE_TIME_FORMATTER;
    private static final ZoneId ZONE_ID;

    static {
        ZONE_ID = ZoneId.systemDefault();
        DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
                .withZone(ZONE_ID);
    }

    public static String getCurrentTime() {
        return DATE_TIME_FORMATTER.format(Instant.now());
    }

    public static String getTaskId() {
        String currentTime = getCurrentTime().replace(" ", "").replace("-", "").replace(":", "").replace(".", "");
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return currentTime + "-" + uuid;
    }

    public static String[] readAppIdAndToken(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return new String[] { null, null };
        }
        String appId = null;
        String token = null;
        try {
            List<String> lines = Files.readAllLines(Paths.get(filePath));

            Pattern appIdPattern = Pattern.compile("APP_ID=(.*)");
            Pattern tokenPattern = Pattern.compile("TOKEN=(.*)");

            for (String line : lines) {
                Matcher appIdMatcher = appIdPattern.matcher(line);
                Matcher tokenMatcher = tokenPattern.matcher(line);

                if (appIdMatcher.find()) {
                    appId = appIdMatcher.group(1);
                }
                if (tokenMatcher.find()) {
                    token = tokenMatcher.group(1);
                }

                if (appId != null && token != null) {
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new String[] { appId, token };
    }

    public static VideoStreamType convertToVideoStreamType(String type) {
        if (type == null) {
            return VideoStreamType.VIDEO_STREAM_HIGH;
        }

        switch (type) {
            case "high":
                return VideoStreamType.VIDEO_STREAM_HIGH;
            case "low":
                return VideoStreamType.VIDEO_STREAM_LOW;
            default:
                return VideoStreamType.VIDEO_STREAM_HIGH;
        }
    }

    public static MediaRecorderStreamType convertToMediaRecorderStreamType(String type) {
        if (type == null) {
            return MediaRecorderStreamType.STREAM_TYPE_BOTH;
        }

        switch (type) {
            case "audio_only":
                return MediaRecorderStreamType.STREAM_TYPE_AUDIO;
            case "video_only":
                return MediaRecorderStreamType.STREAM_TYPE_VIDEO;
            case "both":
                return MediaRecorderStreamType.STREAM_TYPE_BOTH;
            default:
                return MediaRecorderStreamType.STREAM_TYPE_BOTH;
        }
    }

    public static boolean recorderIsVideo(MediaRecorderStreamType type) {
        return type == MediaRecorderStreamType.STREAM_TYPE_VIDEO
                || type == MediaRecorderStreamType.STREAM_TYPE_BOTH;
    }

    public static boolean recorderIsAudio(MediaRecorderStreamType type) {
        return type == MediaRecorderStreamType.STREAM_TYPE_AUDIO
                || type == MediaRecorderStreamType.STREAM_TYPE_BOTH;
    }

    public static Constants.EncryptionMode convertToEncryptionMode(String mode) {
        if (mode == null) {
            return Constants.EncryptionMode.AES_128_GCM2;
        }

        switch (mode) {
            case "AES_128_XTS":
                return Constants.EncryptionMode.AES_128_XTS;
            case "AES_128_ECB":
                return Constants.EncryptionMode.AES_128_ECB;
            case "AES_256_XTS":
                return Constants.EncryptionMode.AES_256_XTS;
            case "SM4_128_ECB":
                return Constants.EncryptionMode.SM4_128_ECB;
            case "AES_128_GCM":
                return Constants.EncryptionMode.AES_128_GCM;
            case "AES_256_GCM":
                return Constants.EncryptionMode.AES_256_GCM;
            case "AES_128_GCM2":
                return Constants.EncryptionMode.AES_128_GCM2;
            case "AES_256_GCM2":
                return Constants.EncryptionMode.AES_256_GCM2;
            default:
                return Constants.EncryptionMode.AES_128_GCM2;
        }
    }

    public static WatermarkSourceType convertToWatermarkSourceType(String type) {
        if (type == null) {
            return WatermarkSourceType.PICTURE;
        }

        switch (type) {
            case "litera":
                return WatermarkSourceType.LITERA;
            case "time":
                return WatermarkSourceType.TIMESTAMPS;
            case "picture":
                return WatermarkSourceType.PICTURE;
            default:
                return WatermarkSourceType.PICTURE;
        }
    }
}
