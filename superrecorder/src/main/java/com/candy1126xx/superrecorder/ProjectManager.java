package com.candy1126xx.superrecorder;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import static android.media.MediaCodec.BUFFER_FLAG_CODEC_CONFIG;
import static android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM;
import static android.media.MediaCodec.BUFFER_FLAG_KEY_FRAME;
import static android.media.MediaCodec.BUFFER_FLAG_SYNC_FRAME;

/**
 * Created by Administrator on 2017/6/30 0030.
 */

public class ProjectManager {

    //---------------------------------生成Clip
    private AVMuxer currentMuxer;

    private int index;
    //---------------------------------生成Clip

    //---------------------------------合成
    private MergeMuxer mergeMuxer;
    //---------------------------------合成

    //---------------------------------合成复用
    private long videoDuration;
    private long audioDuration;

    private ByteBuffer inputBuffer;

    private MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

    private int sampleSize;

    private MediaExtractor currentExtractor;
    //---------------------------------合成复用

    private String rootPath = Environment.getExternalStorageDirectory() +
            File.separator + "SuperRecorder";

    private String tempPath = rootPath + File.separator + "demo";

    private String resultPath = rootPath + File.separator + "output.mp4";

    private ProjectManagerCallback callback;

    public ProjectManager() {
        File rootFile = new File(rootPath);
        if (!rootFile.exists()) rootFile.mkdirs();
        File tempFile = new File(tempPath);
        if (tempFile.exists()) tempFile.delete();
        tempFile.mkdirs();
    }

    public void createNewClip() {
        index++;
        currentMuxer = new AVMuxer(new File(tempPath + File.separator + "demo" + index + ".mp4"));
        if (callback != null) callback.onNewClipCreated();
    }

    public void stopCurrentClip() {
        currentMuxer.stop();
        if (callback != null) callback.onCurrentClipStop();
    }

    public void mergeAllClips() {
        mergeMuxer = new MergeMuxer(new File(resultPath));
        File tempFile = new File(tempPath);
        File[] clipFiles = tempFile.listFiles();
        for (File file : clipFiles) {
            mergeClip(file.getAbsolutePath());
        }
        mergeMuxer.stop();
        mergeMuxer = null;
        if (callback != null) callback.onAllClipsMerged();
    }

    private void mergeClip(String clipPath) {
        try {
            currentExtractor = new MediaExtractor();
            currentExtractor.setDataSource(clipPath);
            int numTracks = currentExtractor.getTrackCount();

            // 先加入轨道
            for (int i = 0; i < numTracks; i++) {
                currentExtractor.selectTrack(i);
                MediaFormat format = currentExtractor.getTrackFormat(i);
                if (format.getString(MediaFormat.KEY_MIME).startsWith("video")) {
                    mergeMuxer.addTrack(format, 1);
                } else if (format.getString(MediaFormat.KEY_MIME).startsWith("audio")) {
                    mergeMuxer.addTrack(format, 2);
                }
                currentExtractor.unselectTrack(i);
            }

            for (int i = 0; i < numTracks; i++) {
                currentExtractor.selectTrack(i);
                MediaFormat format = currentExtractor.getTrackFormat(i);
                int maxInputSize = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
                inputBuffer = ByteBuffer.allocate(maxInputSize);

                if (format.getString(MediaFormat.KEY_MIME).startsWith("video")) {
                    while ((sampleSize = currentExtractor.readSampleData(inputBuffer, 0)) >= 0) {
                        bufferInfo.offset = 0;
                        bufferInfo.size = sampleSize;
                        bufferInfo.flags = currentExtractor.getSampleFlags();
                        bufferInfo.presentationTimeUs = videoDuration + currentExtractor.getSampleTime();
                        mergeMuxer.writeSample(inputBuffer, bufferInfo, 1);
                        currentExtractor.advance();
                    }
                    videoDuration += format.getLong(MediaFormat.KEY_DURATION);
                    currentExtractor.unselectTrack(i);
                } else if (format.getString(MediaFormat.KEY_MIME).startsWith("audio")) {
                    while ((sampleSize = currentExtractor.readSampleData(inputBuffer, 0)) >= 0) {
                        bufferInfo.offset = 0;
                        bufferInfo.size = sampleSize;
                        bufferInfo.flags = currentExtractor.getSampleFlags();
                        bufferInfo.presentationTimeUs = audioDuration + currentExtractor.getSampleTime();
                        mergeMuxer.writeSample(inputBuffer, bufferInfo, 2);
                        currentExtractor.advance();
                    }
                    audioDuration += format.getLong(MediaFormat.KEY_DURATION);
                    currentExtractor.unselectTrack(i);
                }
            }

            currentExtractor.release();
            currentExtractor = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeToResult(int trackIndex, long pts) {

    }

    public AVMuxer getCurrentMuxer() {
        return currentMuxer;
    }

    public void setCallback(ProjectManagerCallback callback) {
        this.callback = callback;
    }

    public interface ProjectManagerCallback {
        void onNewClipCreated();

        void onCurrentClipStop();

        void onAllClipsMerged();
    }
}
