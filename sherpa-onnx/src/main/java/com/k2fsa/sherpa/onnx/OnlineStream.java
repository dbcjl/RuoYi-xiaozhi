// Copyright 2022-2023 by zhaoming
// Copyright 2024 Xiaomi Corporation

package com.k2fsa.sherpa.onnx;

import com.ruoyi.xiaozhi.onnx.SherpaOnnxLoader;

public class OnlineStream {
    static {
        SherpaOnnxLoader.init();
    }

    private long ptr = 0;

    public OnlineStream() {
        this.ptr = 0;
    }

    public OnlineStream(long ptr) {
        this.ptr = ptr;
    }

    public long getPtr() {
        return ptr;
    }

    public void setPtr(long ptr) {
        this.ptr = ptr;
    }

    public void acceptWaveform(float[] samples, int sampleRate) {
        acceptWaveform(this.ptr, samples, sampleRate);
    }

    public void inputFinished() {
        inputFinished(this.ptr);
    }

    public void release() {
        // stream object must be release after used
        if (this.ptr == 0) {
            return;
        }
        delete(this.ptr);
        this.ptr = 0;
    }

    private native void acceptWaveform(long ptr, float[] samples, int sampleRate);

    private native void inputFinished(long ptr);

    private native void delete(long ptr);
}