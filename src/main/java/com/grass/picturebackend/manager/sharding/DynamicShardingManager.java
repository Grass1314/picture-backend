package com.grass.picturebackend.manager.sharding;

import com.baomidou.mybatisplus.extension.toolkit.SqlRunner;
import com.grass.picturebackend.model.entity.Space;
import com.grass.picturebackend.model.enums.SpaceLevelEnum;
import com.grass.picturebackend.model.enums.SpaceTypeEnum;
import com.grass.picturebackend.service.SpaceService;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.driver.jdbc.core.connection.ShardingSphereConnection;
import org.apache.shardingsphere.infra.metadata.database.rule.ShardingSphereRuleMetaData;
import org.apache.shardingsphere.mode.manager.ContextManager;
import org.apache.shardingsphere.sharding.api.config.ShardingRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.rule.ShardingTableRuleConfiguration;
import org.apache.shardingsphere.sharding.rule.ShardingRule;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Mr.Liuxq
 * @description: 分表管理器
 * @date 2025年05月14日 09:28
 */
//@Component
@Slf4j
public class DynamicShardingManager {

    @Resource
    private DataSource dataSource;

    @Resource
    private SpaceService spaceService;

    private static final String LOGIC_TABLE_NAME = "picture";

    private static final String DATABASE_NAME = "grass_picture"; // 配置文件中的数据库名称

    @PostConstruct
    public void initialize() {
        log.info("初始化动态分表配置...");
        updateShardingTableNodes();
    }

    /**
     * @description: 获取所有picture表名，包括初始表picture  和所有picture_spaceId表
     * @author: Mr.Liuxq
     * @date 2025/5/14 09:41
     * @return java.util.Set<java.lang.String>
     */
    private Set<String> fetchAllPictureTableNames() {
        // 获取所有旗舰版的团队空间
        Set<Long> spaceIds = spaceService.lambdaQuery()
                .eq(Space::getSpaceType, SpaceTypeEnum.TEAM.getValue()).eq(Space::getSpaceLevel, SpaceLevelEnum.FLAGSHIP.getValue())
                .list()
                .stream()
                .map(Space::getId)
                .collect(Collectors.toSet());
        Set<String> tableNames = spaceIds.stream()
                .map(spaceId -> LOGIC_TABLE_NAME + "_" + spaceId)
                .collect(Collectors.toSet());
        tableNames.add(LOGIC_TABLE_NAME);
        return tableNames;
    }

    /**
     * @description: 更新分表节点 更新 ShardingSphere 的 actual-data-nodes 动态表名配置
     * @author: Mr.Liuxq
     * @date 2025/5/14 09:41
     */
    private void updateShardingTableNodes() {
        Set<String> tableNames = fetchAllPictureTableNames();
        // 拼接合法前缀
        String newActualDataNodes = tableNames.stream().map(tableName -> "grass_picture." + tableName).collect(Collectors.joining(","));
        log.info("动态分表 actual-data-nodes 配置：{}", newActualDataNodes);
        ContextManager contextManager = getContextManager();
        ShardingSphereRuleMetaData ruleMetaData = contextManager.getMetaDataContexts()
                .getMetaData()
                .getDatabases()
                .get(DATABASE_NAME)
                .getRuleMetaData();
        Optional<ShardingRule> shardingRule = ruleMetaData.findSingleRule(ShardingRule.class);
        if (shardingRule.isPresent()) {
            ShardingRuleConfiguration ruleConfig = (ShardingRuleConfiguration) shardingRule.get().getConfiguration();
            List<ShardingTableRuleConfiguration> updateRules = ruleConfig.getTables().stream().map(oldTableRule -> {
                if (LOGIC_TABLE_NAME.equals(oldTableRule.getLogicTable())) {
                    ShardingTableRuleConfiguration newTableRule = new ShardingTableRuleConfiguration(LOGIC_TABLE_NAME, newActualDataNodes);
                    newTableRule.setDatabaseShardingStrategy(oldTableRule.getDatabaseShardingStrategy());
                    newTableRule.setTableShardingStrategy(oldTableRule.getTableShardingStrategy());
                    newTableRule.setKeyGenerateStrategy(oldTableRule.getKeyGenerateStrategy());
                    newTableRule.setAuditStrategy(oldTableRule.getAuditStrategy());
                    return newTableRule;
                }
                return oldTableRule;
            }).collect(Collectors.toList());
            ruleConfig.setTables(updateRules);
            contextManager.alterRuleConfiguration(DATABASE_NAME, Collections.singleton(ruleConfig));
            contextManager.reloadDatabase(DATABASE_NAME);
            log.info("动态分表配置更新成功！");
        } else {
            log.error("未找到分片规则，请检查分片规则配置是否正确！");
        }
    }

    public void createSpacePictureTable(Space space) {
        // 仅对旗舰版团队创建分表
        if (space.getSpaceType() == SpaceTypeEnum.TEAM.getValue() && space.getSpaceLevel() == SpaceLevelEnum.FLAGSHIP.getValue()) {
            Long spaceId = space.getId();
            String tableName = LOGIC_TABLE_NAME + "_" + spaceId;
            // 创建新表
            try {
                String sql = "CREATE TABLE IF NOT EXISTS `" + tableName + "` LIKE `" + LOGIC_TABLE_NAME + "`";
                SqlRunner.db().update(sql);
                // 更新分表节点
                updateShardingTableNodes();
                log.info("创建分表成功：{}", tableName);
            } catch (Exception e) {
                log.error("创建图片空间分表失败：{}", tableName, e);
            }
        }
    }

    /**
     * 获取 ShardingSphere ContextManager
     */
    private ContextManager getContextManager() {
        try (ShardingSphereConnection connection = dataSource.getConnection().unwrap(ShardingSphereConnection.class)) {
            return connection.getContextManager();
        } catch (SQLException e) {
            throw new RuntimeException("获取 ShardingSphere ContextManager 失败", e);
        }
    }

}
