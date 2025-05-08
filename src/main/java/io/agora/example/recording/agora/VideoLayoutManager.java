package io.agora.example.recording.agora;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import io.agora.recording.AgoraMediaRtcRecorder;
import io.agora.recording.MixerLayoutConfig;
import io.agora.recording.UserMixerLayout;
import io.agora.recording.VideoMixingLayout;
import io.agora.example.recording.model.RecordingUserInfo;
import io.agora.example.recording.model.LayoutCoordinates;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VideoLayoutManager {
    private final AgoraMediaRtcRecorder recorder;
    private final RecorderConfig recorderConfig;
    private final List<RecordingUserInfo> recordingUserInfos;

    public VideoLayoutManager(AgoraMediaRtcRecorder recorder, RecorderConfig recorderConfig) {
        this.recorder = recorder;
        this.recorderConfig = recorderConfig;
        this.recordingUserInfos = new CopyOnWriteArrayList<>();
    }

    public void release() {
        recordingUserInfos.clear();
    }

    public synchronized void addRecordingUserInfo(String userId, int videoWidth, int videoHeight) {
        log.info("VideoLayoutManager addRecordingUserInfo userId: " + userId + " videoWidth: " + videoWidth
                + " videoHeight: " + videoHeight);
        for (RecordingUserInfo recordingUserInfo : recordingUserInfos) {
            if (recordingUserInfo.getUserId().equals(userId)) {
                return;
            }
        }
        RecordingUserInfo recordingUserInfo = new RecordingUserInfo();
        recordingUserInfo.setUserId(userId);
        recordingUserInfo.setVideoHeight(videoHeight);
        recordingUserInfo.setVideoWidth(videoWidth);
        recordingUserInfos.add(recordingUserInfo);
        updateVideoMixLayout();
    }

    public synchronized void removeRecordingUserInfo(String userId) {
        log.info("removeRecordingUserInfo userId: " + userId);
        for (RecordingUserInfo recordingUserInfo : recordingUserInfos) {
            if (recordingUserInfo.getUserId().equals(userId)) {
                recordingUserInfos.remove(recordingUserInfo);
                break;
            }
        }
        updateVideoMixLayout();
    }

    private void updateVideoMixLayout() {
        if (!recordingUserInfos.isEmpty() && recordingUserInfos.size() <= 17) {
            VideoMixingLayout layout = new VideoMixingLayout();
            layout.setCanvasWidth(recorderConfig.getVideo().getWidth());
            layout.setCanvasHeight(recorderConfig.getVideo().getHeight());
            layout.setCanvasFps(recorderConfig.getVideo().getFps());
            layout.setBackgroundColor(recorderConfig.getBackgroundColor());
            layout.setBackgroundImage(recorderConfig.getBackgroundImage());

            UserMixerLayout[] useLayout = new UserMixerLayout[recordingUserInfos
                    .size()];
            for (int i = 0; i < useLayout.length; i++) {
                useLayout[i] = new UserMixerLayout();
            }

            switch (recorderConfig.getLayoutMode()) {
                case ExampleConstants.BESTFIT_LAYOUT:
                    adjustBestFitVideoLayout(useLayout);
                    break;
                case ExampleConstants.DEFAULT_LAYOUT:
                    adjustDefaultVideoLayout(useLayout);
                    break;
                case ExampleConstants.VERTICAL_LAYOUT:
                    adjustVerticalLayout(useLayout, recorderConfig.getMaxResolutionUid());
                    break;
            }
            for (int i = 0; i < recordingUserInfos.size(); i++) {
                String userId = useLayout[i].getUserId();
                for (RecorderConfig.Rotation rotation : recorderConfig.getRotation()) {
                    if (rotation.getUid().equals(userId)) {
                        useLayout[i].getConfig().setRotation(rotation.getDegree());
                    }
                }
            }

            layout.setUserLayoutConfigs(useLayout);
            int ret = recorder.setVideoMixingLayout(layout);
            log.info("updateVideoMixLayout layout:" + layout + " ret: " + ret);
        }
    }

    /**
     * Calculates the layout coordinates for a video within a target region,
     * preserving aspect ratio and centering.
     *
     * @param videoWidth   The original width of the video.
     * @param videoHeight  The original height of the video.
     * @param targetX      The X coordinate of the target region's top-left corner.
     * @param targetY      The Y coordinate of the target region's top-left corner.
     * @param targetWidth  The width of the target region.
     * @param targetHeight The height of the target region.
     * @return LayoutCoordinates containing the calculated centered position and
     *         size.
     */
    private LayoutCoordinates calculateLayoutInRegion(int videoWidth, int videoHeight, int targetX, int targetY,
            int targetWidth, int targetHeight) {
        LayoutCoordinates coords = new LayoutCoordinates(targetX, targetY, 0, 0); // Default to zero size at target
                                                                                  // origin

        if (targetWidth <= 0 || targetHeight <= 0 || videoWidth <= 0 || videoHeight <= 0) {
            log.warn("calculateLayoutInRegion: Invalid dimensions (video: " + videoWidth + "x" + videoHeight
                    + ", target: " + targetWidth + "x" + targetHeight + "). Setting zero size.");
            return coords; // Return zero size
        }

        int scaledWidth;
        int scaledHeight;

        // Compare aspect ratios using cross-multiplication to avoid division by zero
        // and precision issues
        // Use long to prevent potential overflow during multiplication
        if ((long) targetWidth * videoHeight > (long) videoWidth * targetHeight) {
            // Target area is relatively wider than the video (letterbox)
            // Fit to height, calculate width proportionally
            scaledHeight = targetHeight;
            // Use double for intermediate calculation for better precision before casting
            // to int
            scaledWidth = (int) Math.round((double) videoWidth * targetHeight / videoHeight);
        } else {
            // Target area is relatively narrower than or same aspect ratio as the video
            // (pillarbox)
            // Fit to width, calculate height proportionally
            scaledWidth = targetWidth;
            // Use double for intermediate calculation for better precision before casting
            // to int
            scaledHeight = (int) Math.round((double) videoHeight * targetWidth / videoWidth);
        }

        // Center the scaled video within the target area
        coords.setWidth(scaledWidth);
        coords.setHeight(scaledHeight);
        coords.setX(targetX + (targetWidth - scaledWidth) / 2);
        coords.setY(targetY + (targetHeight - scaledHeight) / 2);

        return coords;
    }

    private void adjustBestFitVideoLayout(UserMixerLayout[] regionList) {
        if (regionList.length == 0) {
            return;
        }

        if (recordingUserInfos.size() == 1) {
            adjustBestFitLayout1(regionList, 1);
        } else if (recordingUserInfos.size() == 2) {
            adjustBestFitLayout2(regionList);
        } else if (recordingUserInfos.size() <= 4) {
            adjustBestFitLayouSquare(regionList, 2);
        } else if (recordingUserInfos.size() <= 9) {
            adjustBestFitLayouSquare(regionList, 3);
        } else if (recordingUserInfos.size() <= 16) {
            adjustBestFitLayouSquare(regionList, 4);
        } else if (recordingUserInfos.size() == 17) {
            adjustBestFitLayout17(regionList);
        } else {
            log.error("adjustBestFitVideoLayout is more than 17 users");
        }
    }

    private void adjustBestFitLayout1(UserMixerLayout[] regionList, int square) {
        if (regionList.length == 0) {
            return;
        }
        MixerLayoutConfig config = new MixerLayoutConfig();
        int canvasWidth = recorderConfig.getVideo().getWidth();
        int canvasHeight = recorderConfig.getVideo().getHeight();

        // Default to zero size layout
        int layoutX = 0;
        int layoutY = 0;
        int layoutWidth = 0;
        int layoutHeight = 0;

        if (recordingUserInfos != null && !recordingUserInfos.isEmpty()) {
            RecordingUserInfo firstUser = recordingUserInfos.get(0);
            int videoWidth = firstUser.getVideoWidth();
            int videoHeight = firstUser.getVideoHeight();

            // Only calculate layout if all dimensions are positive
            if (canvasWidth > 0 && canvasHeight > 0 && videoWidth > 0 && videoHeight > 0) {
                // Compare aspect ratios using cross-multiplication to avoid division
                // Use long to prevent potential overflow during multiplication
                if ((long) canvasWidth * videoHeight > (long) videoWidth * canvasHeight) {
                    // Canvas is relatively wider than the video (letterbox)
                    // Fit to height, calculate width proportionally, center horizontally
                    layoutHeight = canvasHeight;
                    // Use double for intermediate calculation for better precision before casting
                    // to int
                    layoutWidth = (int) Math.round((double) videoWidth * canvasHeight / videoHeight);
                    layoutX = (canvasWidth - layoutWidth) / 2;
                    layoutY = 0;
                } else {
                    // Canvas is relatively narrower than or same aspect ratio as the video
                    // (pillarbox)
                    // Fit to width, calculate height proportionally, center vertically
                    layoutWidth = canvasWidth;
                    // Use double for intermediate calculation for better precision before casting
                    // to int
                    layoutHeight = (int) Math.round((double) videoHeight * canvasWidth / videoWidth);
                    layoutX = 0;
                    layoutY = (canvasHeight - layoutHeight) / 2;
                }
            }
        }
        // else: Keep the default zero size layout if no users or invalid dimensions

        config.setX(layoutX);
        config.setY(layoutY);
        config.setWidth(layoutWidth);
        config.setHeight(layoutHeight);

        regionList[0].setConfig(config);
        regionList[0].setUserId(recordingUserInfos.get(0).getUserId());
    }

    private void adjustBestFitLayout2(UserMixerLayout[] regionList) {
        if (regionList == null || regionList.length < 2 || recordingUserInfos == null
                || recordingUserInfos.size() < 2) {
            log.warn("adjustBestFitLayout2: Invalid input for layout calculation.");
            return;
        }

        int canvasWidth = recorderConfig.getVideo().getWidth();
        int canvasHeight = recorderConfig.getVideo().getHeight();

        // Calculate the target area width for each user (half the canvas)
        int targetWidth = canvasWidth / 2;
        int targetHeight = canvasHeight;

        for (int i = 0; i < 2; i++) {
            RecordingUserInfo recordingUserInfo = recordingUserInfos.get(i);
            regionList[i].setUserId(recordingUserInfo.getUserId());
            MixerLayoutConfig config = new MixerLayoutConfig();

            int videoWidth = recordingUserInfo.getVideoWidth();
            int videoHeight = recordingUserInfo.getVideoHeight();

            // Calculate the target X position for this user's area
            int targetX = i * targetWidth; // 0 for first, targetWidth for second
            int targetY = 0;

            // Default to zero size layout
            int layoutX = targetX;
            int layoutY = targetY;
            int layoutWidth = 0;
            int layoutHeight = 0;

            // Only calculate layout if all dimensions are positive
            if (targetWidth > 0 && targetHeight > 0 && videoWidth > 0 && videoHeight > 0) {
                int scaledWidth = 0;
                int scaledHeight = 0;

                // Compare aspect ratios using cross-multiplication
                if ((long) targetWidth * videoHeight > (long) videoWidth * targetHeight) {
                    // Target area is relatively wider (letterbox for video)
                    scaledHeight = targetHeight;
                    scaledWidth = (int) Math.round((double) videoWidth * targetHeight / videoHeight);
                } else {
                    // Target area is relatively narrower or same aspect ratio (pillarbox for video)
                    scaledWidth = targetWidth;
                    scaledHeight = (int) Math.round((double) videoHeight * targetWidth / videoWidth);
                }

                // Center the scaled video within the target area
                layoutWidth = scaledWidth;
                layoutHeight = scaledHeight;
                layoutX = targetX + (targetWidth - scaledWidth) / 2;
                layoutY = targetY + (targetHeight - scaledHeight) / 2;
            }
            // else: Keep the default zero size layout if dimensions are invalid

            config.setX(layoutX);
            config.setY(layoutY);
            config.setWidth(layoutWidth);
            config.setHeight(layoutHeight);
            regionList[i].setConfig(config);
        }
    }

    private void adjustBestFitLayouSquare(UserMixerLayout[] regionList, int square) {
        // Input validation
        if (regionList == null || regionList.length == 0 || recordingUserInfos == null || recordingUserInfos.isEmpty()
                || square <= 0) {
            log.warn("adjustBestFitLayouSquare: Invalid input (regionList, recordingUserInfos, or square <= 0).");
            return;
        }

        int canvasWidth = recorderConfig.getVideo().getWidth();
        int canvasHeight = recorderConfig.getVideo().getHeight();

        // Check for invalid canvas dimensions
        if (canvasWidth <= 0 || canvasHeight <= 0) {
            log.warn(
                    "adjustBestFitLayouSquare: Invalid canvas dimensions (" + canvasWidth + "x" + canvasHeight + ").");
            // Set all regions to zero size as a fallback
            for (int i = 0; i < regionList.length; ++i) {
                MixerLayoutConfig config = new MixerLayoutConfig();
                config.setWidth(0);
                config.setHeight(0);
                config.setX(0);
                config.setY(0);
                if (i < recordingUserInfos.size()) {
                    regionList[i].setUserId(recordingUserInfos.get(i).getUserId());
                } else {
                    regionList[i].setUserId(""); // Or null
                }
                regionList[i].setConfig(config);
            }
            return;
        }

        // Calculate dimensions for each grid cell
        int targetWidth = canvasWidth / square;
        int targetHeight = canvasHeight / square;

        int count = Math.min(regionList.length, recordingUserInfos.size());

        for (int i = 0; i < count; i++) {
            RecordingUserInfo recordingUserInfo = recordingUserInfos.get(i);
            regionList[i].setUserId(recordingUserInfo.getUserId());
            MixerLayoutConfig config = new MixerLayoutConfig();

            int videoWidth = recordingUserInfo.getVideoWidth();
            int videoHeight = recordingUserInfo.getVideoHeight();

            // Calculate target position for the top-left corner of the cell
            int targetX = (i % square) * targetWidth;
            int targetY = (i / square) * targetHeight;

            // Default to zero size layout within the cell
            int layoutX = targetX;
            int layoutY = targetY;
            int layoutWidth = 0;
            int layoutHeight = 0;

            // Only calculate layout if all dimensions are positive
            if (targetWidth > 0 && targetHeight > 0 && videoWidth > 0 && videoHeight > 0) {
                int scaledWidth = 0;
                int scaledHeight = 0;

                // Compare aspect ratios using cross-multiplication
                if ((long) targetWidth * videoHeight > (long) videoWidth * targetHeight) {
                    // Target cell is relatively wider (letterbox for video)
                    scaledHeight = targetHeight;
                    scaledWidth = (int) Math.round((double) videoWidth * targetHeight / videoHeight);
                } else {
                    // Target cell is relatively narrower or same aspect ratio (pillarbox for video)
                    scaledWidth = targetWidth;
                    scaledHeight = (int) Math.round((double) videoHeight * targetWidth / videoWidth);
                }

                // Center the scaled video within the target cell
                layoutWidth = scaledWidth;
                layoutHeight = scaledHeight;
                layoutX = targetX + (targetWidth - scaledWidth) / 2;
                layoutY = targetY + (targetHeight - scaledHeight) / 2;
            } else {
                log.warn("adjustBestFitLayouSquare: Invalid dimensions for user " + recordingUserInfo.getUserId() +
                        " (video: " + videoWidth + "x" + videoHeight + ", target: " + targetWidth + "x"
                        + targetHeight + "). Setting zero size.");
                // Keep layoutWidth/Height as 0 if dimensions are invalid
            }

            config.setX(layoutX);
            config.setY(layoutY);
            config.setWidth(layoutWidth);
            config.setHeight(layoutHeight);
            regionList[i].setConfig(config);
        }

        // Handle remaining regionList elements if regionList.length >
        // recordingUserInfos.size()
        // Set them to zero size
        for (int i = count; i < regionList.length; i++) {
            MixerLayoutConfig config = new MixerLayoutConfig();
            config.setWidth(0);
            config.setHeight(0);
            config.setX(0); // Explicitly set position too
            config.setY(0);
            regionList[i].setUserId(""); // Set empty user ID or null
            regionList[i].setConfig(config);
        }
    }

    private void adjustBestFitLayout17(UserMixerLayout[] regionList) {
        // Input validation
        if (regionList == null || regionList.length < 17 || recordingUserInfos == null
                || recordingUserInfos.size() < 17) {
            log.warn("adjustBestFitLayout17: Invalid input (requires at least 17 regions and users).");
            // Fallback or simply return if strict 17 is required
            // If fallback needed, maybe call adjustBestFitLayouSquare(regionList, 4) or
            // similar?
            return; // Assuming strict 17 users are required for this specific layout
        }

        int canvasWidth = recorderConfig.getVideo().getWidth();
        int canvasHeight = recorderConfig.getVideo().getHeight();

        // Check for invalid canvas dimensions
        if (canvasWidth <= 0 || canvasHeight <= 0) {
            log.warn(
                    "adjustBestFitLayout17: Invalid canvas dimensions (" + canvasWidth + "x" + canvasHeight + ").");
            // Set all regions to zero size as a fallback
            for (int i = 0; i < regionList.length; ++i) {
                MixerLayoutConfig config = new MixerLayoutConfig();
                config.setWidth(0);
                config.setHeight(0);
                config.setX(0);
                config.setY(0);
                if (i < recordingUserInfos.size()) {
                    regionList[i].setUserId(recordingUserInfos.get(i).getUserId());
                } else {
                    regionList[i].setUserId(""); // Or null
                }
                regionList[i].setConfig(config);
            }
            return;
        }

        int n = 5;
        // Calculate dimensions for each conceptual grid cell
        int targetWidth = canvasWidth / n;
        int targetHeight = canvasHeight / n;

        int count = Math.min(regionList.length, recordingUserInfos.size()); // Should be at least 17 due to initial
                                                                            // check
        count = Math.min(count, 17); // Ensure we don't process more than 17 for this specific layout

        for (int i = 0; i < count; i++) {
            RecordingUserInfo recordingUserInfo = recordingUserInfos.get(i);
            regionList[i].setUserId(recordingUserInfo.getUserId());
            MixerLayoutConfig config = new MixerLayoutConfig();

            int videoWidth = recordingUserInfo.getVideoWidth();
            int videoHeight = recordingUserInfo.getVideoHeight();

            // Calculate target top-left corner for the cell based on the 17-layout pattern
            int targetX = 0;
            int targetY = 0;
            if (i == 16) { // Special case for the 17th user
                targetX = (int) (0.4f * canvasWidth); // As per original calculation: (1 - 1/n) * 0.5 * canvasWidth
                int yIndex = 4; // As per original calculation: i / (n - 1) => 16 / 4 = 4
                targetY = (int) ((1.0f / n) * yIndex * canvasHeight); // 0.2 * 4 * canvasHeight = 0.8 * canvasHeight
            } else { // For the first 16 users (i=0 to 15)
                int xIndex = i % 4; // Column index within the 4x4 grid
                int yIndex = i / 4; // Row index within the 4x4 grid
                targetX = (int) ((0.5f / n + (1.0f / n) * xIndex) * canvasWidth); // (0.1 + 0.2 * xIndex) * canvasWidth
                targetY = (int) ((1.0f / n) * yIndex * canvasHeight); // 0.2 * yIndex * canvasHeight
            }

            // Default to zero size layout within the cell
            int layoutX = targetX;
            int layoutY = targetY;
            int layoutWidth = 0;
            int layoutHeight = 0;

            // Only calculate layout if all dimensions are positive
            if (targetWidth > 0 && targetHeight > 0 && videoWidth > 0 && videoHeight > 0) {
                int scaledWidth = 0;
                int scaledHeight = 0;

                // Compare aspect ratios using cross-multiplication
                if ((long) targetWidth * videoHeight > (long) videoWidth * targetHeight) {
                    // Target cell is relatively wider (letterbox for video)
                    scaledHeight = targetHeight;
                    scaledWidth = (int) Math.round((double) videoWidth * targetHeight / videoHeight);
                } else {
                    // Target cell is relatively narrower or same aspect ratio (pillarbox for video)
                    scaledWidth = targetWidth;
                    scaledHeight = (int) Math.round((double) videoHeight * targetWidth / videoWidth);
                }

                // Center the scaled video within the target cell
                layoutWidth = scaledWidth;
                layoutHeight = scaledHeight;
                layoutX = targetX + (targetWidth - scaledWidth) / 2;
                layoutY = targetY + (targetHeight - scaledHeight) / 2;
            } else {
                log.warn("adjustBestFitLayout17: Invalid dimensions for user " + recordingUserInfo.getUserId() +
                        " (video: " + videoWidth + "x" + videoHeight + ", target: " + targetWidth + "x"
                        + targetHeight + "). Setting zero size.");
                // Keep layoutWidth/Height as 0
            }

            config.setX(layoutX);
            config.setY(layoutY);
            config.setWidth(layoutWidth);
            config.setHeight(layoutHeight);
            regionList[i].setConfig(config);
        }

        // Handle remaining regionList elements if regionList.length > 17
        for (int i = count; i < regionList.length; i++) {
            MixerLayoutConfig config = new MixerLayoutConfig();
            config.setWidth(0);
            config.setHeight(0);
            config.setX(0);
            config.setY(0);
            regionList[i].setUserId(""); // Set empty user ID or null
            regionList[i].setConfig(config);
        }
    }

    private void adjustDefaultVideoLayout(UserMixerLayout[] regionList) {
        if (regionList == null || regionList.length == 0 || recordingUserInfos == null
                || recordingUserInfos.isEmpty()) {
            log.warn("adjustDefaultVideoLayout: Invalid input.");
            return;
        }

        int canvasWidth = recorderConfig.getVideo().getWidth();
        int canvasHeight = recorderConfig.getVideo().getHeight();

        // Check for invalid canvas dimensions
        if (canvasWidth <= 0 || canvasHeight <= 0) {
            log.warn(
                    "adjustDefaultVideoLayout: Invalid canvas dimensions (" + canvasWidth + "x" + canvasHeight + ").");
            // Set all regions to zero size as a fallback
            for (int i = 0; i < regionList.length; ++i) {
                MixerLayoutConfig config = new MixerLayoutConfig();
                config.setWidth(0);
                config.setHeight(0);
                config.setX(0);
                config.setY(0);
                if (i < recordingUserInfos.size()) {
                    regionList[i].setUserId(recordingUserInfos.get(i).getUserId());
                } else {
                    regionList[i].setUserId("");
                }
                regionList[i].setConfig(config);
            }
            return;
        }

        // --- Layout for the first user (main view) ---
        RecordingUserInfo firstUser = recordingUserInfos.get(0);
        regionList[0].setUserId(firstUser.getUserId());
        MixerLayoutConfig mainConfig = new MixerLayoutConfig();

        int mainLayoutX = 0;
        int mainLayoutY = 0;
        int mainLayoutWidth = 0;
        int mainLayoutHeight = 0;
        int mainVideoWidth = firstUser.getVideoWidth();
        int mainVideoHeight = firstUser.getVideoHeight();

        if (mainVideoWidth > 0 && mainVideoHeight > 0) {
            if ((long) canvasWidth * mainVideoHeight > (long) mainVideoWidth * canvasHeight) {
                // Canvas is wider (letterbox)
                mainLayoutHeight = canvasHeight;
                mainLayoutWidth = (int) Math.round((double) mainVideoWidth * canvasHeight / mainVideoHeight);
                mainLayoutX = (canvasWidth - mainLayoutWidth) / 2;
                mainLayoutY = 0;
            } else {
                // Canvas is narrower or same (pillarbox)
                mainLayoutWidth = canvasWidth;
                mainLayoutHeight = (int) Math.round((double) mainVideoHeight * canvasWidth / mainVideoWidth);
                mainLayoutX = 0;
                mainLayoutY = (canvasHeight - mainLayoutHeight) / 2;
            }
        } else {
            log.warn("adjustDefaultVideoLayout: Invalid dimensions for main user " + firstUser.getUserId() +
                    " (video: " + mainVideoWidth + "x" + mainVideoHeight + "). Setting zero size.");
            // Keep main layout dimensions as 0
        }
        mainConfig.setX(mainLayoutX);
        mainConfig.setY(mainLayoutY);
        mainConfig.setWidth(mainLayoutWidth);
        mainConfig.setHeight(mainLayoutHeight);
        mainConfig.setZOrder(0); // Main view is at the bottom
        regionList[0].setConfig(mainConfig);

        // --- Layout for subsequent users (small overlays) ---
        float overlayTargetWidthRatio = 0.235f;
        float overlayTargetHeightRatio = 0.235f;
        float hEdgeRatio = 0.012f;
        float vEdgeRatio = 0.012f;

        int overlayTargetWidth = (int) (overlayTargetWidthRatio * canvasWidth);
        int overlayTargetHeight = (int) (overlayTargetHeightRatio * canvasHeight);

        int count = Math.min(regionList.length, recordingUserInfos.size());

        for (int i = 1; i < count; i++) {
            RecordingUserInfo overlayUser = recordingUserInfos.get(i);
            regionList[i].setUserId(overlayUser.getUserId());
            MixerLayoutConfig overlayConfig = new MixerLayoutConfig();

            int overlayVideoWidth = overlayUser.getVideoWidth();
            int overlayVideoHeight = overlayUser.getVideoHeight();

            // Calculate target position for the top-left corner of the overlay cell
            float xIndex = (i - 1) % 4;
            float yIndex = (i - 1) / 4;
            int targetX = (int) ((xIndex * (overlayTargetWidthRatio + hEdgeRatio) + hEdgeRatio) * canvasWidth);
            int targetY = (int) ((1 - (yIndex + 1) * (overlayTargetHeightRatio + vEdgeRatio)) * canvasHeight);

            // Default to zero size layout within the cell
            int layoutX = targetX;
            int layoutY = targetY;
            int layoutWidth = 0;
            int layoutHeight = 0;

            // Only calculate layout if all dimensions are positive
            if (overlayTargetWidth > 0 && overlayTargetHeight > 0 && overlayVideoWidth > 0 && overlayVideoHeight > 0) {
                int scaledWidth = 0;
                int scaledHeight = 0;

                // Compare aspect ratios of target overlay area and video
                if ((long) overlayTargetWidth * overlayVideoHeight > (long) overlayVideoWidth * overlayTargetHeight) {
                    // Target area is relatively wider (letterbox for video)
                    scaledHeight = overlayTargetHeight;
                    scaledWidth = (int) Math
                            .round((double) overlayVideoWidth * overlayTargetHeight / overlayVideoHeight);
                } else {
                    // Target area is relatively narrower or same (pillarbox for video)
                    scaledWidth = overlayTargetWidth;
                    scaledHeight = (int) Math
                            .round((double) overlayVideoHeight * overlayTargetWidth / overlayVideoWidth);
                }

                // Center the scaled video within the target overlay area
                layoutWidth = scaledWidth;
                layoutHeight = scaledHeight;
                layoutX = targetX + (overlayTargetWidth - scaledWidth) / 2;
                layoutY = targetY + (overlayTargetHeight - scaledHeight) / 2;
            } else {
                log.warn(
                        "adjustDefaultVideoLayout: Invalid dimensions for overlay user " + overlayUser.getUserId() +
                                " (video: " + overlayVideoWidth + "x" + overlayVideoHeight + ", target: "
                                + overlayTargetWidth + "x" + overlayTargetHeight + "). Setting zero size.");
                // Keep layout dimensions as 0
            }

            overlayConfig.setX(layoutX);
            overlayConfig.setY(layoutY);
            overlayConfig.setWidth(layoutWidth);
            overlayConfig.setHeight(layoutHeight);
            overlayConfig.setZOrder(1); // Overlays are on top
            regionList[i].setConfig(overlayConfig);
        }

        // Handle remaining regionList elements if regionList.length >
        // recordingUserInfos.size()
        for (int i = count; i < regionList.length; i++) {
            MixerLayoutConfig config = new MixerLayoutConfig();
            config.setWidth(0);
            config.setHeight(0);
            config.setX(0);
            config.setY(0);
            regionList[i].setUserId("");
            regionList[i].setConfig(config);
        }
    }

    private void adjustVerticalLayout(UserMixerLayout[] regionList, String maxResolutionUid) {
        if (recordingUserInfos.size() <= 5) {
            adjustVideo5Layout(regionList, maxResolutionUid);
        } else if (recordingUserInfos.size() <= 7) {
            adjustVideo7Layout(regionList, maxResolutionUid);
        } else if (recordingUserInfos.size() <= 9) {
            adjustVideo9Layout(regionList, maxResolutionUid);
        } else {
            adjustVideo17Layout(regionList, maxResolutionUid);
        }
    }

    private void setMaxResolutionUid(int number, RecordingUserInfo maxUserInfo, UserMixerLayout[] regionList,
            double weightRatio) {

        if (maxUserInfo == null) {
            log.error("setMaxResolutionUid: maxUserInfo is null for index " + number);
            // Set a default zero-size layout
            regionList[number].setUserId("");
            MixerLayoutConfig layoutConfig = new MixerLayoutConfig();
            layoutConfig.setX(0);
            layoutConfig.setY(0);
            layoutConfig.setWidth(0);
            layoutConfig.setHeight(0);
            regionList[number].setConfig(layoutConfig);
            return;
        }

        regionList[number].setUserId(maxUserInfo.getUserId());
        MixerLayoutConfig layoutConfig = new MixerLayoutConfig();

        int canvasWidth = recorderConfig.getVideo().getWidth();
        int canvasHeight = recorderConfig.getVideo().getHeight();
        int videoWidth = maxUserInfo.getVideoWidth();
        int videoHeight = maxUserInfo.getVideoHeight();

        // Define target region for the main user
        int targetX = 0;
        int targetY = 0;
        int targetWidth = (int) (weightRatio * canvasWidth);
        int targetHeight = canvasHeight;

        // Calculate layout using helper logic
        LayoutCoordinates coords = calculateLayoutInRegion(videoWidth, videoHeight, targetX, targetY, targetWidth,
                targetHeight);

        layoutConfig.setX(coords.getX());
        layoutConfig.setY(coords.getY());
        layoutConfig.setWidth(coords.getWidth());
        layoutConfig.setHeight(coords.getHeight());
        layoutConfig.setAlpha(1.0f);
        regionList[number].setConfig(layoutConfig);
    }

    private void adjustVideo5Layout(UserMixerLayout[] regionList, String maxResolutionUid) {
        boolean flag = false;
        int number = 0;
        int i = 0;

        for (RecordingUserInfo recordingUserInfo : recordingUserInfos) {
            if (maxResolutionUid.equals(recordingUserInfo.getUserId())) {
                flag = true;
                setMaxResolutionUid(number, recordingUserInfo, regionList, 0.8);
                number++;
                continue;
            }
            regionList[number].setUserId(recordingUserInfo.getUserId());
            float yIndex = flag ? (number - 1) % 4 : number % 4;
            MixerLayoutConfig layoutConfig = new MixerLayoutConfig();
            // Calculate the target area for the small window
            int targetX = (int) (0.8f * recorderConfig.getVideo().getWidth());
            int targetY = (int) (0.25f * yIndex * recorderConfig.getVideo().getHeight());
            int targetWidth = (int) (0.2f * recorderConfig.getVideo().getWidth());
            int targetHeight = (int) (0.25f * recorderConfig.getVideo().getHeight());

            int videoWidth = recordingUserInfo.getVideoWidth();
            int videoHeight = recordingUserInfo.getVideoHeight();

            // Default to zero size layout within the cell
            int layoutX = targetX;
            int layoutY = targetY;
            int layoutWidth = 0;
            int layoutHeight = 0;

            // Only calculate layout if all dimensions are positive
            if (targetWidth > 0 && targetHeight > 0 && videoWidth > 0 && videoHeight > 0) {
                LayoutCoordinates coords = calculateLayoutInRegion(videoWidth, videoHeight, targetX, targetY,
                        targetWidth, targetHeight);
                layoutX = coords.getX();
                layoutY = coords.getY();
                layoutWidth = coords.getWidth();
                layoutHeight = coords.getHeight();
            } else {
                log.warn(
                        "adjustVideo5Layout: Invalid dimensions for small window user " + recordingUserInfo.getUserId()
                                +
                                " (video: " + videoWidth + "x" + videoHeight + ", target: "
                                + targetWidth + "x" + targetHeight + "). Setting zero size.");
                // Keep layout dimensions as 0
            }

            layoutConfig.setX(layoutX);
            layoutConfig.setY(layoutY);
            layoutConfig.setWidth(layoutWidth);
            layoutConfig.setHeight(layoutHeight);
            layoutConfig.setAlpha(1.0f);
            regionList[number].setConfig(layoutConfig);
            number++;
            i++;
            if (i == 4 && !flag) {
                adjustVideo7Layout(regionList, maxResolutionUid);
            }
        }
    }

    private void adjustVideo7Layout(UserMixerLayout[] regionList, String maxResolutionUid) {
        boolean flag = false;
        int number = 0;
        int i = 0;

        for (RecordingUserInfo recordingUserInfo : recordingUserInfos) {
            if (maxResolutionUid.equals(recordingUserInfo.getUserId())) {
                flag = true;
                setMaxResolutionUid(number, recordingUserInfo, regionList, 6.f / 7);
                number++;
                continue;
            }
            regionList[number].setUserId(recordingUserInfo.getUserId());
            float yIndex = flag ? (number - 1) % 6 : number % 6;
            MixerLayoutConfig layoutConfig = new MixerLayoutConfig();
            // Calculate the target area for the small window
            int targetX = (int) (6.f / 7 * recorderConfig.getVideo().getWidth());
            int targetY = (int) (1.f / 6 * yIndex * recorderConfig.getVideo().getHeight());
            int targetWidth = (int) (1.f / 7 * recorderConfig.getVideo().getWidth());
            int targetHeight = (int) (1.f / 6 * recorderConfig.getVideo().getHeight());

            int videoWidth = recordingUserInfo.getVideoWidth();
            int videoHeight = recordingUserInfo.getVideoHeight();

            // Default to zero size layout within the cell
            int layoutX = targetX;
            int layoutY = targetY;
            int layoutWidth = 0;
            int layoutHeight = 0;

            // Only calculate layout if all dimensions are positive
            if (targetWidth > 0 && targetHeight > 0 && videoWidth > 0 && videoHeight > 0) {
                LayoutCoordinates coords = calculateLayoutInRegion(videoWidth, videoHeight, targetX, targetY,
                        targetWidth, targetHeight);
                layoutX = coords.getX();
                layoutY = coords.getY();
                layoutWidth = coords.getWidth();
                layoutHeight = coords.getHeight();
            } else {
                log.warn(
                        "adjustVideo7Layout: Invalid dimensions for small window user " + recordingUserInfo.getUserId()
                                +
                                " (video: " + videoWidth + "x" + videoHeight + ", target: "
                                + targetWidth + "x" + targetHeight + "). Setting zero size.");
                // Keep layout dimensions as 0
            }

            layoutConfig.setX(layoutX);
            layoutConfig.setY(layoutY);
            layoutConfig.setWidth(layoutWidth);
            layoutConfig.setHeight(layoutHeight);
            layoutConfig.setAlpha(1.0f);
            regionList[number].setConfig(layoutConfig);
            number++;
            i++;
            if (i == 6 && !flag) {
                adjustVideo9Layout(regionList, maxResolutionUid);
            }
        }
    }

    private void adjustVideo9Layout(UserMixerLayout[] regionList, String maxResolutionUid) {
        if (regionList == null || regionList.length == 0 || recordingUserInfos == null
                || recordingUserInfos.isEmpty()) {
            log.warn("adjustVideo9Layout: Invalid input.");
            return;
        }

        RecordingUserInfo maxUserInfo = null;
        List<RecordingUserInfo> sideUserInfos = new ArrayList<>();

        // Separate max user and side users
        for (RecordingUserInfo info : recordingUserInfos) {
            if (maxResolutionUid != null && !maxResolutionUid.isEmpty() && maxResolutionUid.equals(info.getUserId())) {
                if (maxUserInfo == null) { // Found the max user
                    maxUserInfo = info;
                } else {
                    // Duplicate max user ID found? Treat subsequent ones as side users.
                    log.warn("adjustVideo9Layout: Duplicate maxResolutionUid found: " + maxResolutionUid
                            + ". Treating subsequent as side user.");
                    sideUserInfos.add(info);
                }
            } else {
                sideUserInfos.add(info);
            }
        }

        if (maxUserInfo == null && maxResolutionUid != null && !maxResolutionUid.isEmpty()) {
            log.warn("adjustVideo9Layout: maxResolutionUid '" + maxResolutionUid
                    + "' not found in user list. Treating all as side users.");
        }

        int regionIndex = 0;
        final int maxSidePanels = 8;
        final int totalCapacity = 9;

        // 1. Set layout for the main user (if exists)
        if (maxUserInfo != null) {
            if (regionIndex < regionList.length) {
                // Note: Original logic used 9.f / 5 = 1.8 ratio, seems incorrect (wider than
                // canvas?). Using 8/9 like side panels.
                setMaxResolutionUid(regionIndex, maxUserInfo, regionList, 8.f / 9);
                regionIndex++;
            } else {
                log.warn("adjustVideo9Layout: Not enough space in regionList for the main user.");
            }
        }

        // 2. Set layout for side users
        int sidePanelCount = 0;
        for (RecordingUserInfo sideUserInfo : sideUserInfos) {
            // Stop if we have filled the allocated regions or the total capacity (9) or the
            // side panel limit (8 or 9)
            if (regionIndex >= regionList.length || regionIndex >= totalCapacity) {
                log.warn(
                        "adjustVideo9Layout: Reached regionList limit or total capacity. Stopping side user layout.");
                break;
            }
            // Also check if we exceeded the max number of side panels allowed
            int maxAllowedSidePanels = (maxUserInfo != null) ? maxSidePanels : totalCapacity;
            if (sidePanelCount >= maxAllowedSidePanels) {
                log.warn("adjustVideo9Layout: Reached maximum allowed side panels (" + maxAllowedSidePanels
                        + "). Stopping side user layout.");
                break;
            }

            regionList[regionIndex].setUserId(sideUserInfo.getUserId());
            MixerLayoutConfig layoutConfig = new MixerLayoutConfig();

            // Calculate target area for the small side window
            float yIndex = sidePanelCount % maxSidePanels; // Always use maxSidePanels (8) for vertical indexing
            int targetX = (int) (8.f / 9 * recorderConfig.getVideo().getWidth());
            int targetY = (int) (1.f / maxSidePanels * yIndex * recorderConfig.getVideo().getHeight());
            int targetWidth = (int) (1.f / 9 * recorderConfig.getVideo().getWidth());
            int targetHeight = (int) (1.f / maxSidePanels * recorderConfig.getVideo().getHeight());

            int videoWidth = sideUserInfo.getVideoWidth();
            int videoHeight = sideUserInfo.getVideoHeight();

            log.debug("adjustVideo9Layout [Side User: " + sideUserInfo.getUserId() + "] - Target Region: x="
                    + targetX + ", y=" + targetY + ", w=" + targetWidth + ", h=" + targetHeight + " | Video: w="
                    + videoWidth + ", h=" + videoHeight);
            LayoutCoordinates coords = calculateLayoutInRegion(videoWidth, videoHeight, targetX, targetY, targetWidth,
                    targetHeight);
            log.debug("adjustVideo9Layout [Side User: " + sideUserInfo.getUserId() + "] - Calculated Coords: x="
                    + coords.getX() + ", y=" + coords.getY() + ", w=" + coords.getWidth() + ", h="
                    + coords.getHeight());

            layoutConfig.setX(coords.getX());
            layoutConfig.setY(coords.getY());
            layoutConfig.setWidth(coords.getWidth());
            layoutConfig.setHeight(coords.getHeight());
            layoutConfig.setAlpha(1.0f);
            regionList[regionIndex].setConfig(layoutConfig);

            regionIndex++;
            sidePanelCount++;
        }

        // 3. Clear remaining regions
        for (int j = regionIndex; j < regionList.length; j++) {
            MixerLayoutConfig config = new MixerLayoutConfig();
            config.setWidth(0);
            config.setHeight(0);
            config.setX(0);
            config.setY(0);
            regionList[j].setUserId("");
            regionList[j].setConfig(config);
        }
    }

    // Updated adjustVideo17Layout using the helper method
    private void adjustVideo17Layout(UserMixerLayout[] regionList, String maxResolutionUid) {
        if (regionList == null || regionList.length == 0 || recordingUserInfos == null
                || recordingUserInfos.isEmpty()) {
            log.warn("adjustVideo17Layout: Invalid input.");
            return;
        }

        RecordingUserInfo maxUserInfo = null;
        List<RecordingUserInfo> sideUserInfos = new ArrayList<>();

        // Separate max user and side users
        for (RecordingUserInfo info : recordingUserInfos) {
            if (maxResolutionUid != null && !maxResolutionUid.isEmpty() && maxResolutionUid.equals(info.getUserId())) {
                if (maxUserInfo == null) { // Found the max user
                    maxUserInfo = info;
                } else {
                    // Duplicate max user ID found? Treat subsequent ones as side users.
                    log.warn("adjustVideo17Layout: Duplicate maxResolutionUid found: " + maxResolutionUid
                            + ". Treating subsequent as side user.");
                    sideUserInfos.add(info);
                }
            } else {
                sideUserInfos.add(info);
            }
        }

        if (maxUserInfo == null && maxResolutionUid != null && !maxResolutionUid.isEmpty()) {
            log.warn("adjustVideo17Layout: maxResolutionUid '" + maxResolutionUid
                    + "' not found in user list. Treating all as side users.");
        }

        int regionIndex = 0;
        final int maxSidePanels = 16; // Maximum number of side panels
        final int totalCapacity = 17; // Total layout capacity
        final int rowsPerColumn = 8;
        final float mainUserWidthRatio = 0.8f; // 80% for main user
        final float sidePanelWidthRatio = (1.0f - mainUserWidthRatio) / 2; // Width for each side column

        // 1. Set layout for the main user (if exists)
        if (maxUserInfo != null) {
            if (regionIndex < regionList.length) {
                setMaxResolutionUid(regionIndex, maxUserInfo, regionList, mainUserWidthRatio);
                regionIndex++;
            } else {
                log.warn("adjustVideo17Layout: Not enough space in regionList for the main user.");
            }
        }

        // 2. Set layout for side users
        int sidePanelCount = 0;
        for (RecordingUserInfo sideUserInfo : sideUserInfos) {
            // Stop if we have filled the allocated regions or the total capacity
            if (regionIndex >= regionList.length || regionIndex >= totalCapacity) {
                log.warn(
                        "adjustVideo17Layout: Reached regionList limit or total capacity. Stopping side user layout.");
                break;
            }
            // Also check if we exceeded the max number of side panels allowed
            int maxAllowedSidePanels = (maxUserInfo != null) ? maxSidePanels : totalCapacity;
            if (sidePanelCount >= maxAllowedSidePanels) {
                log.warn("adjustVideo17Layout: Reached maximum allowed side panels (" + maxAllowedSidePanels
                        + "). Stopping side user layout.");
                break;
            }

            regionList[regionIndex].setUserId(sideUserInfo.getUserId());
            MixerLayoutConfig layoutConfig = new MixerLayoutConfig();

            // Calculate target region for the side user based on the 17-layout pattern
            int targetWidth = (int) (sidePanelWidthRatio * recorderConfig.getVideo().getWidth());
            int targetHeight = (int) (1.f / rowsPerColumn * recorderConfig.getVideo().getHeight());
            int targetX;
            int targetY;

            int currentPanelIndex = sidePanelCount; // Index of the current side panel (0 to 15)
            int columnIndex = currentPanelIndex / rowsPerColumn; // 0 for first column, 1 for second
            int rowIndex = currentPanelIndex % rowsPerColumn; // 0 to 7 within the column

            // Determine column X coordinate
            targetX = (int) ((mainUserWidthRatio + columnIndex * sidePanelWidthRatio)
                    * recorderConfig.getVideo().getWidth());
            targetY = (int) ((1.f / rowsPerColumn * rowIndex) * recorderConfig.getVideo().getHeight());

            int videoWidth = sideUserInfo.getVideoWidth();
            int videoHeight = sideUserInfo.getVideoHeight();

            log.debug("adjustVideo17Layout [Side User: " + sideUserInfo.getUserId() + ", PanelIdx: "
                    + currentPanelIndex + "] - Target Region: x=" + targetX + ", y=" + targetY + ", w=" + targetWidth
                    + ", h=" + targetHeight + " | Video: w=" + videoWidth + ", h=" + videoHeight);
            LayoutCoordinates coords = calculateLayoutInRegion(videoWidth, videoHeight, targetX, targetY, targetWidth,
                    targetHeight);
            log.debug("adjustVideo17Layout [Side User: " + sideUserInfo.getUserId() + "] - Calculated Coords: x="
                    + coords.getX() + ", y=" + coords.getY() + ", w=" + coords.getWidth() + ", h="
                    + coords.getHeight());

            layoutConfig.setX(coords.getX());
            layoutConfig.setY(coords.getY());
            layoutConfig.setWidth(coords.getWidth());
            layoutConfig.setHeight(coords.getHeight());
            layoutConfig.setAlpha(1.0f);
            regionList[regionIndex].setConfig(layoutConfig);

            regionIndex++; // Increment region list index
            sidePanelCount++; // Increment side panel counter
        }

        // 3. Clear remaining regions if any were not filled
        for (int j = regionIndex; j < regionList.length; j++) {
            MixerLayoutConfig config = new MixerLayoutConfig();
            config.setWidth(0);
            config.setHeight(0);
            config.setX(0);
            config.setY(0);
            regionList[j].setUserId(""); // Set empty user ID or null
            regionList[j].setConfig(config);
        }
    }

}