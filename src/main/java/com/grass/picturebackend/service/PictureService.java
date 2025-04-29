package com.grass.picturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.grass.picturebackend.model.dto.picture.PictureQueryRequest;
import com.grass.picturebackend.model.dto.picture.PictureReviewRequest;
import com.grass.picturebackend.model.dto.picture.PictureUploadByBatchRequest;
import com.grass.picturebackend.model.dto.picture.PictureUploadRequest;
import com.grass.picturebackend.model.entity.Picture;
import com.grass.picturebackend.model.entity.User;
import com.grass.picturebackend.model.vo.PictureVO;

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


}
