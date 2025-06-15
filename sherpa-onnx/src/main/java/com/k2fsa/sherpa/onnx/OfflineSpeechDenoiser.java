// Copyright 2025 Xiaomi Corporation

package com.k2fsa.sherpa.onnx;

import com.ruoyi.xiaozhi.onnx.SherpaOnnxLoader;

public class OfflineSpeechDenoiser {
    static {
        SherpaOnnxLoader.init();
    }

    private long ptr = 0;

    public OfflineSpeechDenoiser(OfflineSpeechDenoiserConfig config) {
        ptr = newFromFile(config);
    }

    public int getSampleRate() {
        return getSampleRate(ptr);
    }

    public DenoisedAudio run(float[] samples, int sampleRate) {
        return run(ptr, samples, sampleRate);
    }

    public void release() {
        if (this.ptr == 0) {
            return;
        }
        delete(this.ptr);
        this.ptr = 0;
    }

    private native void delete(long ptr);

    private native int getSampleRate(long ptr);

    private native DenoisedAudio run(long ptr, float[] samples, int sampleRate);

    private native long newFromFile(OfflineSpeechDenoiserConfig config);
}
