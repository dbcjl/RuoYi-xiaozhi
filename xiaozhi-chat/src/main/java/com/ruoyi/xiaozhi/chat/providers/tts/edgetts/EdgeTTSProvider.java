package com.ruoyi.xiaozhi.chat.providers.tts.edgetts;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.ruoyi.xiaozhi.chat.connect.ChatServerHandler;
import com.ruoyi.xiaozhi.chat.providers.tts.BaseTTSProvider;
import io.github.whitemagic2014.tts.TTS;
import io.github.whitemagic2014.tts.TTSVoice;
import io.github.whitemagic2014.tts.bean.Voice;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Function;

/**
 * edgeTTS 语音合成
 * @author ruoyi-xiaozhi
 */
@Slf4j
public class EdgeTTSProvider extends BaseTTSProvider {

    public EdgeTTSProvider(ChatServerHandler handler) {
        super(handler);
    }

    /**
     * tts语音合成
     *
     * @param text          要进行语音合成的文本
     * @param sampleRate    音频的采样率
     * @param channels      音频的通道数
     * @param frameSizeByte 回调期望的帧长（字节单位）
     * @param callback      音频合成回调（支持流式)，返回false表示中断/结束合成
     */
    @Override
    public void execute(String text, int sampleRate, int channels, int frameSizeByte, Function<byte[], Boolean> callback) {
        if (CharSequenceUtil.isBlank(text)) {
            return;
        }
        String ttsFile = null;
        try {
            log.info("Edge tts start, text: {}", text);
            // 生成临时文件
            ttsFile = this.generateAudioFile(".mp3").getAbsolutePath();
            String filename = ttsFile.substring(0, ttsFile.lastIndexOf("."));
            // 语音合成
            Voice voice = CollUtil.findOne(TTSVoice.provides(), v -> v.getShortName().equals("zh-CN-XiaoyiNeural"));
            ttsFile = new TTS(voice, CharSequenceUtil.trim(text))
                    .findHeadHook()
                    .storage("")
                    .isRateLimited(true) // Set to true to resolve the rate limiting issue in certain regions.
                    .fileName(filename)
                    .overwrite(false)
                    .formatMp3()
                    .trans();
            log.info("Edge tts end, file: {}", ttsFile);
            // 解码音频
            this.decodeAudio(ttsFile, sampleRate, channels, frameSizeByte, callback);
        }finally {
            // 删除文件
            FileUtil.del(ttsFile);
            FileUtil.del(ttsFile + ".vtt");
        }
    }

}
