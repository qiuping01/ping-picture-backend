package com.ping.pingpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ping.pingpicturebackend.model.dto.picture.PictureQueryRequest;
import com.ping.pingpicturebackend.model.dto.picture.PictureReviewRequest;
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
     * 验证图片
     *
     * @param picture 图片
     */
    void validPicture(Picture picture);

    /**
     * 上传图片
     *
     * @param inputSource          输入源
     * @param pictureUploadRequest pictureId
     * @param loginUser            登录用户
     * @return PictureVO
     */
    PictureVO uploadPicture(Object inputSource,
                            PictureUploadRequest pictureUploadRequest,
                            User loginUser);

    /**
     * 构造查询 QueryWrapper
     *
     * @param pictureQueryRequest 查询请求
     * @return 查询 QueryWrapper
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    /**
     * 获取单个图片封装
     *
     * @param picture 图片
     * @return PictureVO
     */
    PictureVO getPictureVO(Picture picture);

    /**
     * 获取分页图片封装
     *
     * @param picturePage 图片分页
     * @return PictureVO分页
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage);

    /**
     * 图片审核
     *
     * @param pictureReviewRequest 审核请求
     * @param loginUser            登录用户
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    /**
     * 填充审核参数
     *
     * @param picture   图片
     * @param loginUser 登录用户
     */
    void fillReviewParams(Picture picture, User loginUser);
}
