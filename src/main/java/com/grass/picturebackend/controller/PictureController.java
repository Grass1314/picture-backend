package com.grass.picturebackend.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.grass.picturebackend.annotation.AuthCheck;
import com.grass.picturebackend.common.BaseResponse;
import com.grass.picturebackend.common.DeleteRequest;
import com.grass.picturebackend.common.ResultUtils;
import com.grass.picturebackend.constant.PictureTagCategoryConstant;
import com.grass.picturebackend.constant.UserConstant;
import com.grass.picturebackend.exception.BusinessException;
import com.grass.picturebackend.exception.ErrorCode;
import com.grass.picturebackend.exception.ThrowUtils;
import com.grass.picturebackend.model.dto.picture.*;
import com.grass.picturebackend.model.entity.Picture;
import com.grass.picturebackend.model.entity.User;
import com.grass.picturebackend.model.enums.PictureReviewStatusEnum;
import com.grass.picturebackend.model.vo.PictureTagCategory;
import com.grass.picturebackend.model.vo.PictureVO;
import com.grass.picturebackend.service.PictureService;
import com.grass.picturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;

/**
 * @author Mr.Liuxq
 * @description: 图片
 * @date 2025年04月27日 17:11
 */
@RestController
@RequestMapping("/picture")
@Slf4j
public class PictureController {

    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;


    /**
     * @description:  图片上传
     * @author: Mr.Liuxq
     * @date 2025/4/28 10:27
     * @param multipartFile 文件
     * @param pictureUploadRequest 图片上传请求
     * @param request 图片信息
     *
     */
    @PostMapping("/upload")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<PictureVO> uploadPicture(
            @RequestPart("file") MultipartFile multipartFile,
            PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * @description: 删除图片
     * @author: Mr.Liuxq
     * @date 2025/4/28 10:27
     * @param deleteRequest 图片id
     * @param request http
     * @return com.grass.picturebackend.common.BaseResponse<java.lang.Boolean>
     */
    @DeleteMapping("/delete")
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        Long id = deleteRequest.getId();
        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅限本人或者管理员可删除
        if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 删除图片
        boolean result = pictureService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * @description: 更新图片 (仅限管理员可用)
     * @author: Mr.Liuxq
     * @date 2025/4/28 10:27
     * @param pictureUpdateRequest 图片信息
     * @return com.grass.picturebackend.common.BaseResponse<java.lang.Boolean>
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest, HttpServletRequest request) {
        if (pictureUpdateRequest == null || pictureUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Picture picture = new Picture();
        BeanUtil.copyProperties(pictureUpdateRequest, picture);
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));
        // 数据校验
        pictureService.validPicture(picture);
        // 判断图片是否存在
        Picture oldPicture = pictureService.getById(pictureUpdateRequest.getId());
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        User loginUser = userService.getLoginUser(request);
        // 添加审核信息
        pictureService.fillReviewParams(picture, loginUser);
        // 更新
        boolean result = pictureService.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * @description: 获取图片信息 (管理员)
     * @author: Mr.Liuxq
     * @date 2025/4/28 10:27
     * @param id 图片id
     * @return com.grass.picturebackend.common.BaseResponse<com.grass.picturebackend.model.entity.Picture>
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Picture> getPictureById(Long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 获取图片
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(picture);
    }

    /**
     * @description: 获取图片信息 (用户)
     * @author: Mr.Liuxq
     * @date 2025/4/28 10:27
     * @param id 图片id
     * @return com.grass.picturebackend.common.BaseResponse<com.grass.picturebackend.model.vo.PictureVO>
     */
    @GetMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureVOById(Long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 获取图片
        // Picture picture = pictureService.getById(id);
        PictureQueryRequest pictureQueryRequest = new PictureQueryRequest();
        pictureQueryRequest.setId(id);
        // 用户只能查询审核通过的图片
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        Picture picture = pictureService.getOne(pictureService.getQueryWrapper(pictureQueryRequest));
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(pictureService.getPictureVO(picture));
    }

