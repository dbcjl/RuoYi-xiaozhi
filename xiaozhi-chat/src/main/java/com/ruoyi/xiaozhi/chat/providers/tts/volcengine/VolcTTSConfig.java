package com.ruoyi.xiaozhi.chat.providers.tts.volcengine;

import com.ruoyi.xiaozhi.chat.properties.VolcTTSProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 火山引擎TTS配置
 * @author ruoyi-xiaozhi
 */
@Slf4j
@Configuration
public class VolcTTSConfig {

    public static final String RESOURCE_ID = "volc.service_type.10029";

    /**
     * 获取TTS客户端连接池
     * @return TTS客户端连接池
     */
    @Bean(destroyMethod = "close")
    public GenericObjectPool<VolcTTSClient> volcTTSClientPool(VolcTTSProperties properties) {
        GenericObjectPoolConfig<VolcTTSClient> config = new GenericObjectPoolConfig<>();
        config.setMaxTotal(10);
        config.setMinIdle(1);
        config.setJmxEnabled(false);
        // 获取连接时进行测试
        config.setTestOnBorrow(true);
        return new GenericObjectPool<>(new VolcTTSClientFactory(properties), config);
    }

    /**
     * TTS客户端工厂
     */
    public static class VolcTTSClientFactory implements PooledObjectFactory<VolcTTSClient> {

        private final VolcTTSProperties properties;

        public VolcTTSClientFactory(VolcTTSProperties properties) {
            this.properties = properties;
        }

        @Override
        public void activateObject(PooledObject<VolcTTSClient> p) {

        }

        @Override
        public void destroyObject(PooledObject<VolcTTSClient> p) {
            p.getObject().close();
        }

        @Override
        public PooledObject<VolcTTSClient> makeObject() throws InterruptedException {
            VolcTTSClient client = VolcTTSClient.builder()
                    .appid(properties.getAppid())
                    .accessToken(properties.getAccessToken())
                    .resourceId(RESOURCE_ID)
                    .build();
            // 建立websocket连接
            client.connectBlocking();
            return new DefaultPooledObject<>(client);
        }

        @Override
        public void passivateObject(PooledObject<VolcTTSClient> p) {

        }

        @Override
        public boolean validateObject(PooledObject<VolcTTSClient> p) {
            boolean ttsClientOpen = p.getObject().isOpen();
            log.info("validateObject, ttsClientOpen: {}", ttsClientOpen);
            return ttsClientOpen;
        }
    }

}
