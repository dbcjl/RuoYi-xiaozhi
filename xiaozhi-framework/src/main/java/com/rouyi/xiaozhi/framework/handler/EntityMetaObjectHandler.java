package com.rouyi.xiaozhi.framework.handler;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.rouyi.xiaozhi.common.utils.SecurityUtils;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 自定义填充公共字段
 * @author ruoyi-xiaozhi
 */
@Slf4j
@Component
public class EntityMetaObjectHandler implements MetaObjectHandler {

    @PostConstruct
    public void init() {
        log.info("load entityMetaObjectHandler init");
    }

    @Override
    public void insertFill(MetaObject metaObject) {
        log.info("start insert fill ....");
        this.strictInsertFill(metaObject, "createBy", SecurityUtils::getUsername, String.class);
        this.strictInsertFill(metaObject, "createTime", Date.class, DateUtil.date());
        this.strictInsertFill(metaObject, "updateBy", SecurityUtils::getUsername, String.class);
        this.strictInsertFill(metaObject, "updateTime", Date.class, DateUtil.date());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateBy", SecurityUtils::getUsername, String.class);
        this.strictUpdateFill(metaObject, "updateTime", Date.class, DateUtil.date());
    }

}
