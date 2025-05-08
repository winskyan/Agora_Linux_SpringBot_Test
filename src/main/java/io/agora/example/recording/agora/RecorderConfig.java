package io.agora.example.recording.agora;

import java.util.ArrayList;
import java.util.List;

public class RecorderConfig {
    private String appId;
    private String token;
    private String channelName;
    private boolean useStringUid;
    private boolean useCloudProxy;
    private String userId;
    private boolean subAllAudio;
    private List<String> subAudioUserList;
    private boolean subAllVideo;
    private List<String> subVideoUserList;
    private String subStreamType;
    private boolean isMix;
    private long backgroundColor;
    private String backgroundImage;
    private String layoutMode;
    private String maxResolutionUid;
    private String recorderStreamType;
    private String recorderPath;
    private int maxDuration;
    private boolean recoverFile;
    private AudioConfig audio;
    private VideoConfig video;
    private List<WaterMark> waterMark;
    private Encryption encryption;
    private List<Rotation> rotation;

    public RecorderConfig() {
        audio = new AudioConfig();
        video = new VideoConfig();
        subAudioUserList = new ArrayList<>();
        subVideoUserList = new ArrayList<>();
        waterMark = new ArrayList<>();
        encryption = new Encryption();
        rotation = new ArrayList<>();
        maxDuration = 120;
        recoverFile = false;
    }

    public static class AudioConfig {
        private int sampleRate;
        private int numOfChannels;

        public int getSampleRate() {
            return sampleRate;
        }

        public void setSampleRate(int sampleRate) {
            this.sampleRate = sampleRate;
        }

        public int getNumOfChannels() {
            return numOfChannels;
        }

        public void setNumOfChannels(int numOfChannels) {
            this.numOfChannels = numOfChannels;
        }

        @Override
        public String toString() {
            return "AudioConfig{" +
                    "sampleRate=" + sampleRate +
                    ", numOfChannels=" + numOfChannels +
                    '}';
        }
    }

    public static class VideoConfig {
        private int width;
        private int height;
        private int fps;

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public int getFps() {
            return fps;
        }

        public void setFps(int fps) {
            this.fps = fps;
        }

        @Override
        public String toString() {
            return "VideoConfig{" +
                    "width=" + width +
                    ", height=" + height +
                    ", fps=" + fps +
                    '}';
        }
    }

    public static class WaterMark {
        private String type;
        private String litera;
        private String fontFilePath;
        private int fontSize;
        private int x;
        private int y;
        private int width;
        private int height;
        private int zorder;
        private String imgUrl;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getLitera() {
            return litera;
        }

        public void setLitera(String litera) {
            this.litera = litera;
        }

        public String getFontFilePath() {
            return fontFilePath;
        }

        public void setFontFilePath(String fontFilePath) {
            this.fontFilePath = fontFilePath;
        }

        public int getFontSize() {
            return fontSize;
        }

        public void setFontSize(int fontSize) {
            this.fontSize = fontSize;
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public int getZorder() {
            return zorder;
        }

        public void setZorder(int zorder) {
            this.zorder = zorder;
        }

        public String getImgUrl() {
            return imgUrl;
        }

        public void setImgUrl(String imgUrl) {
            this.imgUrl = imgUrl;
        }

        @Override
        public String toString() {
            return "WaterMark{" +
                    "type='" + type + '\'' +
                    ", litera='" + litera + '\'' +
                    ", fontFilePath='" + fontFilePath + '\'' +
                    ", fontSize=" + fontSize +
                    ", x=" + x +
                    ", y=" + y +
                    ", width=" + width +
                    ", height=" + height +
                    ", zorder=" + zorder +
                    ", imgUrl='" + imgUrl + '\'' +
                    '}';
        }
    }

    public static class Encryption {
        private String mode;
        private String key;
        private String salt;

        // Getters and Setters
        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getSalt() {
            return salt;
        }

        public void setSalt(String salt) {
            this.salt = salt;
        }

        @Override
        public String toString() {
            return "Encryption{" +
                    "mode='" + mode + '\'' +
                    ", key='" + key + '\'' +
                    ", salt='" + salt + '\'' +
                    '}';
        }
    }

    public static class Rotation {
        private String uid;
        private int degree;

        // Getters and Setters
        public String getUid() {
            return uid;
        }

        public void setUid(String uid) {
            this.uid = uid;
        }

        public int getDegree() {
            return degree;
        }

        public void setDegree(int degree) {
            this.degree = degree;
        }

        @Override
        public String toString() {
            return "Rotation{" +
                    "uid=" + uid +
                    ", degree=" + degree +
                    '}';
        }
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public boolean isUseStringUid() {
        return useStringUid;
    }

