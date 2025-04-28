package com.grass.picturebackend.controller;

import com.grass.picturebackend.annotation.AuthCheck;
import com.grass.picturebackend.common.BaseResponse;
import com.grass.picturebackend.common.ResultUtils;
import com.grass.picturebackend.constant.UserConstant;
import com.grass.picturebackend.exception.BusinessException;
import com.grass.picturebackend.exception.ErrorCode;
import com.grass.picturebackend.manager.CosManager;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;

/**
 * @author Mr.Liuxq
 * @description: 文件控制器
 * @date 2025年04月27日 14:32
 */

@Slf4j
@RestController
@RequestMapping("/file")
public class FileController {

    @Resource
    private CosManager cosManager;

    /**
     * 测试文件上传
     * @param multipartFile 文件
     * @return 文件路径
     * @author Mr.Liuxq
     */
    @PostMapping("/test/upload")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<String> testUploadFile(@RequestPart("file")MultipartFile multipartFile) {
        // 文件目录
        String filename = multipartFile.getOriginalFilename();
        String filePath = String.format("/test/%s", filename);
        File file = null;
        try {
            file = File.createTempFile(filePath,null);
            multipartFile.transferTo(file);
            cosManager.putObject(filePath,file);
            // 返回可访问地址
            return ResultUtils.success(filePath);
        } catch (Exception e) {
            log.error("file upload error, filePath = {}",filePath,e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            if (file != null) {
                // 删除临时文件
                boolean delete = file.delete();
                if (!delete) {
                    log.error("file delete error, filePath = {}",filePath);
                }
            }
        }
    }

    /**
     * 测试文件下载
     * @param filePath 文件路径
     * @param response 响应
     * @author Mr.Liuxq
     */
    @GetMapping("/test/download")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public void testDownloadFile(String filePath, HttpServletResponse response) {
        COSObjectInputStream cosObjectInputStream = null;
        try {
            COSObject cosObject = cosManager.getObject(filePath);
            cosObjectInputStream = cosObject.getObjectContent();
            // 处理下载到的流
            byte[] bytes = IOUtils.toByteArray(cosObjectInputStream);
            // 设置响应头
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + filePath);
            // 写入响应
            response.getOutputStream().write(bytes);
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("file download error, filePath = {}",filePath,e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "下载失败");
        } finally {
            if (cosObjectInputStream != null) {
                // 关闭流
                IOUtils.release(cosObjectInputStream, log);
            }
        }
    }
}
