package com.ping.pingpicturebackend.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.ping.pingpicture.infrastructure.common.BaseResponse;
import com.ping.pingpicture.infrastructure.common.ResultUtils;
import com.ping.pingpicturebackend.constant.UserConstant;
import com.ping.pingpicture.infrastructure.exception.BusinessException;
import com.ping.pingpicture.infrastructure.exception.ErrorCode;
import com.ping.pingpicture.infrastructure.api.CosManager;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 * 文件上传检查接口
 */
@Slf4j
@RestController
@RequestMapping("/file")
public class FileController {

    @Resource
    private CosManager cosManager;

    /**
     * 文件上传检查
     *
     * @param multipartFile 文件
     * @return URL
     */
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @PostMapping("/test/upload")
    public BaseResponse<String> testUploadFile(@RequestPart("file") MultipartFile multipartFile) {
        // COS 文件目录
        String filename = multipartFile.getOriginalFilename();
        String filePath = String.format("/ping_pic/test/%s", filename);
        File file = null;
        try {
            // 上传文件
            file = File.createTempFile(filePath, null);
            multipartFile.transferTo(file);
            // 上传到cos
            cosManager.putObject(filePath, file);
            // 返回可访问的地址
            String fileURL = "https://img-1372837879.cos.ap-guangzhou.myqcloud.com" + filePath;
            return ResultUtils.success(fileURL);
        } catch (Exception e) {
            log.error("文件上传失败, filepath = " + filePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件上传失败");
        } finally {
            if (file != null) {
                // 删除临时文件
                boolean delete = file.delete();
                if (!delete) {
                    log.error("文件删除失败, filepath = {}", filePath);
                }
            }
        }
    }

    /**
     * 文件下载检查
     *
     * @param filepath 文件路径
     * @param response 响应对象
     */
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @GetMapping("/test/download")
    public void testDownloadFile(String filepath, HttpServletResponse response) throws IOException {
        COSObjectInputStream cosObjectInput = null;
        try {
            // 从cos下载文件
            COSObject cosObject = cosManager.getObject(filepath);
            cosObjectInput = cosObject.getObjectContent();
            byte[] bytes = IOUtils.toByteArray(cosObjectInput);
            // 设置响应头
            response.setContentType("application/octet-stream;charset=utf-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + filepath);
            // 写入响应
            response.getOutputStream().write(bytes);
            response.getOutputStream().flush();
        } catch (IOException e) {
            log.error("文件下载失败, filepath = " + filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件下载失败");
        } finally {
            // 释放流
            if (cosObjectInput != null) {
                cosObjectInput.close();
            }
        }
    }
}


