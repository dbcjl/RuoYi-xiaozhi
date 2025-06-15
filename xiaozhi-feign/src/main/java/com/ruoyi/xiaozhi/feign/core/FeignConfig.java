package com.ruoyi.xiaozhi.feign.core;

import feign.Feign;
import feign.codec.Decoder;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.context.annotation.Bean;

/**
 * Feign配置
 * @author ruoyi-xiaozhi
 */
public class FeignConfig {

    /**
     * 自定义解码器
     * @param messageConverters  消息转换器
     * @return  Feign解码器
     */
    @Bean
    public Decoder decoder(ObjectFactory<HttpMessageConverters> messageConverters) {
        return new FeignDecoder(new SpringDecoder(messageConverters));
    }

    /**
     * 配置FeignBuilder，配置解码void的方法，默认是不解码的
     */
    @Bean
    public Feign.Builder feignBuilderDecodeVoid() {
        return Feign.builder().decodeVoid();
    }

}
