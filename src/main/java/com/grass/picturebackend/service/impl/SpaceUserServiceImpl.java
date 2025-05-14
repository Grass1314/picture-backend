package com.grass.picturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.grass.picturebackend.exception.BusinessException;
import com.grass.picturebackend.exception.ErrorCode;
import com.grass.picturebackend.exception.ThrowUtils;
import com.grass.picturebackend.mapper.SpaceUserMapper;
import com.grass.picturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.grass.picturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.grass.picturebackend.model.entity.Space;
import com.grass.picturebackend.model.entity.SpaceUser;
import com.grass.picturebackend.model.entity.User;
import com.grass.picturebackend.model.enums.SpaceRoleEnum;
import com.grass.picturebackend.model.vo.SpaceUserVO;
import com.grass.picturebackend.model.vo.SpaceVO;
import com.grass.picturebackend.model.vo.UserVO;
import com.grass.picturebackend.service.SpaceService;
import com.grass.picturebackend.service.SpaceUserService;
import com.grass.picturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Mr.Liuxq
 * @description: 空间用户业务层
 * @date 2025年05月12日 14:56
 */
@Service
@Slf4j
public class SpaceUserServiceImpl extends ServiceImpl<SpaceUserMapper, SpaceUser> implements SpaceUserService {

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    /**
     * 添加空间成员
     *
     * @param spaceUserAddRequest 空间成员添加表单
     * @return 添加后的空间成员id
     */
    @Override
    public Long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest) {
        // 参数校验
        ThrowUtils.throwIf(spaceUserAddRequest == null, ErrorCode.PARAMS_ERROR);
        SpaceUser spaceUser = new SpaceUser();
        BeanUtil.copyProperties(spaceUserAddRequest, spaceUser);
        validSpaceUser(spaceUser, true);

        // 新增
        boolean result = this.save(spaceUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return spaceUser.getId();
    }

    /**
     * 校验空间成员
     *
     * @param spaceUser  空间成员
     * @param add        是否为创建校验
     */
    @Override
    public void validSpaceUser(SpaceUser spaceUser, boolean add) {
        ThrowUtils.throwIf(spaceUser.getSpaceId() == null, ErrorCode.PARAMS_ERROR);

        if (add) {
            // 新增时空间ID、用户ID必填
            ThrowUtils.throwIf(spaceUser.getUserId() == null, ErrorCode.PARAMS_ERROR);
            User user = userService.getById(spaceUser.getUserId());
            ThrowUtils.throwIf(user == null, ErrorCode.PARAMS_ERROR,"用户不存在");
            ThrowUtils.throwIf(spaceUser.getSpaceId() == null, ErrorCode.PARAMS_ERROR);
            Space space = spaceService.getById(spaceUser.getSpaceId());
            ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR,"空间不存在");
            // 校验是否已经添加了成员
            if (spaceUser.getUserId() != null && spaceUser.getSpaceId() != null) {
                // 同一个用户在同一个空间中只能有一个角色
                SpaceUser whetherSpaceUser = this.getOne(new QueryWrapper<SpaceUser>().eq("userId", spaceUser.getUserId()).eq("spaceId", spaceUser.getSpaceId()));
                ThrowUtils.throwIf(whetherSpaceUser != null, ErrorCode.PARAMS_ERROR, "该成员已存在");
            }
        }
        // 校验空间角色
        String spaceRole = spaceUser.getSpaceRole();
        SpaceRoleEnum spaceRoleEnum = SpaceRoleEnum.getEnumByValue(spaceRole);
        if (spaceRole != null && spaceRoleEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间角色不存在");
        }

    }

    /**
     * 空间用户查询条件封装
     *
     * @param spaceUserQueryRequest 空间用户查询请求
     * @return 查询条件封装对象
     * @author: Mr.Grass
     */
    @Override
    public QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest) {
        QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
        if (spaceUserQueryRequest == null) {
            return queryWrapper;
        }
        queryWrapper.eq(ObjUtil.isNotNull(spaceUserQueryRequest.getId()), "id", spaceUserQueryRequest.getId());
        queryWrapper.eq(ObjUtil.isNotNull(spaceUserQueryRequest.getSpaceId()), "spaceId", spaceUserQueryRequest.getSpaceId());
        queryWrapper.eq(ObjUtil.isNotNull(spaceUserQueryRequest.getUserId()), "userId", spaceUserQueryRequest.getUserId());
        queryWrapper.eq(ObjUtil.isNotNull(spaceUserQueryRequest.getSpaceRole()), "spaceRole", spaceUserQueryRequest.getSpaceRole());
        return queryWrapper;
    }

    /**
     * 获取空间成员视图对象
     *
     * @param spaceUser 空间成员
     * @param request   http请求
     * @return 空间成员视图对象
     * @author: Mr.Grass
     */
    @Override
    public SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request) {
        SpaceUserVO spaceUserVO = SpaceUserVO.objToVo(spaceUser);
        // 查询关联的用户信息
        if (spaceUser.getUserId() != null && spaceUser.getUserId() > 0) {
            User user = userService.getById(spaceUser.getUserId());
            if (ObjUtil.isNotNull(user)) {
                spaceUserVO.setUser(UserVO.objToVo(user));
            }
        }
        // 查询关联的空间信息
        if (spaceUser.getSpaceId() != null && spaceUser.getSpaceId() > 0) {
            Space space = spaceService.getById(spaceUser.getSpaceId());
            if (ObjUtil.isNotNull(space)) {
                spaceUserVO.setSpace(SpaceVO.objToVo(space));
            }
        }
        return spaceUserVO;
    }

    /**
     * 获取空间成员视图对象列表
     *
     * @param spaceUserList 空间成员列表
     * @return 空间成员视图对象列表
     * @author: Mr.Grass
     */
    @Override
    public List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList) {
        if (CollUtil.isEmpty(spaceUserList)) {
            return Collections.emptyList();
        }
        // 对象 ==> 视图对象
        List<SpaceUserVO> spaceUserVOList = spaceUserList.stream().map(SpaceUserVO::objToVo).collect(Collectors.toList());
        // 获取用户ID与空间ID
        Set<Long> userIdSet = spaceUserVOList.stream().map(SpaceUserVO::getUserId).collect(Collectors.toSet());
        Set<Long> spaceIdSet = spaceUserVOList.stream().map(SpaceUserVO::getSpaceId).collect(Collectors.toSet());

        // 查询用户信息与空间信息
        Map<Long, UserVO> userVOMapMap = userService.listByIds(userIdSet).stream().map(UserVO::objToVo).collect(Collectors.toMap(UserVO::getId, Function.identity()));
        Map<Long, SpaceVO> spaceVOMap = spaceService.listByIds(spaceIdSet).stream().map(SpaceVO::objToVo).collect(Collectors.toMap(SpaceVO::getId, Function.identity()));

        // 填充用户信息与空间信息
        for (SpaceUserVO spaceUserVO : spaceUserVOList) {
            UserVO userVO = userVOMapMap.get(spaceUserVO.getUserId());
            if (userVO != null) {
                spaceUserVO.setUser(userVO);
            }
            SpaceVO spaceVO = spaceVOMap.get(spaceUserVO.getSpaceId());
            if (spaceVO != null) {
                spaceUserVO.setSpace(spaceVO);
            }
        }
        return spaceUserVOList;
    }

    /**
     * @param userId 用户ID
     * @return java.util.List<com.grass.picturebackend.model.vo.SpaceUserVO>
     * @description: 获取用户待确认的团队空间邀请
     * @author: Mr.Liuxq
     * @date 2025/5/12 17:27
     */
    @Override
    public List<SpaceUserVO> listInvitationConfirm(Long userId) {
        QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        queryWrapper.eq("invitationConfirmStatus", "0");
        List<SpaceUser> spaceUserList = this.list(queryWrapper);
        return this.getSpaceUserVOList(spaceUserList);
    }

    /**
     * @param userId  用户ID
     * @param spaceId 空间ID
     * @param status  审批状态
     * @description: 审批团队空间邀请
     * @author: Mr.Liuxq
     * @date 2025/5/12 17:27
     */
    @Override
    public void approveInvitationInfo(Long userId, Long spaceId, Integer status) {
        // 校验用户与空间是否存在
        User user = userService.getById(userId);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        // 校验成员用户是否有邀请记录
        QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        queryWrapper.eq("spaceId", spaceId);
        queryWrapper.eq("invitationConfirmStatus", "0");
        SpaceUser spaceUser = this.getOne(queryWrapper);
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.OPERATION_ERROR, "没有邀请记录");
        spaceUser.setInvitationConfirmStatus(status.toString());
        boolean result = this.updateById(spaceUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "审批失败");
    }
}
