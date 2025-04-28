package com.grass.picturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.grass.picturebackend.exception.ErrorCode;
import com.grass.picturebackend.exception.ThrowUtils;
import com.grass.picturebackend.manager.FileManager;
import com.grass.picturebackend.mapper.PictureMapper;
import com.grass.picturebackend.model.dto.file.UploadPictureResult;
import com.grass.picturebackend.model.dto.picture.PictureQueryRequest;
import com.grass.picturebackend.model.dto.picture.PictureUploadRequest;
import com.grass.picturebackend.model.entity.Picture;
import com.grass.picturebackend.model.entity.User;
import com.grass.picturebackend.model.vo.PictureVO;
import com.grass.picturebackend.model.vo.UserVO;
import com.grass.picturebackend.service.PictureService;
import com.grass.picturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Mr.Liuxq
 * @description: 图片处理业务层
 * @date 2025年04月27日 15:07
 */
@Service
@Slf4j
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture> implements PictureService {

    @Resource
    private FileManager fileManager;

    @Resource
    private UserService userService;

    /**
     * 上传图片
     *
     * @param multipartFile        文件
     * @param pictureUploadRequest 文件上传表单
     * @param loginUser            登录用户
     * @return 图片信息
     */
    @Override
    public PictureVO uploadPicture(MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        // 判断是新增还是更新图片
        Long pictureId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }
        // 如果更新图片，先校验图片是否存在
        if (pictureId != null) {
            this.lambdaQuery().eq(Picture::getId, pictureId).oneOpt().orElseThrow(() -> new RuntimeException("图片不存在"));
        }
        // 上传图片
        // 按照用户ID划分目录
        String uploadPathPrefix = String.format("public/%s", loginUser.getId());
        UploadPictureResult uploadPictureResult = fileManager.uploadPicture(multipartFile, uploadPathPrefix);
        // 封装图片信息
        Picture picture = new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setName(uploadPictureResult.getPicName());
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        // 如果pictureId不为空，则更新图片信息,否则新增
        if (pictureId != null) {
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        boolean result = this.saveOrUpdate(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败");
        return PictureVO.objToVo(picture);
    }

    /**
     * 获取查询条件
     *
     * @param pictureQueryRequest 查询条件
     * @return 图片查询条件
     */
    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) {
            return queryWrapper;
        }
        // 获取查询条件
        if (StrUtil.isNotBlank(pictureQueryRequest.getSearchText())) {
            queryWrapper.and(i -> i.like("name", pictureQueryRequest.getSearchText()))
                    .or()
                    .like("introduction", pictureQueryRequest.getSearchText());
        }
        queryWrapper.eq(ObjUtil.isNotEmpty(pictureQueryRequest.getId()), "id", pictureQueryRequest.getId());
        queryWrapper.eq(ObjUtil.isNotEmpty(pictureQueryRequest.getUserId()), "userId", pictureQueryRequest.getUserId());
        queryWrapper.eq(ObjUtil.isNotEmpty(pictureQueryRequest.getName()), "name", pictureQueryRequest.getName());
        queryWrapper.like(ObjUtil.isNotEmpty(pictureQueryRequest.getIntroduction()), "introduction", pictureQueryRequest.getIntroduction());
        queryWrapper.like(ObjUtil.isNotEmpty(pictureQueryRequest.getPicFormat()), "picFormat", pictureQueryRequest.getPicFormat());
        queryWrapper.eq(StrUtil.isNotBlank(pictureQueryRequest.getCategory()), "category", pictureQueryRequest.getCategory());
        queryWrapper.eq(ObjUtil.isNotEmpty(pictureQueryRequest.getPicWidth()), "picWidth", pictureQueryRequest.getPicWidth());
        queryWrapper.eq(ObjUtil.isNotEmpty(pictureQueryRequest.getPicHeight()), "picHeight", pictureQueryRequest.getPicHeight());
        queryWrapper.eq(ObjUtil.isNotEmpty(pictureQueryRequest.getPicSize()), "picSize", pictureQueryRequest.getPicSize());
        queryWrapper.eq(ObjUtil.isNotEmpty(pictureQueryRequest.getPicScale()), "picScale", pictureQueryRequest.getPicScale());
        // json 数组查询
        List<String> tags = pictureQueryRequest.getTags();
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        // 排序
        queryWrapper.orderBy(StrUtil.isNotBlank(pictureQueryRequest.getSortField()), pictureQueryRequest.getSortOrder().equals("ascend"), pictureQueryRequest.getSortField());
        return queryWrapper;
    }

    /**
     * 获取图片包装类（单条）
     *
     * @param picture 图片
     * @return 图片信息
     */
    @Override
    public PictureVO getPictureVO(Picture picture) {
        PictureVO pictureVO = PictureVO.objToVo(picture);
        Long userId = picture.getUserId();
        if (userId != null) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setUser(userVO);
        }
        return pictureVO;
    }

    /**
     * 获取图片包装类（分页）
     *
     * @param picturePage 图片列表
     * @return 图片信息列表
     */
    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage) {
        List<Picture> records = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(records)) {
            return pictureVOPage;
        }
        // 转换PO ==》 VO
        List<PictureVO> pictureVOS = records.stream().map(PictureVO::objToVo).collect(Collectors.toList());
        // 获取用户信息
        Set<Long> userIdSet = records.stream().map(Picture::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userService.listByIds(userIdSet).stream().collect(Collectors.toMap(User::getId, Function.identity()));
        // 填充用户信息
        pictureVOS.forEach(pictureVO -> {
            User user = null;
            if (userMap.containsKey(pictureVO.getUserId())) {
                user = userMap.get(pictureVO.getUserId());
            }
            pictureVO.setUser(userService.getUserVO(user));
        });

        pictureVOPage.setRecords(pictureVOS);
        return pictureVOPage;
    }

    /**
     * 校验图片
     *
     * @param picture 图片
     */
    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();

        ThrowUtils.throwIf(ObjUtil.isEmpty(id), ErrorCode.PARAMS_ERROR, "图片id不能为空");
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 1024, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }

}
