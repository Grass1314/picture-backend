package com.grass.picturebackend.controller;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.grass.picturebackend.common.BaseResponse;
import com.grass.picturebackend.common.DeleteRequest;
import com.grass.picturebackend.common.ResultUtils;
import com.grass.picturebackend.exception.BusinessException;
import com.grass.picturebackend.exception.ErrorCode;
import com.grass.picturebackend.exception.ThrowUtils;
import com.grass.picturebackend.manager.auth.annotation.SaSpaceCheckPermission;
import com.grass.picturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.grass.picturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.grass.picturebackend.model.dto.spaceuser.SpaceUserEditRequest;
import com.grass.picturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.grass.picturebackend.model.entity.SpaceUser;
import com.grass.picturebackend.model.entity.User;
import com.grass.picturebackend.model.vo.SpaceUserVO;
import com.grass.picturebackend.service.SpaceUserService;
import com.grass.picturebackend.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author Mr.Liuxq
 * @description: 空间成员控制器
 * @date 2025年05月12日 15:49
 */
@RestController
@RequestMapping("/spaceUser")
public class SpaceUserController {

    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    private UserService userService;

    /**
     * 添加空间成员
     * @param spaceUserAddRequest 添加空间成员请求
     * @return 空间成员id
     * @author: Mr.Grass
     */
    @RequestMapping("/add")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Long> addSpaceUser(@RequestBody SpaceUserAddRequest spaceUserAddRequest) {
        ThrowUtils.throwIf(spaceUserAddRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = spaceUserService.addSpaceUser(spaceUserAddRequest);
        return ResultUtils.success(id);
    }

    /**
     * 删除空间成员
     * @param deleteRequest 删除空间成员请求
     * @return 删除结果
     * @author: Mr.Grass
     */
    @RequestMapping("/delete")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Boolean> deleteSpaceUser(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 判断空间用户是否存在
        SpaceUser oldSpaceUser = spaceUserService.getById(deleteRequest.getId());
        ThrowUtils.throwIf(oldSpaceUser == null, ErrorCode.NOT_FOUND_ERROR);
        User loginUser = userService.getLoginUser(request);
        // 管理员可以删除所有空间成员、否则只能删除自己的空间成员
        if (!userService.isAdmin(loginUser) && !oldSpaceUser.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"没有删除权限");
        }
        boolean result = spaceUserService.removeById(deleteRequest.getId());
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 查询某个成员在某个空间的信息
     * @param spaceUserQueryRequest 空间成员查询请求
     * @return 空间成员
     * @author: Mr.Grass
     */
    @PostMapping("/get")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<SpaceUser> getSpaceUser(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUserQueryRequest == null, ErrorCode.PARAMS_ERROR);
        SpaceUser spaceUser = spaceUserService.getOne(spaceUserService.getQueryWrapper(spaceUserQueryRequest));
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(spaceUser);
    }

    /**
     * 查询成员信息列表
     * @param spaceUserQueryRequest 空间成员查询请求
     * @return 空间成员列表
     * @author: Mr.Grass
     */
    @PostMapping("/list")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<List<SpaceUserVO>> listSpaceUser(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUserQueryRequest == null, ErrorCode.PARAMS_ERROR);
        List<SpaceUserVO> spaceUserVOList = spaceUserService.getSpaceUserVOList(spaceUserService.list(spaceUserService.getQueryWrapper(spaceUserQueryRequest)));
        return ResultUtils.success(spaceUserVOList);
    }

    /**
     * 编辑空间成员
     * @param spaceUserEditRequest 编辑空间成员请求
     * @return 编辑结果
     * @author: Mr.Grass
     */
    @PostMapping("/edit")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Boolean> editSpaceUser(@RequestBody SpaceUserEditRequest spaceUserEditRequest, HttpServletRequest request) {
        if (spaceUserEditRequest == null || spaceUserEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 判断是否存在
        SpaceUser oldSpaceUser = spaceUserService.getById(spaceUserEditRequest.getId());
        ThrowUtils.throwIf(oldSpaceUser == null, ErrorCode.NOT_FOUND_ERROR);

        User loginUser = userService.getLoginUser(request);
        // 管理员可以编辑所有空间成员、否则只能编辑自己的空间成员
        if (!userService.isAdmin(loginUser) && !oldSpaceUser.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        SpaceUser spaceUser = new SpaceUser();
        BeanUtil.copyProperties(spaceUserEditRequest, spaceUser);
        // 数据校验
        spaceUserService.validSpaceUser(spaceUser, false);
        // 判断编辑前后的角色是否一致，如果一致就不更新
        if (oldSpaceUser.getSpaceRole().equals(spaceUser.getSpaceRole())) {
            return ResultUtils.success(true);
        }
        // 更新
        boolean result = spaceUserService.updateById(spaceUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 获取当前用户加入的团队空间列表
     * @param request 请求
     * @return 团队空间列表
     * @author: Mr.Grass
     */
    @PostMapping("/list/my")
    public BaseResponse<List<SpaceUserVO>> listMySpaceUser(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        List<SpaceUserVO> spaceUserVOList = spaceUserService.getSpaceUserVOList(spaceUserService.list(new QueryWrapper<SpaceUser>().eq("userId", loginUser.getId())));
        return ResultUtils.success(spaceUserVOList);
    }

    /**
     * 获取用户待确认的团队空间列表
     * @param userId 用户id
     * @return 团队空间列表
     * @author: Mr.Grass
     */
    @GetMapping("/list/invitation/confirm")
    public BaseResponse<List<SpaceUserVO>> listInvitationConfirm(Long userId) {
        List<SpaceUserVO> spaceUserVOList = spaceUserService.listInvitationConfirm(userId);
        return ResultUtils.success(spaceUserVOList);
    }

    /**
     * 审批用户加入团队空间
     * @param userId 用户id
     * @param spaceId 团队空间id
     * @param status 审批状态
     * @return 审批结果
     * @author: Mr.Grass
     */
    @PostMapping("/approve")
    public BaseResponse<Boolean> approveInvitationInfo(@RequestParam Long userId, @RequestParam Long spaceId, @RequestParam Integer status) {
        spaceUserService.approveInvitationInfo(userId, spaceId, status);
        return ResultUtils.success(true);
    }
}
