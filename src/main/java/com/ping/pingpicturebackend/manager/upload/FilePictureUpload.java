package com.ping.pingpicturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import com.ping.pingpicturebackend.exception.ErrorCode;
import com.ping.pingpicturebackend.exception.ThrowUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * 本地文件上传
 */
@Service
public class FilePictureUpload extends PictureUploadTemplate {

    @Override
    protected void validPicture(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR, "上传文件不能为空");
        // 校验文件大小
        long fileSize = multipartFile.getSize();
        final long ONE_MB = 1024 * 1024;
        ThrowUtils.throwIf(fileSize > ONE_MB * 5, ErrorCode.PARAMS_ERROR, "上传文件大小不能超过5MB");
        // 校验文件类型
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        // 允许上传的文件后缀列表
        final List<String> ALLOW_FORMAT_LIST = Arrays.asList("jpg", "png", "jpeg", "gif", "bmp", "webp");
        ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(fileSuffix), ErrorCode.PARAMS_ERROR, "上传文件类型错误");
    }

    @Override
    protected String getOriginalFilename(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        return multipartFile.getOriginalFilename();
    }

    @Override
    protected void processFile(Object inputSource, File file) throws Exception {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        multipartFile.transferTo(file);
    }
}
