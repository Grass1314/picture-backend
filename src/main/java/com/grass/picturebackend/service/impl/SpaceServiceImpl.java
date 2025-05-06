package com.grass.picturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.grass.picturebackend.exception.BusinessException;
import com.grass.picturebackend.exception.ErrorCode;
import com.grass.picturebackend.exception.ThrowUtils;
import com.grass.picturebackend.mapper.SpaceMapper;
import com.grass.picturebackend.model.dto.space.SpaceAddRequest;
import com.grass.picturebackend.model.entity.Space;
import com.grass.picturebackend.model.entity.User;
import com.grass.picturebackend.model.enums.SpaceLevelEnum;
import com.grass.picturebackend.service.SpaceService;
import com.grass.picturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Mr.Liuxq
 * @description: 空间业务层
 * @date 2025年05月06日 09:13
 */
@Service
@Slf4j
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space> implements SpaceService {

    @Resource
    private UserService userService;

    @Resource
    private TransactionTemplate  transactionTemplate;

    Map<Long, Object> lockMap = new ConcurrentHashMap<>();

    /**
     * 校验
     *
     * @param space 空间PO
     * @param add   是否新增
     */
    @Override
    public void validSpace(Space space, boolean add) {
        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR);

        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        // 创建
        if (add) {
            ThrowUtils.throwIf(StrUtil.isBlank(spaceName), ErrorCode.PARAMS_ERROR, "空间名称不能为空");
            ThrowUtils.throwIf(spaceLevel == null, ErrorCode.PARAMS_ERROR, "空间等级不能为空");
        }
        // 修改数据是判断要修改的空间级别
        if (spaceLevel != null && spaceLevelEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不存在");
        }
        if (StrUtil.isNotBlank(spaceName) && spaceName.length() > 30) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称过长");
        }
    }

    /**
     * 填充空间信息
     *
     * @param space 空间PO
     */
    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        // 根据空间级别自动填充限额
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
        if (spaceLevelEnum != null) {
            long maxSize = spaceLevelEnum.getMaxSize();
            if (space.getMaxSize() == null) {
                space.setMaxSize(maxSize);
            }
            long maxCount = spaceLevelEnum.getMaxCount();
            if (space.getMaxCount() == null) {
                space.setMaxCount(maxCount);
            }
        }
    }

    /**
     * 添加空间
     *
     * @param spaceAddRequest 空间添加请求
     * @param loginUser       登录用户
     * @return 空间id
     */
    @Override
    public Long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
        Space space = new Space();
        BeanUtil.copyProperties(spaceAddRequest, space);
        // 默认空间名称
        if (StrUtil.isBlank(space.getSpaceName())) {
            space.setSpaceName("默认空间");
        }
        if (space.getSpaceLevel() == null) {
            space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
        // 填充数据
        this.fillSpaceBySpaceLevel(space);
        // 校验
        this.validSpace(space, true);
        Long userId = loginUser.getId();
        space.setUserId(userId);
        // 权限校验
        if (SpaceLevelEnum.COMMON.getValue() != spaceAddRequest.getSpaceLevel() && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限创建指定级别的空间");
        }
        // 通过用户进行加锁
//        String lock = String.valueOf(userId).intern();
        Object lock = lockMap.computeIfAbsent(userId, k -> new Object());
        synchronized (lock) {
            try {
                Long newSpaceId = transactionTemplate.execute(status -> {
                    boolean exists = this.lambdaQuery().eq(Space::getUserId, userId).exists();
                    ThrowUtils.throwIf(exists, ErrorCode.OPERATION_ERROR, "每个用户只能创建一个私有空间");
                    //  插入数据库
                    boolean result = this.save(space);
                    ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
                    return space.getId();
                });
                return Optional.ofNullable(newSpaceId).orElse(-1L);
            } finally {
                // 防止内存泄漏
                lockMap.remove(userId);
            }
        }
    }
}
