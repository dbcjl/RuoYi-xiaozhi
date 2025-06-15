package com.ruoyi.xiaozhi.chat.core;

/**
 * 环形字节缓冲区，支持高效循环读写操作
 *
 * @author ruoyi-xiaozhi
 */
public class RingByteBuffer {

    /** 底层数据存储 */
    private final byte[] dataStore;

    /** 缓冲区总容量 */
    private final int bufferCapacity;

    /** 数据读取位置 */
    private int readPosition = 0;

    /** 数据写入位置 */
    private int writePosition = 0;

    /** 当前存储的数据量 */
    private int storedDataSize = 0;

    /**
     * 初始化环形缓冲区
     *
     * @param bufferCapacity 缓冲区容量，必须大于0
     * @throws IllegalArgumentException 如果容量不合法
     */
    public RingByteBuffer(int bufferCapacity) {
        if (bufferCapacity <= 0) {
            throw new IllegalArgumentException("缓冲区容量必须大于0");
        }
        this.bufferCapacity = bufferCapacity;
        this.dataStore = new byte[bufferCapacity];
    }

    /**
     * 写入完整字节数组
     *
     * @param inputData 要写入的数据
     */
    public void writeBytes(byte[] inputData) {
        writeBytes(inputData, 0, inputData.length);
    }

    /**
     * 写入字节数组指定部分
     *
     * @param inputData   输入数据
     * @param sourceOffset 数据起始偏移
     * @param writeLength  写入数据长度
     * @throws IllegalArgumentException 如果参数非法或空间不足
     */
    public void writeBytes(byte[] inputData, int sourceOffset, int writeLength) {
        if (inputData == null || sourceOffset < 0 || writeLength < 0 ||
                sourceOffset + writeLength > inputData.length) {
            throw new IllegalArgumentException("写入参数非法");
        }
        if (writeLength > getAvailableSpace()) {
            throw new IllegalArgumentException("写入数据超出缓冲区可用空间");
        }

        performWriteOperation(inputData, sourceOffset, writeLength);
    }

    /**
     * 读取指定长度字节数据
     *
     * @param readLength 要读取的长度
     * @return 读取的数据
     */
    public byte[] readBytes(int readLength) {
        byte[] result = new byte[readLength];
        readBytes(result);
        return result;
    }

    /**
     * 读取数据到指定数组
     * @param outputBuffer  输出缓冲区
     */
    public void readBytes(byte[] outputBuffer) {
        readBytes(outputBuffer, 0, outputBuffer.length);
    }

    /**
     * 读取数据到指定数组位置
     *
     * @param outputBuffer   输出缓冲区
     * @param targetOffset   目标偏移量
     * @param readLength     读取长度
     * @throws IllegalArgumentException 如果参数非法或数据不足
     */
    public void readBytes(byte[] outputBuffer, int targetOffset, int readLength) {
        if (outputBuffer == null || targetOffset < 0 || readLength < 0 ||
                targetOffset + readLength > outputBuffer.length) {
            throw new IllegalArgumentException("读取参数非法");
        }
        if (readLength > storedDataSize) {
            throw new IllegalArgumentException("读取长度超过缓冲区数据量");
        }

        performReadOperation(outputBuffer, targetOffset, readLength);
    }

    /**
     * 查看但不移除指定长度数据（复制副本）
     *
     * @param peekLength 要查看的长度
     * @return 数据副本
     */
    public byte[] peekBytes(int peekLength) {
        byte[] result = new byte[peekLength];
        peekBytes(result);
        return result;
    }

    /**
     * 查看但不移除数据到指定数组
     * @param outputBuffer  输出缓冲区
     */
    public void peekBytes(byte[] outputBuffer) {
        peekBytes(outputBuffer, 0, outputBuffer.length);
    }

    /**
     * 查看但不移除数据到指定数组位置
     *
     * @param outputBuffer   输出缓冲区
     * @param targetOffset   目标偏移量
     * @param peekLength     查看长度
     * @throws IllegalArgumentException 如果参数非法或数据不足
     */
    public void peekBytes(byte[] outputBuffer, int targetOffset, int peekLength) {
        if (outputBuffer == null || targetOffset < 0 || peekLength < 0 ||
                targetOffset + peekLength > outputBuffer.length) {
            throw new IllegalArgumentException("peek操作参数非法");
        }
        if (peekLength > storedDataSize) {
            throw new IllegalArgumentException("查看长度超过缓冲区数据量");
        }

        performDataCopy(outputBuffer, targetOffset, peekLength);
    }

    /**
     * 跳过指定字节数
     *
     * @param skipLength 要跳过的字节数
     * @throws IllegalArgumentException 如果跳过长度超过数据量
     */
    public void skipBytes(int skipLength) {
        if (skipLength > storedDataSize) {
            throw new IllegalArgumentException("跳过长度超过缓冲区数据量");
        }
        readPosition = (readPosition + skipLength) % bufferCapacity;
        storedDataSize -= skipLength;
    }

    /**
     * 获取当前数据量
     *
     * @return 已存储数据大小
     */
    public int getCurrentSize() {
        return storedDataSize;
    }

    /**
     * 获取可用空间大小
     *
     * @return 剩余可写空间
     */
    public int getAvailableSpace() {
        return bufferCapacity - storedDataSize;
    }

    /**
     * 获取缓冲区总容量
     *
     * @return 缓冲区容量
     */
    public int getCapacity() {
        return bufferCapacity;
    }

    /**
     * 检查缓冲区是否为空
     *
     * @return 是否为空
     */
    public boolean isEmpty() {
        return storedDataSize == 0;
    }

    /**
     * 检查缓冲区是否已满
     *
     * @return 是否已满
     */
    public boolean isFull() {
        return storedDataSize == bufferCapacity;
    }

    /**
     * 清空缓冲区
     */
    public void clear() {
        readPosition = writePosition = storedDataSize = 0;
    }

    /**
     * 执行数据写入操作
     */
    private void performWriteOperation(byte[] sourceData, int sourceOffset, int writeLength) {
        // 计算尾部连续空间
        int tailSpace = bufferCapacity - writePosition;
        int firstSegment = Math.min(writeLength, tailSpace);

        // 写入第一部分数据
        System.arraycopy(sourceData, sourceOffset, dataStore, writePosition, firstSegment);

        // 处理回绕写入
        int remainingSegment = writeLength - firstSegment;
        if (remainingSegment > 0) {
            System.arraycopy(sourceData, sourceOffset + firstSegment, dataStore, 0, remainingSegment);
        }

        // 更新写入位置和数据量
        writePosition = (writePosition + writeLength) % bufferCapacity;
        storedDataSize += writeLength;
    }

    /**
     * 执行数据读取操作（会移动读指针）
     */
    private void performReadOperation(byte[] destination, int targetOffset, int readLength) {
        // 先复制数据
        performDataCopy(destination, targetOffset, readLength);

        // 更新读取位置和数据量
        readPosition = (readPosition + readLength) % bufferCapacity;
        storedDataSize -= readLength;
    }

    /**
     * 执行数据复制（不移动指针）
     */
    private void performDataCopy(byte[] destination, int targetOffset, int copyLength) {
        // 计算头部连续数据量
        int headContiguous = Math.min(copyLength, bufferCapacity - readPosition);

        // 复制第一部分数据
        System.arraycopy(dataStore, readPosition, destination, targetOffset, headContiguous);

        // 处理回绕数据
        int remainingSegment = copyLength - headContiguous;
        if (remainingSegment > 0) {
            System.arraycopy(dataStore, 0, destination, targetOffset + headContiguous, remainingSegment);
        }
    }
}
