package com.ruoyi.xiaozhi.feign.core;

import feign.FeignException;
import feign.Response;
import feign.Types;
import feign.codec.Decoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * 自定义feign解码器（统一解析响应）
 * @author ruoyi-xiaozhi
 */
@Slf4j
@RequiredArgsConstructor
public class FeignDecoder implements Decoder {

    private final Decoder decoder;

    @Override
    public Object decode(Response response, Type type) throws FeignException, IOException {
        if (this.isVoidType(type)) {
            type = Void.class;
        }
        // 响应的原始类型
        ParameterizedType originType = Types.parameterize(R.class, type);
        // 转换类型
        R<?> result = (R<?>) this.decoder.decode(response, originType);
        // 判断响应结果
        if (!result.isSuccess()) {
            log.error("[FeignDecoder] 响应失败：{}", result.getMsg());
            throw new FeignCallException(result.getMsg());
        }
        return result.getData();
    }

    /**
     * 判断返回类型是否为void
     */
    private boolean isVoidType(Type returnType) {
        return returnType == Void.class
                || returnType == void.class
                || returnType.getTypeName().equals("kotlin.Unit");
    }


}
