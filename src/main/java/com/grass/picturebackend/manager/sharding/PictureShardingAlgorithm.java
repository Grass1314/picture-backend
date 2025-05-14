package com.grass.picturebackend.manager.sharding;

import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;

import java.util.Collection;
import java.util.Collections;
import java.util.Properties;

/**
 * @author Mr.Liuxq
 * @description: 图片空间分库分表
 * @date 2025年05月14日 09:13
 */

public class PictureShardingAlgorithm implements StandardShardingAlgorithm<Long> {
    /**
     * @param availableTargetNames
     * @param preciseShardingValue
     * @return
     */
    @Override
    public String doSharding(Collection<String> availableTargetNames, PreciseShardingValue<Long> preciseShardingValue) {
        Long spaceId = preciseShardingValue.getValue();
        String logicTableName = preciseShardingValue.getLogicTableName();
        // spaceId 为null 表示查询所有图片
        if (spaceId == null) {
            return logicTableName;
        }
        // 根据spaceID分表
        String realTableName = "picture_" + spaceId;
        if (availableTargetNames.contains(realTableName)) {
            return realTableName;
        } else {
            return logicTableName;
        }
    }

    /**
     * @param collection
     * @param rangeShardingValue
     * @return
     */
    @Override
    public Collection<String> doSharding(Collection<String> collection, RangeShardingValue<Long> rangeShardingValue) {
        return Collections.emptyList();
    }

    /**
     * @return
     */
    @Override
    public Properties getProps() {
        return null;
    }

    /**
     * @param properties
     */
    @Override
    public void init(Properties properties) {

    }
}
