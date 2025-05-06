package com.grass.picturebackend.controller;

import cn.hutool.core.bean.BeanUtil;
import com.grass.picturebackend.annotation.AuthCheck;
import com.grass.picturebackend.common.BaseResponse;
import com.grass.picturebackend.common.ResultUtils;
import com.grass.picturebackend.constant.UserConstant;
import com.grass.picturebackend.exception.BusinessException;
import com.grass.picturebackend.exception.ErrorCode;
import com.grass.picturebackend.exception.ThrowUtils;
import com.grass.picturebackend.model.dto.space.SpaceLevel;
import com.grass.picturebackend.model.dto.space.SpaceUpdateRequest;
import com.grass.picturebackend.model.entity.Space;
import com.grass.picturebackend.model.enums.SpaceLevelEnum;
import com.grass.picturebackend.service.SpaceService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Mr.Liuxq
 * @description: 空间控制器
 * @date 2025年05月06日 09:11
 */

@RestController
@RequestMapping("/space")
public class SpaceController {

    @Resource
    private SpaceService spaceService;

    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateSpace(@RequestBody SpaceUpdateRequest spaceUpdateRequest) {
        if (spaceUpdateRequest == null || spaceUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Space space = new Space();
        BeanUtil.copyProperties(spaceUpdateRequest, space);
        // 自动填充数据
        spaceService.fillSpaceBySpaceLevel(space);
        // 数据校验
        spaceService.validSpace(space, false);
        // 判断是否存在
        Long id = spaceUpdateRequest.getId();
        Space oldSpace = spaceService.getById(id);
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);
        // 修改
        boolean result = spaceService.updateById(space);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @GetMapping("/list/level")
    public BaseResponse<List<SpaceLevel>> listSpaceLevel() {
        List<SpaceLevel> spaceLevels = Arrays.stream(SpaceLevelEnum.values())
                .map(item -> new SpaceLevel(item.getValue(), item.getText(), item.getMaxCount(), item.getMaxSize()))
                .collect(Collectors.toList());
        return ResultUtils.success(spaceLevels);
    }
}
