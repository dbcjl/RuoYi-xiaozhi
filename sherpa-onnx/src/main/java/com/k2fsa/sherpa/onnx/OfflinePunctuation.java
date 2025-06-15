// Copyright 2024 Xiaomi Corporation

package com.k2fsa.sherpa.onnx;

import com.ruoyi.xiaozhi.onnx.SherpaOnnxLoader;

public class OfflinePunctuation {
    static {
        SherpaOnnxLoader.init();
    }

    private long ptr = 0;

    public OfflinePunctuation(OfflinePunctuationConfig config) {
        ptr = newFromFile(config);
    }

    public String addPunctuation(String text) {
        return addPunctuation(ptr, text);
    }

    // You'd better call it manually if it is not used anymore
    public void release() {
        if (this.ptr == 0) {
            return;
        }
        delete(this.ptr);
        this.ptr = 0;
    }

    private native void delete(long ptr);

    private native long newFromFile(OfflinePunctuationConfig config);

    private native String addPunctuation(long ptr, String text);
}
