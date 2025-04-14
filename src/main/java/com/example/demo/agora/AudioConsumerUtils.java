package com.example.demo.agora;

import java.nio.ByteBuffer;

import io.agora.rtc.AgoraAudioPcmDataSender;
import io.agora.rtc.AudioFrame;

/**
 * Base class for consuming PCM data and pushing it to RTC channels.
 * 
 * In AI scenarios:
 * - When TTS returns data: Call AudioConsumerUtils.pushPcmData method to
 * directly
 * push the returned TTS data to AudioConsumerUtils
 * - In another "timer" trigger function, call AudioConsumerUtils.consume()
 * method
 * to push data to RTC
 * 
 * Recommendations:
 * - "Timer" can be implemented using various approaches .
 * Just need to call AudioConsumerUtils.consume() in the timer trigger function
 * - Timer trigger interval can be consistent with existing business timer
 * intervals or adjusted based on business needs,
 * recommended between 40-80ms
 * 
 * Usage Pattern:
 * 1. Prerequisites:
 * - Application needs to implement its own timer with interval between
 * [10ms,80ms].
 * The timer trigger method is represented as app::TimeFunc.
 * - One user can only correspond to one AudioConsumerUtils object, ensuring one
 * producer corresponds to one consumer.
 * 
 * 2. Usage Steps:
 * A. Create an AudioConsumerUtils object for each "PCM data producing" userid,
 * ensuring one producer corresponds to one consumer
 * B. When PCM data is generated (e.g., TTS return), call
 * AudioConsumer.AudioConsumerUtils(data)
 * C. When consumption is needed , call
 * AudioConsumerUtils.consume() method,
 * which will automatically complete data consumption by pushing to RTC channel
 * D. For interruption (e.g., stopping current AI dialogue): call
 * AudioConsumerUtils.clear() method,
 * which will automatically clear current buffer data
 * E. On exit, call release() method to free resources
 */

public class AudioConsumerUtils {
    // Constants
    private static final int START_BY_MAX_FRAME_SIZE = 18;
    private static final int START_BY_MIN_FRAME_SIZE = 6;
    private static final int INTERVAL_ONE_FRAME = 10; // ms
    private static final int INTERVAL_PCM_INTERRUPT = 200; // ms
    private static final int BYTES_PER_SAMPLE = 2; // 16-bit audio

    // Instance variables
    private AgoraAudioPcmDataSender audioFrameSender;
    private final int numOfChannels;
    private final int sampleRate;
    private final int oneFrameSize;
    private int startCacheDataSize;
    private ByteBuffer buffer;
    private long startedTimestamp;
    private long lastSendTimestamp;
    private int consumedFrameCount;
    private final AudioFrame audioFrame;

    // Constructor
    public AudioConsumerUtils(AgoraAudioPcmDataSender audioFrameSender, int numOfChannels, int sampleRate) {
        this.audioFrameSender = audioFrameSender;
        this.numOfChannels = numOfChannels;
        this.sampleRate = sampleRate;
        this.oneFrameSize = numOfChannels * (sampleRate / 1000) * INTERVAL_ONE_FRAME * 2;
        this.startCacheDataSize = oneFrameSize * START_BY_MAX_FRAME_SIZE;
        this.buffer = ByteBuffer.allocate(startCacheDataSize * 2);
        this.startedTimestamp = 0;
        this.lastSendTimestamp = 0;
        this.consumedFrameCount = 0;
        this.audioFrame = new AudioFrame();
    }

    public int getOneFrameSize() {
        return oneFrameSize;
    }

    // Calculate samples per channel
    public int getSamplesPerChannel(int dataSize) {
        return sampleRate / 1000 * INTERVAL_ONE_FRAME * (dataSize / oneFrameSize);
    }

