package com.grass.picturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.grass.picturebackend.model.dto.picture.*;
import com.grass.picturebackend.model.entity.Picture;
import com.grass.picturebackend.model.entity.User;
import com.grass.picturebackend.model.vo.PictureVO;

import java.util.List;

public interface PictureService extends IService<Picture> {

    /**
     * 上传图片
     * @param inputSource 文件
     * @param pictureUploadRequest 文件上传表单
     * @param loginUser 登录用户
     * @return 图片信息
     */
    PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser);

    /**
     * 获取查询条件
     * @param pictureQueryRequest 查询条件
     * @return 图片查询条件
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    /**
     * 获取图片包装类（单条）
     * @param picture 图片
     * @return 图片信息
     */
    PictureVO getPictureVO(Picture picture);

    /**
     * 获取图片包装类（多条）
     * @param picturePage 图片列表
     * @return 图片信息列表
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage);

    /**
     * 校验图片
     * @param picture 图片
     */
    void validPicture(Picture picture);

    /**
     * 图片审核
     * @param pictureReviewRequest 图片审核请求
     * @param loginUser 登录用户
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    /**
     * 填充审核参数
     * @param picture 图片
     * @param loginUser 登录用户
     */
    void fillReviewParams(Picture picture, User loginUser);

    /**
     * 批量抓取和创建图片
     *
     * @param pictureUploadByBatchRequest 图片批量上传请求
     * @param loginUser 登录用户
     * @return 成功创建的图片数
     */
    Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser);

    /**
     * 清理图片文件
     * @param picture 图片
     */
    void clearPictureFile(Picture picture);

    /**
     * 检查图片空间权限
     * @param loginUser 登录用户
     * @param picture   图片
     */
    void checkPictureAuth(User loginUser, Picture picture);

    /**
     * 删除图片
     * @param pictureId 图片id
     * @param loginUser 登录用户
     */
    void deletePicture(Long pictureId, User loginUser);

    /**
     * 编辑图片
     * @param pictureEditRequest 图片编辑请求
     * @param loginUser 登录用户
     */
    void editPicture(PictureEditRequest pictureEditRequest, User loginUser);

    /**
     * 根据颜色搜索图片
     * @param searchPictureByColorRequest 搜索图片请求
     * @param loginUser 登录用户
     * @return 图片信息列表
     */
    List<PictureVO> searchPictureByColor(SearchPictureByColorRequest searchPictureByColorRequest, User loginUser);

    /**
     * 批量编辑图片
     * @param pictureEditByBatchRequest 图片批量编辑请求
     * @param loginUser 登录用户
     */
    void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser);

    /**
     * 批量编辑图片元数据
     * @param request 图片批量编辑请求
     * @param loginUserId 登录用户
     */
    void batchPictureMetadata(PictureBatchEditRequest request, Long loginUserId);
}
