package com.grass.picturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.grass.picturebackend.exception.BusinessException;
import com.grass.picturebackend.exception.ErrorCode;
import com.grass.picturebackend.exception.ThrowUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 * @author Mr.Liuxq
 * @description: URL地址上传文件
 * @date 2025年04月28日 16:29
 */
@Service
public class UrlPictureUpload extends PictureUploadTemplate {
    /**
     * 校验输入源（本地文件或URL）
     *
     * @param inputSource 输入源
     */
    @Override
    protected void validPicture(Object inputSource) {
        String fileUrl = (String) inputSource;
        ThrowUtils.throwIf(fileUrl == null, ErrorCode.PARAMS_ERROR, "上传文件不能为空");

        // 1.验证 URL 格式
        try {
            // 通过创建URL对象验证是否是合法的URL
            new URL(fileUrl);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        // 2.校验URL 协议
        ThrowUtils.throwIf(!fileUrl.startsWith("http://") && !fileUrl.startsWith("https://"), ErrorCode.PARAMS_ERROR, "仅支持 HTTP 或 HTTPS 协议的文件地址");

        // 3.发送 HEAD  请求以验证文件是否存在
        try (HttpResponse response = HttpUtil.createRequest(Method.HEAD, fileUrl).execute()) {
            // 未正常返回直接结束
            if (response.getStatus() != HttpStatus.HTTP_OK) {
                return;
            }
            // 4.校验文件类型
            String contentType = response.header("Content-Type");
            if (StrUtil.isNotBlank(contentType)) {
                // 允许文件类型
                final List<String> ALLOW_FORMAT_LIST = Arrays.asList("image/png", "image/jpg", "image/jpeg", "image/webp");
                ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(contentType.toLowerCase()), ErrorCode.PARAMS_ERROR, "不支持的文件类型");
            }
            // 5.校验文件大小
            String contentLength = response.header("Content-Length");
            if (StrUtil.isNotBlank(contentLength)) {
                try {
                    long fileSize = Long.parseLong(contentLength);
                    final long ONE_M = 1024 * 1024;
                    ThrowUtils.throwIf(fileSize > 2 * ONE_M, ErrorCode.PARAMS_ERROR, "文件大小不能超过 2M");
                } catch (NumberFormatException e) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小格式错误");
                }
            }
        }
    }

    /**
     * 获取输入源的原始文件名
     *
     * @param inputSource 输入源
     */
    @Override
    protected String getOriginalFileName(Object inputSource) {
        String fileUrl = (String) inputSource;
        return FileUtil.mainName(fileUrl);
    }

    /**
     * 处理输入流并生成本地临时文件
     *
     * @param inputSource 输入源
     * @param file 文件
     */
    @Override
    protected void processFile(Object inputSource, File file) throws Exception {
        String fileUrl = (String) inputSource;
        HttpUtil.downloadFile(fileUrl, file);
    }
}
