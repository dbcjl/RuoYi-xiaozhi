package com.ruoyi.xiaozhi.chat.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

/**
 * 音频工具类
 * @author ruoyi-xiaozhi
 */
public class AudioUtils {

    /**
     * 将字节数组转换为浮点数组（小端序）
     * @param data  字节数组
     * @return  浮点数组
     */
    public static float[] byteToFloat(byte[] data) {
        float[] audioData = new float[data.length / 2];
        for (int i = 0; i < audioData.length; i++) {
            audioData[i] = ((data[i * 2] & 0xff) | (data[i * 2 + 1] << 8)) / 32767.0f;
        }
        return audioData;
    }

    /**
     * 将浮点数组转换为字节数组（假设float范围在[-1.0, 1.0]之间，对应16位PCM）（小端序）
     * @param audioData 浮点数组
     * @return 字节数组
     */
    public static byte[] floatToByte(float[] audioData) {
        byte[] data = new byte[audioData.length * 2];
        for (int i = 0; i < audioData.length; i++) {
            // 限制范围在[-1.0, 1.0]
            float sample = Math.max(-1.0f, Math.min(1.0f, audioData[i]));
            // 转换为16位有符号整数
            short val = (short) (sample * 32767);
            data[i * 2] = (byte) (val & 0xff);         // 低字节
            data[i * 2 + 1] = (byte) ((val >> 8) & 0xff); // 高字节
        }
        return data;
    }

    /**
     * 将short数组转换为byte数组（小端序）
     * @param shortArray    short数组
     * @return byte数组
     */
    public static byte[] shortToByte(short[] shortArray) {
        byte[] byteArray = new byte[shortArray.length * 2];
        for (int i = 0; i < shortArray.length; i++) {
            byteArray[i * 2] = (byte) (shortArray[i] & 0xFF);        // 低字节
            byteArray[i * 2 + 1] = (byte) ((shortArray[i] >> 8) & 0xFF); // 高字节
        }
        return byteArray;
    }

    /**
     * 将ShortBuffer转换为byte数组
     */
    public static byte[] shortToByte(ShortBuffer shortBuffer) {
        int length = shortBuffer.remaining(); // 剩余 short 数量
        byte[] result = new byte[length * 2];
        // 创建小端字节缓冲区视图
        ByteBuffer byteBuffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
        // 快速写入全部 short
        while (shortBuffer.hasRemaining()) {
            byteBuffer.putShort(shortBuffer.get());
        }
        return result;
    }

    /**
     * 将ShortBuffer复制到byte数组（小端序）
     * @param shortBuffer   shortBuffer数据
     * @param dest          目标字节数组
     * @param offset        偏移量
     * @param length        长度
     */
    public static void copy(ShortBuffer shortBuffer, byte[] dest, int offset, int length) {
        for (int i = offset; i < offset + length; i+=2) {
            short value = shortBuffer.get();
            dest[i] = (byte) (value & 0xFF);
            dest[i + 1] = (byte) ((value >> 8) & 0xFF);
        }
    }

}
