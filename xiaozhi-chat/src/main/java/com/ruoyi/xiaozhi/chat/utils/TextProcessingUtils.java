package com.ruoyi.xiaozhi.chat.utils;

import cn.hutool.core.text.CharSequenceUtil;

import java.util.HashSet;
import java.util.Set;

/**
 * 文本处理工具类
 * @author ruoyi-xiaozhi
 */
public final class TextProcessingUtils {

    /** 中文标点符号集合 */
    private static final Set<Character> CHINESE_PUNCTUATION_SET = new HashSet<>();

    /** Unicode分隔符代码点集合 */
    private static final Set<Integer> SEPARATOR_CODE_POINTS = new HashSet<>();

    static {
        // 初始化中文标点符号集合
        char[] chinesePunctuations = {'。', '？', '！', '；', '：'};
        for (char c : chinesePunctuations) {
            CHINESE_PUNCTUATION_SET.add(c);
        }

        // 初始化分隔符代码点集合
        char[] separators = {'，', ',', '。', '.', '！', '!', '-', '－', '、'};
        for (char c : separators) {
            SEPARATOR_CODE_POINTS.add((int) c);
        }
    }

    /**
     * 移除所有标点符号
     * @param inputText  输入文本
     * @return  处理后的文本
     */
    public static String removeAllPunctuationMarks(String inputText) {
        if (CharSequenceUtil.isEmpty(inputText)) {
            return CharSequenceUtil.EMPTY;
        }

        // 全角符号集合
        String fullWidthPunctuations = "！＂＃＄％＆＇（）＊＋，－。／：；＜＝＞？＠［＼］＾＿｀｛｜｝～";
        // 半角符号集合
        String halfWidthPunctuations = "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~";
        // 空格字符
        String processedText = removePunctuation(inputText, fullWidthPunctuations, halfWidthPunctuations);

        // 特殊处理：排除特定字符串
        if ("Yeah".equals(processedText)) {
            return CharSequenceUtil.EMPTY;
        }
        return processedText;
    }

    private static String removePunctuation(String inputText, String fullWidthPunctuations, String halfWidthPunctuations) {
        char halfWidthSpace = ' ';     // 半角空格
        char fullWidthSpace = '　';    // 全角空格

        StringBuilder cleanedTextBuilder = new StringBuilder();
        for (char currentChar : inputText.toCharArray()) {
            // 检查字符是否不属于任何标点符号集合
            if (fullWidthPunctuations.indexOf(currentChar) == -1 &&
                    halfWidthPunctuations.indexOf(currentChar) == -1 &&
                    currentChar != halfWidthSpace &&
                    currentChar != fullWidthSpace) {
                cleanedTextBuilder.append(currentChar);
            }
        }
        return cleanedTextBuilder.toString();
    }

    /**
     * 判断是否为Emoji表情字符
     * @param codePoint  Unicode代码点
     * @return  是否为Emoji
     */
    public static boolean isEmojiCodePoint(int codePoint) {
        // Emoji Unicode范围定义
        int[][] emojiUnicodeRanges = {
                {0x1F600, 0x1F64F},  // Emoticons
                {0x1F300, 0x1F5FF},  // Symbols & Pictographs
                {0x1F680, 0x1F6FF},  // Transport & Map Symbols
                {0x1F900, 0x1F9FF},  // Supplemental Symbols and Pictographs
                {0x1FA70, 0x1FAFF},  // Symbols and Pictographs Extended-A
                {0x2600, 0x26FF},    // Miscellaneous Symbols
                {0x2700, 0x27BF}     // Dingbats
        };

        for (int[] range : emojiUnicodeRanges) {
            if (codePoint >= range[0] && codePoint <= range[1]) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断是否为分隔符字符
     * @param codePoint  Unicode代码点
     * @return  是否为分隔符
     */
    public static boolean isSeparatorCodePoint(int codePoint) {
        return !Character.isWhitespace(codePoint) &&
                !SEPARATOR_CODE_POINTS.contains(Character.getType(codePoint));
    }

    /**
     * 移除所有Emoji并修剪两端的分隔符
     * @param sourceText  原始文本
     * @return  处理后的文本
     */
    public static String removeEmojisAndTrimSeparators(String sourceText) {
        if (sourceText == null || sourceText.isEmpty()) {
            return sourceText;
        }

        // 1. 移除所有Emoji表情
        StringBuilder emojiFreeBuilder = new StringBuilder();
        for (int index = 0; index < sourceText.length(); ) {
            int codePoint = sourceText.codePointAt(index);
            if (!isEmojiCodePoint(codePoint)) {
                emojiFreeBuilder.appendCodePoint(codePoint);
            }
            index += Character.charCount(codePoint);
        }

        String emojiFreeText = emojiFreeBuilder.toString();

        // 2. 修剪两端的标点符号
        int startIndex = 0;
        int endIndex = emojiFreeText.length();

        // 从开头查找第一个非分隔符
        while (startIndex < endIndex) {
            int codePoint = emojiFreeText.codePointAt(startIndex);
            if (isSeparatorCodePoint(codePoint)) {
                break;
            }
            startIndex += Character.charCount(codePoint);
        }

        // 从结尾查找最后一个非分隔符
        while (endIndex > startIndex) {
            int codePoint = emojiFreeText.codePointBefore(endIndex);
            if (isSeparatorCodePoint(codePoint)) {
                break;
            }
            endIndex -= Character.charCount(codePoint);
        }

        return emojiFreeText.substring(startIndex, endIndex);
    }

    /**
     * 查找最后一个中文标点符号的位置
     * @param textContent  文本内容
     * @return  最后一个中文标点符号的索引（未找到返回-1）
     */
    public static int findLastChinesePunctuationIndex(String textContent) {
        char[] characters = textContent.toCharArray();
        for (int i = characters.length - 1; i >= 0; i--) {
            if (CHINESE_PUNCTUATION_SET.contains(characters[i])) {
                return i;
            }
        }
        return -1;
    }
}
