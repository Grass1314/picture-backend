package com.grass.picturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.grass.picturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.grass.picturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.grass.picturebackend.model.entity.SpaceUser;
import com.grass.picturebackend.model.vo.SpaceUserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public interface SpaceUserService extends IService<SpaceUser> {

    /**
     * 添加空间成员
     * @param spaceUserAddRequest 空间成员添加表单
     * @return 添加后的空间成员id
     */
    Long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);

    /**
     * 校验空间成员
     *
     * @param spaceUser  空间成员
     * @param add        是否为创建校验
     * @author: Mr.Grass
     */
    void validSpaceUser(SpaceUser spaceUser, boolean add);

    /**
     * 空间用户查询条件封装
     *
     * @param spaceUserQueryRequest 空间用户查询请求
     * @return 查询条件封装对象
     * @author: Mr.Grass
     */
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

    /**
     * 获取空间成员视图对象
     *
     * @param spaceUser 空间成员
     * @param request   http请求
     * @return 空间成员视图对象
     * @author: Mr.Grass
     */
    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request);

    /**
     * 获取空间成员视图对象列表
     *
     * @param spaceUserList 空间成员列表
     * @return 空间成员视图对象列表
     * @author: Mr.Grass
     */
    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);

    /**
     * @description: 获取用户待确认的团队空间邀请
     * @author: Mr.Liuxq
     * @date 2025/5/12 17:27
     * @param userId 用户ID
     * @return java.util.List<com.grass.picturebackend.model.vo.SpaceUserVO>
     */
    List<SpaceUserVO> listInvitationConfirm(Long userId);

    /**
     * @description: 审批团队空间邀请
     * @author: Mr.Liuxq
     * @date 2025/5/12 17:27
     * @param userId 用户ID
     * @param spaceId 空间ID
     * @param status 审批状态
     */
    void approveInvitationInfo(Long userId, Long spaceId, Integer status);
}
