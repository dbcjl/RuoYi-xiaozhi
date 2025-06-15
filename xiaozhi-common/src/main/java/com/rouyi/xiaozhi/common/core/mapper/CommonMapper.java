package com.rouyi.xiaozhi.common.core.mapper;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ReflectUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.mapper.Mapper;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.ReflectionKit;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Function;

/**
 * 公用的Mapper
 * @author ruoyi-xiaozhi
 */
public interface CommonMapper<T> extends BaseMapper<T> {

    /**
     * 根据id查询map映射
     * @param idList    id列表
     * @return  map映射信息
     * @param <K>   主键的类型
     */
    default <K extends Serializable> Map<K, T> mapRecordsByIds(Collection<K> idList) {
        if (CollUtil.isEmpty(idList)) {
            return Collections.emptyMap();
        }
        // 获取实体类
        Class<?> modelClass = ReflectionKit.getSuperClassGenericType(this.getClass(), Mapper.class, 0);
        // 获取表信息
        TableInfo tableInfo = TableInfoHelper.getTableInfo(modelClass);
        // 构造查询条件
        QueryWrapper<T> query = Wrappers.query();
        // 根据主键查询
        query.in(tableInfo.getKeyColumn(), idList);
        return this.selectAsMap(query, t -> {
            // 反射获取主键的值并转换为主键的类型
            Object fieldValue = ReflectUtil.getFieldValue(t, tableInfo.getKeyProperty());
            return Convert.convert((Type) tableInfo.getKeyType(), fieldValue);
        });
    }

    /**
     * 将查询的列表根据keyMapper获取的key值封装成Map
     * <p>相同key的数据只会保留 1 条
     *
     * @param wrapper   查询条件
     * @param keyMapper Key的映射函数
     * @return  映射结果
     * @param <K>   Key的类型
     */
    default <K> Map<K, T> selectAsMap(Wrapper<T> wrapper, Function<? super T, ? extends K> keyMapper) {
        return this.selectAsMap(wrapper, keyMapper, null);
    }

    /**
     * 将查询的列表根据keyMapper获取的key值封装成Map
     * <p>相同key的数据只会保留 1 条
     * <p>可以根据comparator选择性保留哪一条
     *
     * @param wrapper   查询条件
     * @param keyMapper Key的映射函数
     * @param comparator 覆盖比较器，返回结果大于0就覆盖前面的
     * @return  映射结果
     * @param <K>   Key的类型
     */
    default <K> Map<K, T> selectAsMap(Wrapper<T> wrapper, Function<? super T, ? extends K> keyMapper, Comparator<? super T> comparator) {
        // 存储结果
        Map<K, T> result = new HashMap<>();
        // 使用自定义结果处理器进行流式查询
        this.selectList(wrapper, resultContext -> {
            // 获取当前行对象
            T row = resultContext.getResultObject();
            // 提取对应的key值
            K key = keyMapper.apply(row);
            // 如果之前没有相同key的数据，就直接覆盖，如果有，根据比较器返回结果选择性覆盖
            T oldValue = result.get(key);
            if (oldValue == null) {
                result.put(key, row);
            }else if (comparator != null && comparator.compare(oldValue, row) > 0){
                result.put(key, row);
            }
        });
        // 返回结果
        return result;
    }

    /**
     * 将查询的列表根据keyMapper获取的key值封装成Map
     * <p>相同Key的数据会放到一个list
     *
     * @param wrapper   查询条件
     * @param keyMapper Key的映射函数
     * @return  映射结果
     * @param <K>   Key的类型
     */
    default <K> Map<K, List<T>> selectAsListMap(Wrapper<T> wrapper, Function<? super T, ? extends K> keyMapper) {
        // 存储结果
        Map<K, List<T>> result = new HashMap<>();
        // 使用自定义结果处理器进行流式查询
        this.selectList(wrapper, resultContext -> {
            // 获取当前行对象
            T row = resultContext.getResultObject();
            // 提取对应的key值
            K key = keyMapper.apply(row);
            // 加入对应的列表中
            List<T> rows = result.computeIfAbsent(key, k -> new ArrayList<>());
            rows.add(row);
        });
        // 返回结果
        return result;
    }

}
