package com.grass.picturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.grass.picturebackend.config.CosClientConfig;
import com.grass.picturebackend.exception.BusinessException;
import com.grass.picturebackend.exception.ErrorCode;
import com.grass.picturebackend.exception.ThrowUtils;
import com.grass.picturebackend.manager.CosManager;
import com.grass.picturebackend.manager.upload.FilePictureUpload;
import com.grass.picturebackend.manager.upload.PictureUploadTemplate;
import com.grass.picturebackend.manager.upload.UrlPictureUpload;
import com.grass.picturebackend.mapper.PictureMapper;
import com.grass.picturebackend.model.dto.file.UploadPictureResult;
import com.grass.picturebackend.model.dto.picture.*;
import com.grass.picturebackend.model.entity.Picture;
import com.grass.picturebackend.model.entity.Space;
import com.grass.picturebackend.model.entity.User;
import com.grass.picturebackend.model.enums.PictureReviewStatusEnum;
import com.grass.picturebackend.model.vo.PictureVO;
import com.grass.picturebackend.model.vo.UserVO;
import com.grass.picturebackend.service.PictureService;
import com.grass.picturebackend.service.SpaceService;
import com.grass.picturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;
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

    /*@Resource
    private FileManager fileManager;*/

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private UrlPictureUpload urlPictureUpload;

    @Resource
    private UserService userService;

    @Resource
    private CosManager cosManager;

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private SpaceService spaceService;

    @Resource
    private TransactionTemplate transactionTemplate;


    /**
     * 上传图片
     *
     * @param inputSource        文件
     * @param pictureUploadRequest 文件上传表单
     * @param loginUser            登录用户
     * @return 图片信息
     */
    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        if (inputSource == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片为空");
        }
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        // 校验空间是否存在
        Long spaceId = pictureUploadRequest.getSpaceId();
        if (spaceId != null) {
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            // 判断权限，必须是空间创建人或管理员才能上传
            if (!space.getUserId().equals(loginUser.getId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间权限");
            }
            // 校验空间条数和大小
            if (space.getMaxCount() != null && space.getTotalCount() >= space.getMaxCount()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间条数不足");
            }
            if (space.getMaxSize() != null && space.getTotalSize() > space.getMaxSize()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间大小不足");
            }
        }
        // 判断是新增还是更新图片
        Long pictureId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }
        // 如果是更新图片，需要校验图片是否存在
        if (pictureId != null) {
            Picture oldPicture = this.getById(pictureId);
            ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            // 仅本人或管理员可编辑
            if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
            // 校验空间是否一致
            if (spaceId == null) {
                if (oldPicture.getSpaceId() != null) {
                    spaceId = oldPicture.getSpaceId();
                }
            } else if (oldPicture.getSpaceId() != null && !oldPicture.getSpaceId().equals(spaceId)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片空间不一致");
            }
        }

        // 上传图片
        // 通过空间ID是否存在判断是按照用户ID划分目录还是按照空间ID划分目录
        String uploadPathPrefix;
        if (spaceId == null) {
            uploadPathPrefix = String.format("public/%s", loginUser.getId());
        } else {
            uploadPathPrefix = String.format("space/%s", spaceId);
        }

        // 根据inputSource类型，处理文件来源
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);
        // 封装图片信息
        Picture picture = new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        String picName = uploadPictureResult.getPicName();
        if (pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getPicName())) {
            picName = pictureUploadRequest.getPicName();
        }
        picture.setName(picName);
        picture.setName(uploadPictureResult.getPicName());
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        picture.setSpaceId(spaceId);
        // 如果pictureId不为空，则更新图片信息,否则新增
        if (pictureId != null) {
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        // 添加审核信息
        this.fillReviewParams(picture, loginUser);
        Long finalSpaceId = spaceId;
        transactionTemplate.execute(status -> {
            boolean result = this.saveOrUpdate(picture);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败");
            if (finalSpaceId != null) {
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, finalSpaceId)
                        .setSql("totalSize = totalSize + " + picture.getPicSize())
                        .setSql("totalCount = totalCount + 1")
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "空间信息更新失败");
            }
            return PictureVO.objToVo(picture);
        });
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
        queryWrapper.eq(ObjUtil.isNotEmpty(pictureQueryRequest.getReviewStatus()), "reviewStatus", pictureQueryRequest.getReviewStatus());
        queryWrapper.like(ObjUtil.isNotEmpty(pictureQueryRequest.getReviewMessage()), "reviewMessage", pictureQueryRequest.getReviewMessage());
        queryWrapper.eq(ObjUtil.isNotEmpty(pictureQueryRequest.getReviewerId()), "reviewerId", pictureQueryRequest.getReviewerId());
        queryWrapper.eq(ObjUtil.isNotEmpty(pictureQueryRequest.getSpaceId()), "spaceId", pictureQueryRequest.getSpaceId());
        queryWrapper.isNull(pictureQueryRequest.isNullSpaceId(), "spaceId");
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

    /**
     * 图片审核
     *
     * @param pictureReviewRequest 图片审核请求
     * @param loginUser            登录用户
     */
    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        PictureReviewStatusEnum reviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        if (id == null || reviewStatus == null || PictureReviewStatusEnum.REVIEWING.equals(reviewStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 查询图片
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        if (Objects.equals(oldPicture.getReviewStatus(), reviewStatus)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "图片已审核过");
        }
        // 更新审核状态
        Picture newPicture = new Picture();
        BeanUtil.copyProperties(pictureReviewRequest, newPicture);
        newPicture.setReviewerId(loginUser.getId());
        newPicture.setReviewTime(new Date());
        boolean result = this.updateById(newPicture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    /**
     * 填充审核参数
     *
     * @param picture   图片
     * @param loginUser 登录用户
     */
    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        if (userService.isAdmin(loginUser)) {
            // 管理员自动过审
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewerId(loginUser.getId());
            picture.setReviewMessage("管理员自动过审");
            picture.setReviewTime(new Date());
        } else {
            // 非管理员，创建或编辑都要改为待审核
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }

    /**
     * 批量抓取和创建图片
     *
     * @param pictureUploadByBatchRequest 图片批量上传请求
     * @param loginUser                   登录用户
     * @return 成功创建的图片数
     */
    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        String searchText = pictureUploadByBatchRequest.getSearchText();
        String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
        if (StrUtil.isBlank(namePrefix)) {
            namePrefix = searchText;
        }
        // 格式化数量
        Integer count = pictureUploadByBatchRequest.getCount();
        ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR, "抓取数量不能大于30条");
        // 抓取地址
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        Document document;
        try {
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("抓取图片失败，搜索地址：{}", fetchUrl);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取页面失败");
        }
        Element div = document.getElementsByClass("dgControl").first();
        if (ObjUtil.isEmpty(div)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取元素失败");
        }
        Elements imgElementList = div.select("img.mimg");
        int uploadCount = 0;
        for (Element element : imgElementList) {
            String fileUrl = element.attr("src");
            if (StrUtil.isBlank(fileUrl)) {
                log.info("图片地址为空,已跳过：{}",  fileUrl);
                continue;
            }
            // 处理图片上传地址，防止出现转义问题
            int questionMarkIndex = fileUrl.indexOf("?");
            if (questionMarkIndex > -1) {
                fileUrl = fileUrl.substring(0, questionMarkIndex);
            }
            // 上传图片
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            if (StrUtil.isNotBlank(namePrefix)) {
                pictureUploadRequest.setPicName(namePrefix + (uploadCount + 1));
            }
            try {
                PictureVO pictureVO = this.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
                log.info("图片上传成功, id = {}",  pictureVO.getId());
                uploadCount++;
            } catch (Exception e) {
                log.error("图片上传失败, url = {}",  fileUrl);
                continue;
            }
            if (uploadCount >= count) {
                break;
            }
        }
        return uploadCount;
    }

    /**
     * 清理图片文件
     *
     * @param picture 图片
     */
    @Override
    @Async
    public void clearPictureFile(Picture picture) {
        // 判断该图片是否被多处地方使用
        String pictureUrl = picture.getUrl();
        Long count = this.lambdaQuery().eq(Picture::getUrl, pictureUrl).count();
        // 如果被多处地方使用，则不删除
        if (count > 1) {
            return;
        }
        // 调用删除图片方法之前对URL进行处理，现在URL中含有域名，实际只需要key值（存储路径）
        String key = pictureUrl.substring(pictureUrl.indexOf(cosClientConfig.getHost()));
        try {
            cosManager.deleteObject(key);
        } catch (Exception e) {
            log.error("删除图片失败, url = {}",  pictureUrl);
        }
        // 删除缩略图
        String thumbnailUrl = picture.getThumbnailUrl();
        if (StrUtil.isNotBlank(thumbnailUrl)) {
            String thumbnailKey = thumbnailUrl.substring(thumbnailUrl.indexOf(cosClientConfig.getHost()));
            try {
                cosManager.deleteObject(thumbnailKey);
            } catch (Exception e) {
                log.error("删除缩略图失败, url = {}",  thumbnailUrl);
            }
        }
    }

    /**
     * 检查图片空间权限
     * @param loginUser 登录用户
     * @param picture   图片
     */
    @Override
    public void checkPictureAuth(User loginUser, Picture picture) {
        Long spaceId = picture.getSpaceId();
        if (spaceId == null) {
            // 公共图库，仅本人或管理员可操作
            if (!picture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        } else {
            // 私有空间，仅空间管理员可操作
            if (!picture.getUserId().equals(loginUser.getId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }
    }

    /**
     * 删除图片
     *
     * @param pictureId 图片id
     * @param loginUser 登录用户
     */
    @Override
    public void deletePicture(Long pictureId, User loginUser) {
        Picture oldPicture = this.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅限本人或者管理员可删除
        if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 检验图片空间权限
        this.checkPictureAuth(loginUser, oldPicture);
        // 开启事务
        transactionTemplate.execute(status -> {
            // 删除图片
            boolean result = this.removeById(pictureId);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
            // 释放存储空间
            Long spaceId = oldPicture.getSpaceId();
            if (spaceId != null) {
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, spaceId)
                        .setSql("totalSize = totalSize - " + oldPicture.getPicSize())
                        .setSql("totalCount = totalCount - 1")
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR,"存储空间更新失败");
            }
            return true;
        });
        // 清理图片文件
        this.clearPictureFile(oldPicture);
    }

    /**
     * 编辑图片
     *
     * @param pictureEditRequest 图片编辑请求
     * @param loginUser          登录用户
     */
    @Override
    public void editPicture(PictureEditRequest pictureEditRequest, User loginUser) {
        Picture picture = new Picture();
        BeanUtil.copyProperties(pictureEditRequest, picture);
        picture.setEditTime(new Date());
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        // 编辑时间
        picture.setEditTime(new Date());
        // 校验
        this.validPicture(picture);
        // 查询图片是否存在
        Picture oldPicture = this.getById(pictureEditRequest.getId());
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        this.checkPictureAuth(loginUser, oldPicture);
        // 添加审核信息
        this.fillReviewParams(picture, loginUser);
        // 更新图片
        boolean result = this.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }


}
