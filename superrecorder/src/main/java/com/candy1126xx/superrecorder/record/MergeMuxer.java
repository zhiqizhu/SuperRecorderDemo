package com.candy1126xx.superrecorder.record;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by Administrator on 2017/7/3 0003.
 */

public class MergeMuxer {

    private MediaMuxer muxer;
    private int videoTrackerIndex = -1;
    private int audioTrackerIndex = -1;

    private int videoFrameCount;

    private boolean muxerStarted;

    public MergeMuxer(File outputFile) {
        try {
            if (outputFile.exists()) outputFile.delete();
            outputFile.createNewFile();
            muxer = new MediaMuxer(outputFile.getPath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        muxer.release();
    }

    public void addTrack(MediaFormat format, int type) {
        switch (type) {
            case 1:
                if (videoTrackerIndex == -1) videoTrackerIndex = muxer.addTrack(format);
                break;
            case 2:
                if (audioTrackerIndex == -1) audioTrackerIndex = muxer.addTrack(format);
                break;
        }
        if (videoTrackerIndex != -1 && audioTrackerIndex != -1 && !muxerStarted) {
            muxer.start();
            muxerStarted = true;
        }
    }

    public void writeSample(ByteBuffer byteBuf, MediaCodec.BufferInfo bufferInfo, int type) {
        switch (type) {
            case 1:
                muxer.writeSampleData(videoTrackerIndex, byteBuf, bufferInfo);
                videoFrameCount++;
                break;
            case 2:
                if (videoFrameCount > 0)
                    muxer.writeSampleData(audioTrackerIndex, byteBuf, bufferInfo);
                break;
        }
    }

}
