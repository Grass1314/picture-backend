package com.grass.picturebackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.grass.picturebackend.model.dto.space.SpaceAddRequest;
import com.grass.picturebackend.model.entity.Space;
import com.grass.picturebackend.model.entity.User;

public interface SpaceService extends IService<Space> {

    /**
     * 校验
     *
     * @param space 空间PO
     * @param add 是否新增
     */
    void validSpace(Space space ,boolean add);

    /**
     * 填充空间信息
     * @param space 空间PO
     */
    void fillSpaceBySpaceLevel(Space space);

    /**
     * 添加空间
     * @param spaceAddRequest 空间添加请求
     * @param loginUser 登录用户
     * @return 空间id
     */
    Long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);
}
