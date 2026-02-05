package com.ping.pingpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ping.pingpicture.infrastructure.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.ping.pingpicturebackend.model.dto.picture.*;
import com.ping.pingpicturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ping.pingpicturebackend.model.entity.User;
import com.ping.pingpicturebackend.model.vo.PictureVO;

import java.util.List;

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

    /**
     * 批量抓取图片和创建图片
     *
     * @param pictureUploadByBatchRequest 批量上传请求
     * @param loginUser                   登录用户
     * @return int  成功创建的图片数
     */
    int uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser);

    /**
     * 清除图片文件
     *
     * @param oldPicture 旧图片
     */
    void clearPictureFile(Picture oldPicture);

    /**
     * 从 url 中解析 key
     *
     * @param url url
     */
    String getKeyFromUrl(String url);

    /**
     * 校验图片空间权限
     *
     * @param loginUser 登录用户
     * @param picture   图片
     */
    void checkPictureAuth(User loginUser, Picture picture);

    /**
     * 删除图片
     *
     * @param picId     图片 id
     * @param loginUser 登录用户
     */
    void deletePicture(Long picId, User loginUser);

    /**
     * 编辑图片
     *
     * @param pictureEditRequest 编辑请求
     * @param loginUser          登录用户
     */
    void editPicture(PictureEditRequest pictureEditRequest, User loginUser);

    /**
     * 批量编辑图片
     *
     * @param pictureEditByBatchRequest 批量编辑请求
     * @param loginUser                 登录用户
     */
    void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser);

    /**
     * 根据颜色搜索图片
     *
     * @param picColor  颜色
     * @param spaceId   空间id
     * @param loginUser 登录用户
     * @return List<PictureVO>
     */
    List<PictureVO> searchPictureByColor(String picColor, Long spaceId, User loginUser);

    /**
     * 扩图
     *
     * @param createPictureOutPaintingTaskRequest 扩图请求
     * @param loginUser                           登录用户
     * @return 扩图任务响应类
     */
    CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest,
                                                               User loginUser);
}
