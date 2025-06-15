package com.ruoyi.xiaozhi.chat;

import org.bytedeco.ffmpeg.avutil.AVChannelLayout;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.ffmpeg.global.swresample;
import org.bytedeco.ffmpeg.swresample.SwrContext;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.PointerPointer;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * 流式重采样
 */
public class FFMpegResampleTests {

    @Test
    public void test() throws IOException {

        final int inSampleRate = 22050;
        final int outSampleRate = 16000;
        final int channels = 1;
        final int inFormat = avutil.AV_SAMPLE_FMT_S16;
        final int outFormat = avutil.AV_SAMPLE_FMT_S16;

        File inputFile = new File("/Users/xxx/Desktop/resample-test/output.pcm");
        File outputFile = new File("/Users/xxxx/Desktop/resample-test/output_16000_opts2.pcm");

        // 创建输入/输出布局
        AVChannelLayout inLayout = new AVChannelLayout();
        AVChannelLayout outLayout = new AVChannelLayout();
        avutil.av_channel_layout_default(inLayout, channels);
        avutil.av_channel_layout_default(outLayout, channels);

        // 使用 swr_alloc_set_opts2 创建 SwrContext
        SwrContext[] swrArray = new SwrContext[1];
        PointerPointer<SwrContext> swrPtr = new PointerPointer<>(1);
        int ret = swresample.swr_alloc_set_opts2(
                swrPtr,
                outLayout, outFormat, outSampleRate,
                inLayout, inFormat, inSampleRate,
                0, null
        );
        if (ret < 0) throw new RuntimeException("swr_alloc_set_opts2 failed: " + ret);

        SwrContext swr = new SwrContext(swrPtr.get(SwrContext.class));
        if (swr == null) throw new IllegalStateException("SwrContext is null");

        if ((ret = swresample.swr_init(swr)) < 0)
            throw new RuntimeException("swr_init failed: " + ret);

        // 开始读写 PCM 数据
        try (FileInputStream fis = new FileInputStream(inputFile);
             FileOutputStream fos = new FileOutputStream(outputFile)) {

            byte[] inBuf = new byte[4096];  // 2048 samples
            ByteBuffer inByteBuffer = ByteBuffer.allocateDirect(4096).order(ByteOrder.LITTLE_ENDIAN);
            ByteBuffer outByteBuffer = ByteBuffer.allocateDirect(8192).order(ByteOrder.LITTLE_ENDIAN);

            PointerPointer<BytePointer> inPtrs = new PointerPointer<>(1);
            PointerPointer<BytePointer> outPtrs = new PointerPointer<>(1);

            while (true) {
                int bytesRead = fis.read(inBuf);
                if (bytesRead == -1) break;
                if (bytesRead % 2 != 0) bytesRead--;

                int inSampleCount = bytesRead / 2;
                inByteBuffer.clear();
                inByteBuffer.asShortBuffer().put(ByteBuffer.wrap(inBuf).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer());

                BytePointer inData = new BytePointer(inByteBuffer);
                inPtrs.put(0, inData);

                int maxOutSamples = swresample.swr_get_out_samples(swr, inSampleCount);
                BytePointer outData = new BytePointer(outByteBuffer);
                outPtrs.put(0, outData);

                int outSampleCount = swresample.swr_convert(
                        swr, outPtrs, maxOutSamples,
                        inPtrs, inSampleCount
                );

                if (outSampleCount < 0) throw new RuntimeException("swr_convert failed");

                int outBytes = outSampleCount * 2; // s16: 2 bytes/sample
                byte[] outBuf = new byte[outBytes];
                outData.get(outBuf, 0, outBytes);
                fos.write(outBuf);
            }

            System.out.println("✅ 重采样完成：使用 swr_alloc_set_opts2");

        } finally {
            swresample.swr_free(swrPtr);
        }

    }

}