    public void setUseStringUid(boolean useStringUid) {
        this.useStringUid = useStringUid;
    }

    public boolean isUseCloudProxy() {
        return useCloudProxy;
    }

    public void setUseCloudProxy(boolean useCloudProxy) {
        this.useCloudProxy = useCloudProxy;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public boolean isSubAllAudio() {
        return subAllAudio;
    }

    public void setSubAllAudio(boolean subAllAudio) {
        this.subAllAudio = subAllAudio;
    }

    public List<String> getSubAudioUserList() {
        return subAudioUserList;
    }

    public void setSubAudioUserList(List<String> subAudioUserList) {
        this.subAudioUserList = subAudioUserList;
    }

    public boolean isSubAllVideo() {
        return subAllVideo;
    }

    public void setSubAllVideo(boolean subAllVideo) {
        this.subAllVideo = subAllVideo;
    }

    public List<String> getSubVideoUserList() {
        return subVideoUserList;
    }

    public void setSubVideoUserList(List<String> subVideoUserList) {
        this.subVideoUserList = subVideoUserList;
    }

    public String getSubStreamType() {
        return subStreamType;
    }

    public void setSubStreamType(String subStreamType) {
        this.subStreamType = subStreamType;
    }

    public boolean isMix() {
        return isMix;
    }

    public void setIsMix(boolean isMix) {
        this.isMix = isMix;
    }

    public long getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(long backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public String getBackgroundImage() {
        return backgroundImage;
    }

    public void setBackgroundImage(String backgroundImage) {
        this.backgroundImage = backgroundImage;
    }

    public String getLayoutMode() {
        return layoutMode;
    }

    public void setLayoutMode(String layoutMode) {
        this.layoutMode = layoutMode;
    }

    public String getMaxResolutionUid() {
        return maxResolutionUid;
    }

    public void setMaxResolutionUid(String maxResolutionUid) {
        this.maxResolutionUid = maxResolutionUid;
    }

    public String getRecorderStreamType() {
        return recorderStreamType;
    }

    public void setRecorderStreamType(String recorderStreamType) {
        this.recorderStreamType = recorderStreamType;
    }

    public String getRecorderPath() {
        return recorderPath;
    }

    public void setRecorderPath(String recorderPath) {
        this.recorderPath = recorderPath;
    }

    public int getMaxDuration() {
        return maxDuration;
    }

    public void setMaxDuration(int maxDuration) {
        this.maxDuration = maxDuration;
    }

    public boolean isRecoverFile() {
        return recoverFile;
    }

    public void setRecoverFile(boolean recoverFile) {
        this.recoverFile = recoverFile;
    }

    public AudioConfig getAudio() {
        return audio;
    }

    public void setAudio(AudioConfig audio) {
        this.audio = audio;
    }

    public VideoConfig getVideo() {
        return video;
    }

    public void setVideo(VideoConfig video) {
        this.video = video;
    }

    public List<WaterMark> getWaterMark() {
        return waterMark;
    }

    public void setWaterMark(List<WaterMark> waterMark) {
        this.waterMark = waterMark;
    }

    public Encryption getEncryption() {
        return encryption;
    }

    public void setEncryption(Encryption encryption) {
        this.encryption = encryption;
    }

    public List<Rotation> getRotation() {
        return rotation;
    }

    public void setRotation(List<Rotation> rotation) {
        this.rotation = rotation;
    }

    @Override
    public String toString() {
        return "RecorderConfig{" +
                "appId='" + appId + '\'' +
                ", token='" + token + '\'' +
                ", channelName='" + channelName + '\'' +
                ", useStringUid=" + useStringUid +
                ", useCloudProxy=" + useCloudProxy +
                ", userId='" + userId + '\'' +
                ", subAllAudio=" + subAllAudio +
                ", subAudioUserList=" + subAudioUserList +
                ", subAllVideo=" + subAllVideo +
                ", subVideoUserList=" + subVideoUserList +
                ", subStreamType='" + subStreamType + '\'' +
                ", isMix=" + isMix +
                ", backgroundColor=" + backgroundColor +
                ", backgroundImage='" + backgroundImage + '\'' +
                ", layoutMode='" + layoutMode + '\'' +
                ", maxResolutionUid='" + maxResolutionUid + '\'' +
                ", recorderStreamType='" + recorderStreamType + '\'' +
                ", recorderPath='" + recorderPath + '\'' +
                ", maxDuration=" + maxDuration +
                ", recoverFile=" + recoverFile +
                ", audio=" + audio +
                ", video=" + video +
                ", waterMark=" + waterMark +
                ", encryption=" + encryption +
                ", rotation=" + rotation +
                '}';
    }
}
