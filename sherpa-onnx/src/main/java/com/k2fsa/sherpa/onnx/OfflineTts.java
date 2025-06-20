// Copyright 2024 Xiaomi Corporation

package com.k2fsa.sherpa.onnx;

import com.ruoyi.xiaozhi.onnx.SherpaOnnxLoader;

public class OfflineTts {
    static {
        SherpaOnnxLoader.init();
    }

    private long ptr = 0;

    public OfflineTts(OfflineTtsConfig config) {
        ptr = newFromFile(config);
    }

    public int getSampleRate() {
        return getSampleRate(ptr);
    }

    public GeneratedAudio generate(String text) {
        return generate(text, 0, 1.0f);
    }

    public GeneratedAudio generate(String text, int sid) {
        return generate(text, sid, 1.0f);
    }

    public GeneratedAudio generate(String text, int sid, float speed) {
        Object[] arr = generateImpl(ptr, text, sid, speed);
        float[] samples = (float[]) arr[0];
        int sampleRate = (int) arr[1];
        return new GeneratedAudio(samples, sampleRate);
    }

    public GeneratedAudio generateWithCallback(String text, OfflineTtsCallback callback) {
        return generateWithCallback(text, 0, 1.0f, callback);
    }

    public GeneratedAudio generateWithCallback(String text, int sid, OfflineTtsCallback callback) {
        return generateWithCallback(text, sid, 1.0f, callback);
    }

    public GeneratedAudio generateWithCallback(String text, int sid, float speed, OfflineTtsCallback callback) {
        Object[] arr = generateWithCallbackImpl(ptr, text, sid, speed, callback);
        float[] samples = (float[]) arr[0];
        int sampleRate = (int) arr[1];
        return new GeneratedAudio(samples, sampleRate);
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

    private native int getNumSpeakers(long ptr);

    private native Object[] generateImpl(long ptr, String text, int sid, float speed);

    private native Object[] generateWithCallbackImpl(long ptr, String text, int sid, float speed, OfflineTtsCallback callback);

    private native long newFromFile(OfflineTtsConfig config);
}
