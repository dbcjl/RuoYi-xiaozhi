// Copyright 2024 Xiaomi Corporation

package com.k2fsa.sherpa.onnx;

import com.ruoyi.xiaozhi.onnx.SherpaOnnxLoader;

public class KeywordSpotter {
    static {
        SherpaOnnxLoader.init();
    }

    private long ptr = 0;

    public KeywordSpotter(KeywordSpotterConfig config) {
        ptr = newFromFile(config);
    }

    public OnlineStream createStream(String keywords) {
        long p = createStream(ptr, keywords);
        return new OnlineStream(p);
    }

    public OnlineStream createStream() {
        long p = createStream(ptr, "");
        return new OnlineStream(p);
    }

    public void decode(OnlineStream s) {
        decode(ptr, s.getPtr());
    }

    public void reset(OnlineStream s) {
        reset(ptr, s.getPtr());
    }

    public boolean isReady(OnlineStream s) {
        return isReady(ptr, s.getPtr());
    }

    public KeywordSpotterResult getResult(OnlineStream s) {
        Object[] arr = getResult(ptr, s.getPtr());
        String keyword = (String) arr[0];
        String[] tokens = (String[]) arr[1];
        float[] timestamps = (float[]) arr[2];
        return new KeywordSpotterResult(keyword, tokens, timestamps);
    }

    // You'd better call it manually if it is not used anymore
    public void release() {
        if (this.ptr == 0) {
            return;
        }
        delete(this.ptr);
        this.ptr = 0;
    }

    private native long newFromFile(KeywordSpotterConfig config);

    private native void delete(long ptr);

    private native long createStream(long ptr, String keywords);

    private native void decode(long ptr, long streamPtr);

    private native void reset(long ptr, long streamPtr);

    private native boolean isReady(long ptr, long streamPtr);

    private native Object[] getResult(long ptr, long streamPtr);
}
