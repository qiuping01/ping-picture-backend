package com.ping.pingpicturebackend.service;

import com.ping.pingpicturebackend.model.dto.picture.PictureUploadRequest;
import com.ping.pingpicturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ping.pingpicturebackend.model.entity.User;
import com.ping.pingpicturebackend.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author 21877
 * @description 针对表【picture(图片)】的数据库操作Service
 * @createDate 2025-12-21 21:52:25
 */
public interface PictureService extends IService<Picture> {

    /**
     * 上传图片
     *
     * @param file                 上传文件
     * @param pictureUploadRequest pictureId
     * @param loginUser            登录用户
     * @return PictureVO
     */
    PictureVO uploadPicture(MultipartFile file,
                            PictureUploadRequest pictureUploadRequest,
                            User loginUser);
}
