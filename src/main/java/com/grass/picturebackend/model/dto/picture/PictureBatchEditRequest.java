package com.grass.picturebackend.model.dto.picture;

import lombok.Data;

import java.util.List;

/**
 * @author Mr.Liuxq
 * @description: 批量更新图片
 * @date 2025年05月08日 09:55
 */

@Data
public class PictureBatchEditRequest {

    /**
     * 图片 id 列表
     */
    private List<Long> pictureIdList;

    /**
     * 空间 id
     */
    private Long spaceId;

    /**
     * 分类
     */
    private String category;

    /**
     * 标签
     */
    private List<String> tags;

    /**
     * 命名规则
     */
    private String nameRule;
}
