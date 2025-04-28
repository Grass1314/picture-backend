package com.grass.picturebackend.constant;

import java.util.Arrays;
import java.util.List;

/**
 * @author Mr.Liuxq
 * @description: 图片标签分类
 * @date 2025年04月28日 11:17
 */
public interface PictureTagCategoryConstant {

    /**
     * 图片标签分类
     */
    List<String> TAR_GROUP_LIST = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");

    /**
     * 图片分类
     */
    List<String> CATEGORY_GROUP_LIST = Arrays.asList("模板", "电商", "表情包", "素材", "海报");
}
