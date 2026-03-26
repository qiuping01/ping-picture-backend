package com.ping.pingpicture.application.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.net.url.UrlBuilder;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ping.pingpicture.application.service.UserApplicationService;
import com.ping.pingpicture.domain.picture.entity.Picture;
import com.ping.pingpicture.domain.picture.service.PictureDomainService;
import com.ping.pingpicture.domain.user.entity.User;
import com.ping.pingpicture.infrastructure.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.ping.pingpicture.infrastructure.exception.BusinessException;
import com.ping.pingpicture.infrastructure.exception.ErrorCode;
import com.ping.pingpicture.infrastructure.mapper.PictureMapper;
import com.ping.pingpicture.interfaces.dto.picture.*;
import com.ping.pingpicture.interfaces.vo.picture.PictureVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author 21877
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2025-12-21 21:52:25
 */
@Slf4j
@Service
public class PictureApplicationServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements com.ping.pingpicture.application.service.PictureApplicationService {

    @Resource
    private PictureDomainService pictureDomainService;

    @Resource
    private UserApplicationService userApplicationService;

    /**
     * 上传图片
     *
     * @param inputSource          输入源
     * @param pictureUploadRequest pictureId
     * @param loginUser            登录用户
     * @return PictureVO
     */
    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        return pictureDomainService.uploadPicture(inputSource, pictureUploadRequest, loginUser);
    }

    /**
     * 构造查询 QueryWrapper
     *
     * @param pictureQueryRequest 查询请求
     * @return 查询 QueryWrapper
     */
    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        return pictureDomainService.getQueryWrapper(pictureQueryRequest);
    }

    /**
     * 获取单个图片封装
     *
     * @param picture 图片
     * @return PictureVO
     */
    @Override
    public PictureVO getPictureVO(Picture picture) {
        return pictureDomainService.getPictureVO(picture);
    }

    /**
     * 获取分页图片封装
     *
     * @param picturePage 图片分页
     * @return PictureVO分页
     */
    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage) {
        // 拿到当前页数据
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>
                (picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        // 1. 转换为VO
        List<PictureVO> pictureVOList = pictureList.stream()
                .map(PictureVO::objToVo)
                .collect(Collectors.toList());
        // 2. 提取不重复的userId（Set去重）
        Set<Long> userIdSet = pictureList.stream()
                .map(Picture::getUserId)
                .filter(Objects::nonNull)  // 过滤null值
                .collect(Collectors.toSet());
        // 3. 批量查询用户
        Map<Long, List<User>> userIdUserListMap = userApplicationService.listByIds(userIdSet)
                .stream()
                .collect(Collectors.groupingBy(User::getId));
        // 4. 填充用户信息
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            pictureVO.setUser(userApplicationService.getUserVO(user));
        });
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

    @Override
    public void aiPictureReview(String imageUrl, Long picId, User loginUser) {
        pictureDomainService.aiPictureReview(imageUrl, picId, loginUser);
    }

    /**
     * 图片审核
     *
     * @param pictureReviewRequest 审核请求
     * @param loginUser            登录用户
     */
    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        pictureDomainService.doPictureReview(pictureReviewRequest, loginUser);
    }

    /**
     * 填充审核参数
     *
     * @param picture   图片
     * @param loginUser 登录用户
     */
    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        pictureDomainService.fillReviewParams(picture, loginUser);
    }

    /**
     * 批量抓取图片和创建图片
     *
     * @param pictureUploadByBatchRequest 批量上传请求
     * @param loginUser                   登录用户
     * @return int  成功创建的图片数
     */
    @Override
    public int uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        return pictureDomainService.uploadPictureByBatch(pictureUploadByBatchRequest, loginUser);
    }

    /**
     * 清除图片文件
     *
     * @param oldPicture 旧图片
     */
    @Async
    @Override
    public void clearPictureFile(Picture oldPicture) {
        pictureDomainService.clearPictureFile(oldPicture);
    }

    /**
     * 从 url 中解析 key
     *
     * @param url url
     */
    @Override
    public String getKeyFromUrl(String url) {
        // 解析 key
        UrlBuilder urlBuilder = UrlBuilder.ofHttp(url);
        String key = urlBuilder.getPathStr(); // 获取路径部分
        // 去掉开头的斜杠
        if (key.startsWith("/")) {
            key = key.substring(1);
        }
        return key;
    }

    /**
     * 校验图片空间权限
     *
     * @param loginUser 登录用户
     * @param picture   图片
     */
    @Override
    public void checkPictureAuth(User loginUser, Picture picture) {
        pictureDomainService.checkPictureAuth(loginUser, picture);
    }

    /**
     * 删除图片
     *
     * @param picId     图片 id
     * @param loginUser 登录用户
     */
    @Override
    public void deletePicture(Long picId, User loginUser) {
        pictureDomainService.deletePicture(picId, loginUser);
    }

    /**
     * 编辑图片
     *
     * @param picture   图片
     * @param loginUser 登录用户
     */
    @Override
    public void editPicture(Picture picture, User loginUser) {
        pictureDomainService.editPicture(picture, loginUser);
    }

    /**
     * 批量编辑图片
     *
     * @param pictureEditByBatchRequest 批量编辑请求
     * @param loginUser                 登录用户
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser) {
        pictureDomainService.editPictureByBatch(pictureEditByBatchRequest, loginUser);
    }

    /**
     * 根据颜色搜索图片
     *
     * @param picColor  颜色
     * @param spaceId   空间id
     * @param loginUser 登录用户
     * @return List<PictureVO>
     */
    @Override
    public List<PictureVO> searchPictureByColor(String picColor, Long spaceId, User loginUser) {
        return pictureDomainService.searchPictureByColor(picColor, spaceId, loginUser);
    }

    /**
     * 扩图
     *
     * @param createPictureOutPaintingTaskRequest 扩图请求
     * @param loginUser                           登录用户
     * @return 扩图任务响应类
     */
    @Override
    public CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser) {
        return pictureDomainService.createPictureOutPaintingTask(createPictureOutPaintingTaskRequest, loginUser);
    }

    @Override
    public void validPicture(Picture picture) {
        if (picture == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        picture.validPicture();
    }
}




