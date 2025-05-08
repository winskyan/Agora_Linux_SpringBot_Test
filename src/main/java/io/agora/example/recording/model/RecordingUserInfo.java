package io.agora.example.recording.model;

public class RecordingUserInfo {
    private String userId;
    private int videoHeight;
    private int videoWidth;

    public String getUserId() {
        return userId;
    }

    public int getVideoHeight() {
        return videoHeight;
    }

    public int getVideoWidth() {
        return videoWidth;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setVideoHeight(int videoHeight) {
        this.videoHeight = videoHeight;
    }

    public void setVideoWidth(int videoWidth) {
        this.videoWidth = videoWidth;
    }

    @Override
    public String toString() {
        return "RecordingUserInfo{" +
                "userId='" + userId + '\'' +
                ", videoHeight=" + videoHeight +
                ", videoWidth=" + videoWidth +
                '}';
    }
}