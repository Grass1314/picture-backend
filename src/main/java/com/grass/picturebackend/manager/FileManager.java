package com.grass.picturebackend.manager;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.grass.picturebackend.config.CosClientConfig;
import com.grass.picturebackend.exception.BusinessException;
import com.grass.picturebackend.exception.ErrorCode;
import com.grass.picturebackend.exception.ThrowUtils;
import com.grass.picturebackend.model.dto.file.UploadPictureResult;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @author Mr.Liuxq
 * @description: 公共文件验证类
 * @date 2025年04月27日 15:27
 */
@Service
@Slf4j
public class FileManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManager cosManager;

    public UploadPictureResult uploadPicture(MultipartFile multipartFile, String uploadPathPrefix) {
        // 校验图片
        validPicture(multipartFile);
        // 图片上传地址
        String uuid = RandomUtil.randomString(16);
        String originalFilename = multipartFile.getOriginalFilename();
        String uploadFileName = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, FileUtil.getSuffix(originalFilename));
        String uploadPath = String.format("%s/%s", uploadPathPrefix, uploadFileName);
        File file = null;
        try {
            // 创建临时文件
            file = File.createTempFile(uploadPath, null);
            multipartFile.transferTo(file);
            // 上传图片
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            // 封装返回结果
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            int picWidth = imageInfo.getWidth();
            int picHeight = imageInfo.getHeight();
            double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
            uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
            uploadPictureResult.setPicWidth(picWidth);
            uploadPictureResult.setPicHeight(picHeight);
            uploadPictureResult.setPicScale(picScale);
            uploadPictureResult.setPicFormat(imageInfo.getFormat());
            uploadPictureResult.setPicSize(FileUtil.size(file));
            uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
        } catch (Exception e) {
            log.error("图片上传到对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            this.deleteTmpFile(file);
        }

        return null;
    }

    // 上传文件  
    /*public static String uploadToCOS(MultipartFile multipartFile, String bucketName, String key) throws Exception {
        // 创建 COS 客户端  
        COSClient cosClient = createCOSClient();

        try (InputStream inputStream = multipartFile.getInputStream()) {
            // 元信息配置  
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(multipartFile.getSize());
            metadata.setContentType(multipartFile.getContentType());

            // 创建上传请求  
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, inputStream, metadata);

            // 上传文件  
            cosClient.putObject(putObjectRequest);

            // 生成访问链接  
            return "https://" + bucketName + ".cos." + cosClient.getClientConfig().getRegion().getRegionName()
                    + ".myqcloud.com/" + key;
        } finally {
            cosClient.shutdown();
        }
    }*/


    /**
     * @description: 校验图片
     * @author: Mr.Liuxq
     * @date 2025/4/27 15:58
     * @param multipartFile 文件
     */
    private void validPicture(MultipartFile multipartFile) {
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR, "上传文件不能为空");
        // 1.校验文件大小
        long fileSize = multipartFile.getSize();
        final long ONE_M = 1024 * 1024;
        ThrowUtils.throwIf(fileSize > 2 * ONE_M, ErrorCode.PARAMS_ERROR, "文件大小不能超过 2M");
        // 2.校验文件后缀
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        // 允许上传的文件后缀
        final List<String> ALLOW_FORMAT_LIST = Arrays.asList("png", "jpg", "jpeg", "webp");
        ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(fileSuffix), ErrorCode.PARAMS_ERROR, "不支持的文件类型");
    }

    /**
     * @description: 删除临时文件
     * @author: Mr.Liuxq
     * @date 2025/4/27 15:58
     * @param file 文件
     */
    private void deleteTmpFile(File file) {
        if (file != null) {
            // 删除临时文件
            boolean delete = file.delete();
            if (!delete) {
                log.error("file delete error, filePath = {}", file.getAbsolutePath());
            }
        }
    }

}