    /**
     * @description: 获取图片列表 (管理员)
     * @author: Mr.Liuxq
     * @date 2025/4/28 10:27
     * @param pictureQueryRequest 图片查询请求
     * @return com.grass.picturebackend.common.BaseResponse<com.baomidou.mybatisplus.extension.plugins.pagination.Page < com.grass.picturebackend.model.vo.PictureVO>>
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Picture>> listPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest) {
        // 分页查询
        Page<Picture> picturePage = pictureService.page(new Page<>(pictureQueryRequest.getCurrent(), pictureQueryRequest.getPageSize()),
                pictureService.getQueryWrapper(pictureQueryRequest));
        return ResultUtils.success(picturePage);
    }

    /**
     * @description: 获取图片列表 (用户)
     * @author: Mr.Liuxq
     * @date 2025/4/28 10:27
     * @param pictureQueryRequest 图片查询请求
     * @return com.grass.picturebackend.common.BaseResponse<com.baomidou.mybatisplus.extension.plugins.pagination.Page < com.grass.picturebackend.model.vo.PictureVO>>
     */
    @PostMapping("/list/vo/page")
    public BaseResponse<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest) {
        // 限制爬虫
        ThrowUtils.throwIf(pictureQueryRequest.getPageSize() >20, ErrorCode.PARAMS_ERROR);
        // 普通用户默认只能查看已过审的数据
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        // 分页查询
        Page<Picture> picturePage = pictureService.page(new Page<>(pictureQueryRequest.getCurrent(), pictureQueryRequest.getPageSize()),
                pictureService.getQueryWrapper(pictureQueryRequest));
        return ResultUtils.success(pictureService.getPictureVOPage(picturePage));
    }

    /**
     * @description: 编辑图片 (用户)
     * @author: Mr.Liuxq
     * @date 2025/4/28 10:27
     * @param pictureEditRequest 图片编辑请求
     * @param request 请求
     * @return com.grass.picturebackend.common.BaseResponse<java.lang.Boolean>
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest, HttpServletRequest request) {
        if (pictureEditRequest == null || pictureEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Picture picture = new Picture();
        BeanUtil.copyProperties(pictureEditRequest, picture);
        picture.setEditTime(new Date());
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        // 校验
        pictureService.validPicture(picture);
        // 查询登录用户
        User loginUser = userService.getLoginUser(request);
        // 查询图片是否存在
        Picture oldPicture = pictureService.getById(pictureEditRequest.getId());
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅限本人与管理员可以编辑
        if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 添加审核信息
        pictureService.fillReviewParams(picture, loginUser);
        // 更新图片
        boolean result = pictureService.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * @description: 获取图片标签和分类 (用户)
     * @author: Mr.Liuxq
     * @date 2025/4/28 10:27
     * @return com.grass.picturebackend.common.BaseResponse<com.grass.picturebackend.model.vo.PictureTagCategory>
     */
    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> getPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        pictureTagCategory.setTagList(PictureTagCategoryConstant.TAR_GROUP_LIST);
        pictureTagCategory.setCategoryList(PictureTagCategoryConstant.CATEGORY_GROUP_LIST);

        return ResultUtils.success(pictureTagCategory);
    }

    /**
     * @description: 图片审核 (管理员)
     * @author: Mr.Liuxq
     * @date 2025/4/28 10:27
     * @param pictureReviewRequest 图片审核请求
     * @param request 请求
     * @return com.grass.picturebackend.common.BaseResponse<java.lang.Boolean>
     */
    @PostMapping("/review")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> doPictureReview(@RequestBody PictureReviewRequest pictureReviewRequest, HttpServletRequest request) {
        if (pictureReviewRequest == null || pictureReviewRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 查询登录用户
        User loginUser = userService.getLoginUser(request);
        pictureService.doPictureReview(pictureReviewRequest, loginUser);
        return ResultUtils.success(true);
    }

}