    // Push pcm data into the cache
    // data size must be multiple of oneFrameSize
    public synchronized void pushPcmData(byte[] data) {
        if (data == null || data.length == 0) {
            return;
        }
        try {
            int dataLeft = data.length % oneFrameSize;
            if (dataLeft != 0) {
                // If the data size is not a multiple of one frame size, add zeros to the end of
                // the data
                byte[] newData = new byte[data.length + (oneFrameSize - dataLeft)];
                System.arraycopy(data, 0, newData, 0, data.length);
                data = newData;
            }
            // If the buffer space is insufficient, create a larger buffer
            if (buffer.remaining() < data.length) {
                // If the buffer space is insufficient, create a larger buffer
                int newCapacity = Math.max(buffer.capacity() * 2, buffer.position() + data.length);
                ByteBuffer newBuffer = ByteBuffer.allocate(newCapacity);

                // Copy the contents of the current buffer to the new buffer
                buffer.flip();
                newBuffer.put(buffer);

                // Switch to the new buffer and ensure it's in the correct write position
                buffer = newBuffer;
            }

            buffer.put(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // consume data from the cache and return consume frame count
    public synchronized int consume() {
        if (buffer == null || audioFrameSender == null) {
            return -1;
        }
        long currentTime = System.currentTimeMillis();
        if ((startedTimestamp != 0) && (currentTime - lastSendTimestamp > INTERVAL_PCM_INTERRUPT)) {
            int cacheFrameCount = getBufferSize() / oneFrameSize;

            if (cacheFrameCount < START_BY_MIN_FRAME_SIZE) {
                return -2;
            }

            int maxFrameSize = Math.min(cacheFrameCount, START_BY_MAX_FRAME_SIZE);
            startCacheDataSize = oneFrameSize * maxFrameSize;

            startedTimestamp = 0;
            lastSendTimestamp = 0;
            consumedFrameCount = 0;
        }

        try {
            if (startedTimestamp == 0) {
                if (getBufferSize() >= startCacheDataSize) {
                    startedTimestamp = currentTime;
                    return sendPcmData(extractData(startCacheDataSize, currentTime));
                } else {
                    return -2;
                }
            } else {
                long elapsedTime = currentTime - startedTimestamp;

                if (elapsedTime < 0) {
                    startedTimestamp = lastSendTimestamp;
                    elapsedTime = currentTime - startedTimestamp;
                }

                int startedAllFrameCount = (int) (elapsedTime / INTERVAL_ONE_FRAME);
                if (consumedFrameCount > startedAllFrameCount) {
                    consumedFrameCount = startedAllFrameCount;
                }
                int requiredFrameCount = startedAllFrameCount - consumedFrameCount;
                int wantedFrameCount = Math.min(requiredFrameCount, getBufferSize() / oneFrameSize);
                if (wantedFrameCount > 0) {
                    int requiredFrameSize = wantedFrameCount * oneFrameSize;
                    consumedFrameCount += requiredFrameCount;
                    if (consumedFrameCount < 0) {
                        consumedFrameCount = 0;
                        startedTimestamp = currentTime;
                    }
                    return sendPcmData(extractData(requiredFrameSize, currentTime));
                } else {
                    if (getBufferSize() > 0) {
                        return -3;
                    }
                    return 0;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    // Helper method to extract data from the buffer
    private byte[] extractData(int size, long currentTime) {
        byte[] data = new byte[size];
        buffer.flip();
        buffer.get(data);
        buffer.compact();
        lastSendTimestamp = currentTime;
        return data;
    }

    private int sendPcmData(byte[] data) {
        if (audioFrameSender != null && data != null && data.length > 0) {
            audioFrameSender.send(data, 0,
                    getSamplesPerChannel(data.length), BYTES_PER_SAMPLE,
                    numOfChannels,
                    sampleRate);
            return data.length / oneFrameSize;
        }
        return -1;
    }

    private synchronized int getBufferSize() {
        // In write mode, position is the write position, and limit is the buffer's
        // capacity
        return buffer.position();
    }

    /**
     * Gets the remaining cache duration in milliseconds.
     *
     * @return the remaining cache duration in milliseconds.
     */
    public synchronized int getRemainingCacheDurationInMs() {
        // Calculate the number of frames in the buffer
        int cacheFrameCount = getBufferSize() / oneFrameSize;

        // Calculate and return the remaining cache duration in milliseconds
        return cacheFrameCount * INTERVAL_ONE_FRAME;
    }

    // Clear the cache
    public synchronized void clear() {
        if (null != buffer) {
            buffer.clear();
        }
        startedTimestamp = 0;
        lastSendTimestamp = 0;
        consumedFrameCount = 0;
    }

    public synchronized void release() {
        clear();
        buffer = null;
        audioFrameSender = null;
    }

}
