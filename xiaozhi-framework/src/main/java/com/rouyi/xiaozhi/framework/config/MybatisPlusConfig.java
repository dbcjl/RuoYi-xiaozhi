package com.rouyi.xiaozhi.framework.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Mybatis Plus 配置
 *
 * @author ruoyi
 */
@EnableTransactionManagement(proxyTargetClass = true)
@Configuration
public class MybatisPlusConfig
{
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor()
    {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 阻断插件
        interceptor.addInnerInterceptor(blockAttackInnerInterceptor());
        return interceptor;
    }

    /**
     * 如果是对全表的删除或更新操作，就会终止该操作 <a href="https://baomidou.com/guide/interceptor-block-attack.html">文档链接</a>
     */
    public BlockAttackInnerInterceptor blockAttackInnerInterceptor()
    {
        return new BlockAttackInnerInterceptor();
    }
}
