package com.grass.picturebackend.api.imagesearch;

import com.grass.picturebackend.api.imagesearch.model.ImageSearchResult;
import com.grass.picturebackend.api.imagesearch.sub.GetImageFirstUrlApi;
import com.grass.picturebackend.api.imagesearch.sub.GetImageListApi;
import com.grass.picturebackend.api.imagesearch.sub.GetImagePageUrlApi;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author Mr.Liuxq
 * @description: 图片搜索
 * @date 2025年05月07日 09:30
 */

@Slf4j
public class ImageSearchApiFacade {

    /**
     * 图片搜索
     * @param imageUrl 图片URL
     * @return 图片搜索结果
     */
    public static List<ImageSearchResult> searchImage(String imageUrl) {
        String imagePageUrl = GetImagePageUrlApi.getImagePageUrl(imageUrl);
        String imageFirstUrl = GetImageFirstUrlApi.getImageFirstUrl(imagePageUrl);
        return GetImageListApi.getImageList(imageFirstUrl);
    }
}
