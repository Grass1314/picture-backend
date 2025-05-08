package com.grass.picturebackend.api.imagesearch.sub;

import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.grass.picturebackend.api.imagesearch.model.ImageSearchResult;
import com.grass.picturebackend.exception.BusinessException;
import com.grass.picturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author Mr.Liuxq
 * @description: 获取图片列表
 * @date 2025年05月07日 09:10
 */
@Slf4j
public class GetImageListApi {

    public static List<ImageSearchResult> getImageList(String url) {
        try {
            // 发送get请求
            HttpResponse response = HttpUtil.createGet(url).execute();

            // 获取响应内容
            int status = response.getStatus();
            String body = response.body();

            // 处理响应
            if (status == 200) {
                // 解析 JSON数据
                return processResponse(body);
            } else {
                // 处理失败
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "请求失败");
            }
        } catch (Exception e) {
            log.error("请求失败");
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "请求失败");
        }
    }

    /**
     * 处理响应
     *
     * @param responseBody 接口返回的JSON字符串
     * @return List<ImageSearchResult>
     */
    private static List<ImageSearchResult> processResponse(String responseBody) {
        // 解析响应对象
        JSONObject jsonObject = new JSONObject(responseBody);
        if (!jsonObject.containsKey("data")) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未获取到图片列表");
        }
        JSONObject data = jsonObject.getJSONObject("data");
        if (!data.containsKey("list")) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未获取到图片列表");
        }
        JSONArray list = data.getJSONArray("list");
        return JSONUtil.toList(list, ImageSearchResult.class);
    }
}
