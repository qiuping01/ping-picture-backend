package com.ping.pingpicture.domain.picture.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.rholder.retry.Retryer;
import com.ping.pingpicture.domain.picture.repository.PictureRepository;
import com.ping.pingpicture.domain.picture.service.PictureDomainService;
import com.ping.pingpicture.infrastructure.api.aliyunai.AliYunAiApi;
import com.ping.pingpicture.infrastructure.api.aliyunai.model.CreateOutPaintingTaskRequest;
import com.ping.pingpicture.infrastructure.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.ping.pingpicture.infrastructure.api.qwen.ImageAuditWithStructuredOutput;
import com.ping.pingpicture.infrastructure.api.qwen.model.AuditImageResponse;
import com.ping.pingpicture.infrastructure.api.qwen.model.AuditImageStatusEnum;
import com.ping.pingpicture.infrastructure.exception.BusinessException;
import com.ping.pingpicture.infrastructure.exception.ErrorCode;
import com.ping.pingpicture.infrastructure.exception.ThrowUtils;
import com.ping.pingpicture.infrastructure.api.CosManager;
import com.ping.pingpicture.infrastructure.manager.upload.FilePictureUpload;
import com.ping.pingpicture.infrastructure.manager.upload.PictureUploadTemplate;
import com.ping.pingpicture.infrastructure.manager.upload.URLPictureUpload;
import com.ping.pingpicture.interfaces.dto.picture.*;
import com.ping.pingpicture.infrastructure.manager.upload.file.UploadPictureResult;
import com.ping.pingpicture.domain.picture.entity.Picture;
import com.ping.pingpicture.domain.space.entity.Space;
import com.ping.pingpicture.domain.user.entity.User;
import com.ping.pingpicture.domain.picture.valueobject.PictureReviewStatusEnum;
import com.ping.pingpicture.interfaces.vo.picture.PictureVO;
import com.ping.pingpicture.interfaces.vo.user.UserVO;
import com.ping.pingpicture.application.service.SpaceApplicationService;
import com.ping.pingpicture.application.service.UserApplicationService;
import com.ping.pingpicture.infrastructure.utils.ColorSimilarUtils;
import com.ping.pingpicture.infrastructure.utils.ColorTransformUtils;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author 21877
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2025-12-21 21:52:25
 */
@Slf4j
@Service
public class PictureDomainServiceImpl implements PictureDomainService {

    @Resource
    private PictureRepository pictureRepository;

    @Resource
    private UserApplicationService userApplicationService;

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private URLPictureUpload urlPictureUpload;

    @Resource
    private CosManager cosManager;

    @Resource
    private SpaceApplicationService spaceApplicationService;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private AliYunAiApi aliYunAiApi;

    @Resource
    private ImageAuditWithStructuredOutput imageAuditWithStructuredOutput;

    @Resource
    private TaskScheduler taskScheduler;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Resource
    private Retryer<Object> auditRetryer;

    /**
     * 上传图片
     *
     * @param inputSource          输入源
     * @param pictureUploadRequest pictureId
     * @param loginUser            登录用户
     * @return PictureVO
     */
    // todo 使用消息队列提交审核消息
    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        ThrowUtils.throwIf(inputSource == null, ErrorCode.PARAMS_ERROR, "上传文件不能为空");
        // 用于判断是新增还是更新图片
        Long pictureId = null;
        Long spaceId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
            spaceId = pictureUploadRequest.getSpaceId();
        }
        // 校验是否指定空间
        if (spaceId != null) {   // 指定空间id，则为非默认公共空间
            Space space = spaceApplicationService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR, "空间不存在");
            // 仅本人可编辑
            if (!loginUser.getId().equals(space.getUserId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间权限");
            }
            // 校验空间是否超出最大图片数量
            if (space.getTotalCount() >= space.getMaxCount()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间条数不足");
            }
            // 校验空间是否超出最大图片大小
            if (space.getTotalSize() >= space.getMaxSize()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间大小不足");
            }
        }
        // 如果是更新图片，需要校验图片是否存在
        if (pictureId != null) {
            Picture oldPicture = pictureRepository.getById(pictureId);
            ThrowUtils.throwIf(oldPicture == null, ErrorCode.PARAMS_ERROR, "图片不存在");
            // 仅本人或管理员可编辑
            if (!oldPicture.getUserId().equals(loginUser.getId()) && !loginUser.isAdmin()) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
            // 异步清理图片
            this.clearPictureFile(oldPicture);
            // 校验空间，没传 spaceId 则使用旧图片的 spaceId
            if (spaceId == null) {
                if (oldPicture.getSpaceId() != null) {
                    spaceId = oldPicture.getSpaceId();
                }
            } else {
                // 如果传了 spaceId，则校验是否一致
                if (!ObjUtil.equals(spaceId, oldPicture.getSpaceId())) { // 使用 ObjUtil.equals 防止空指针异常
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片空间不一致");
                }
            }
        }
        String uploadPathPrefix;
        if (spaceId == null) {
            // 上传图片得到信息，并按照用户 id 划分目录（利于私人图库的构建）
            uploadPathPrefix = String.format("public/%s", loginUser.getId());
        } else {
            // 指定空间就按空间 id 划分目录
            uploadPathPrefix = String.format("space/%s", spaceId);
        }
        // 根据 inputSource 类型判断上传方式
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(uploadPathPrefix, inputSource);
        // 构造图片的入库信息
        Picture picture = new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        picture.setOriginalUrl(uploadPictureResult.getOriginalUrl());
        String picName = uploadPictureResult.getPicName();
        if (pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getPicName())) {
            picName = pictureUploadRequest.getPicName();
        }
        picture.setName(picName);
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        picture.setSpaceId(spaceId);
        picture.setPicColor(ColorTransformUtils.getStandardColor(uploadPictureResult.getPicColor()));
        // 补充审核参数
        fillReviewParams(picture, loginUser);
        // 如果 pictureId 不为空，则更新图片
        if (pictureId != null) {
            // 如果是更新，需要补充 id 和编辑时间
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        // 开启事务 - 更新空间额度
        Long finalSpaceId = spaceId;
        transactionTemplate.execute(status -> {
            // 保存图片信息
            boolean result = pictureRepository.saveOrUpdate(picture);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "上传图片失败");
            if (finalSpaceId != null) {
                boolean updateResult = spaceApplicationService.lambdaUpdate()
                        .eq(Space::getId, finalSpaceId)
                        .setSql("totalCount = totalCount + 1")
                        .setSql("totalSize = totalSize + " + picture.getPicSize())
                        .update();
                ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "额度更新失败");
            }
            return picture; // 此处返回图片信息，saveOrUpdate 会更新 picture 对象（比如设置 ID）
        });

        // 直接同步调用
        final Long finalPictureId = picture.getId();
        final String finalPictureUrl = picture.getUrl();
        // 使用 ai 异步审核图片
        aiPictureReview(finalPictureUrl, finalPictureId, loginUser);

        return PictureVO.objToVo(picture);
    }

    /**
     * 构造查询 QueryWrapper
     *
     * @param pictureQueryRequest 查询请求
     * @return 查询 QueryWrapper
     */
    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest
                                                         pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) {
            return queryWrapper;
        }
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        Long spaceId = pictureQueryRequest.getSpaceId();
        Boolean nullSpaceId = pictureQueryRequest.getNullSpaceId();
        Date startEditTime = pictureQueryRequest.getStartEditTime();
        Date endEditTime = pictureQueryRequest.getEndEditTime();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();

        // 从多字段中搜索
        if (StrUtil.isNotBlank(searchText)) {
            queryWrapper.and(qw -> qw
                    .like("name", searchText)
                    .or()
                    .like("introduction", searchText)
            );
        }
        // 单字段搜索
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id)
                .eq(ObjUtil.isNotEmpty(userId), "userId", userId)
                .eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId)
                .eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus)
                .eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId)
                .isNull(nullSpaceId, "spaceId")
                .like(StrUtil.isNotBlank(name), "name", name)
                .like(StrUtil.isNotBlank(introduction), "introduction", introduction)
                .like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat)
                .like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage)
                .eq(StrUtil.isNotBlank(category), "category", category)
                .eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth)
                .eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight)
                .eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize)
                .eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale)
                // >= startEditTime
                .ge(ObjUtil.isNotEmpty(startEditTime), "editTime", startEditTime)
                // < endEditTime
                .lt(ObjUtil.isNotEmpty(endEditTime), "editTime", endEditTime);
        // 标签搜索
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    /**
     * 获取单个图片封装
     *
     * @param picture 图片
     * @return PictureVO
     */
    @Override
    public PictureVO getPictureVO(Picture picture) {
        PictureVO pictureVO = PictureVO.objToVo(picture);
        if (pictureVO == null) {
            return null;
        }
        // 关联查询用户信息
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userApplicationService.getUserById(userId);
            UserVO userVO = userApplicationService.getUserVO(user);
            pictureVO.setUser(userVO);
        }
        return pictureVO;
    }

    /**
     * 图片审核
     *
     * @param pictureReviewRequest 审核请求
     * @param loginUser            登录用户
     */
    // todo 改造为消息队列消费者
    @Override
    public void doPictureReview(PictureReviewRequest
                                        pictureReviewRequest, User loginUser) {
        // 参数校验
        Long picId = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        PictureReviewStatusEnum reviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        if (picId == null || reviewStatusEnum == null ||
                PictureReviewStatusEnum.REVIEWING.equals(reviewStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "审核失败，参数错误");
        }
        // 判断是否存在
        Picture oldPicture = pictureRepository.getById(picId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        // 判断是否已是该状态
        Integer oldPictureReviewStatus = oldPicture.getReviewStatus();
        PictureReviewStatusEnum oldReviewStatusEnum =
                PictureReviewStatusEnum.getEnumByValue(oldPictureReviewStatus);
        if (reviewStatusEnum.equals(oldReviewStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请勿重复审核");
        }
        // 执行审核
        Picture updatePicture = new Picture();
        BeanUtils.copyProperties(pictureReviewRequest, updatePicture);
        updatePicture.setReviewerId(loginUser.getId());
        updatePicture.setReviewTime(new Date());
        boolean result = pictureRepository.updateById(updatePicture);
        ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR, "审核失败");
    }

    /**
     * AI 自动审图
     *
     * @param imageUrl  图片 url
     * @param picId     图片 id
     * @param loginUser 登录用户
     */
    @Override
    public void aiPictureReview(String imageUrl, Long picId, User loginUser) {
        if (loginUser.isAdmin()) {
            return;
        }
        // 异步执行审核，不阻塞主线程
        CompletableFuture.runAsync(() -> {
            try {
                // 1. 调用 AI 审核接口
                // 使用 Retryer 包装需要重试的逻辑
                AuditImageResponse auditImageResponse = (AuditImageResponse) auditRetryer.call(() -> {
                    log.info("开始 AI 审核，图片ID: {}", picId);
                    return imageAuditWithStructuredOutput.auditImage(imageUrl);
                });
                String isPass = auditImageResponse.getIsPass();
                String noPassReason = auditImageResponse.getNoPassReason();
                String description = auditImageResponse.getDescription();
                List<String> tags = auditImageResponse.getTags();
                String category = auditImageResponse.getCategory();

                // 2. 更新图片状态
                // 执行审核
                Picture updatePicture = new Picture();
                updatePicture.setId(picId);
                updatePicture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
                updatePicture.setReviewMessage("AI 自动过审");
                final long aiReviewerId = 943615287620148396L;
                updatePicture.setReviewerId(aiReviewerId);
                updatePicture.setReviewTime(new Date());
                // 附带 AI 信息 - 先查询是否已经存在用户编辑痕迹
                Picture oldPicture = pictureRepository.getById(picId);
                if (oldPicture == null) {
                    log.error("图片不存在，无法回填 AI 信息，图片ID: {}", picId);
                    return;
                }
                String oldIntroduction = oldPicture.getIntroduction();
                String oldTags = oldPicture.getTags();
                String oldCategory = oldPicture.getCategory();
                // 只更新用户未填写的字段
                if (StrUtil.isBlank(oldIntroduction)) {
                    updatePicture.setIntroduction(description);
                }
                if (StrUtil.isBlank(oldTags)) {
                    updatePicture.setTags(JSONUtil.toJsonStr(tags));
                }
                if (StrUtil.isBlank(oldCategory)) {
                    updatePicture.setCategory(category);
                }

                // 图片内容不安全则不通过审核
                if (StrUtil.isNotBlank(noPassReason)) {
                    log.info("图片内容不安全，原因：{}", noPassReason);
                    updatePicture.setReviewMessage(noPassReason);
                    updatePicture.setReviewStatus(PictureReviewStatusEnum.REJECT.getValue());
                }
                boolean result = pictureRepository.updateById(updatePicture);
                ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR, "审核失败");

                // 3. 如果图片不安全，延迟删除
                if (AuditImageStatusEnum.NO.getValue().equals(isPass)) {
                    log.warn("图片内容不安全，将在1分钟后自动删除，图片ID：{}", picId);
                    // 延迟 1分钟执行删除
                    scheduleDelayedDelete(picId, loginUser, 1, TimeUnit.MINUTES);
//                    throw new BusinessException(ErrorCode.PICTURE_CONTENT_UNSAFE);
                }

            } catch (Exception e) {
                // 4. 处理所有重试都失败的最终场景
                log.error("AI 自动审图最终失败，图片ID: {}", picId, e);
                Picture fallbackPicture = new Picture();
                fallbackPicture.setId(picId);
                fallbackPicture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
                fallbackPicture.setReviewMessage("AI审核服务异常，转人工处理");
                pictureRepository.updateById(fallbackPicture);
            }
        }, threadPoolExecutor);
    }

    /**
     * 延迟删除图片
     *
     * @param picId     图片ID
     * @param loginUser 登录用户
     * @param delay     延迟时间
     * @param unit      时间单位
     */
    private void scheduleDelayedDelete(Long picId, User loginUser, long delay, TimeUnit unit) {
        taskScheduler.schedule(() -> {
            try {
                log.info("开始执行延迟删除，图片ID：{}，延迟时间：{} {}", picId, delay, unit);
                // 删除前再次确认图片状态，避免误删
                Picture picture = pictureRepository.getById(picId);
                PictureReviewStatusEnum pictureReviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(picture.getReviewStatus());
                if (picture != null && PictureReviewStatusEnum.REJECT.equals(pictureReviewStatusEnum)) {
                    this.deletePicture(picId, loginUser);
                    log.info("延迟删除成功，图片ID：{}", picId);
                } else {
                    log.info("图片状态已变更，跳过删除，图片ID：{}", picId);
                }
            } catch (Exception e) {
                log.error("延迟删除失败，图片ID：{}", picId, e);
            }
        }, new Date(System.currentTimeMillis() + unit.toMillis(delay)));
    }


    /**
     * 填充审核参数
     *
     * @param picture   图片
     * @param loginUser 登录用户
     */
    // todo 1.可优化为先过审再复审  2.管理员无需复审
    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        // 管理员自动过审
        if (loginUser.isAdmin()) {
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewerId(loginUser.getId());
            picture.setReviewMessage("管理员自动过审");
            picture.setReviewTime(new Date());
        }
        // 普通用户自动待审（创建 / 编辑）
        else {
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
            // todo 此处可补充复审逻辑，并向消息队列发送审核消息
        }
    }

    /**
     * 批量抓取图片和创建图片
     *
     * @param pictureUploadByBatchRequest 批量上传请求
     * @param loginUser                   登录用户
     * @return int  成功创建的图片数
     */
    @Override
    public int uploadPictureByBatch(PictureUploadByBatchRequest
                                            pictureUploadByBatchRequest, User loginUser) {
        ThrowUtils.throwIf(StrUtil.isBlank(pictureUploadByBatchRequest.getSearchText()) ||
                pictureUploadByBatchRequest.getCount() == null, ErrorCode.PARAMS_ERROR, "搜索内容不能为空");
        // 1. 格式化数量
        String searchText = pictureUploadByBatchRequest.getSearchText();
        String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
        if (StrUtil.isBlank(namePrefix)) {
            namePrefix = searchText;
        }
        int count = pictureUploadByBatchRequest.getCount();
        ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR, "一次最多抓取30张图片");
        // 2. 从抓取地址获取页面
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        Document document;
        try {
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("获取页面失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取页面失败");
        }
        // 3. 获取元素
        Element div = document.getElementsByClass("dgControl").first();
        if (ObjUtil.isNull(div)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取元素失败");
        }
        Elements imgElementList = div.select("img.mimg");
        // 4. 处理图片上传地址，防止出现转义问题
        int uploadCount = 0;
        for (Element imgElement : imgElementList) {
            String imgUrl = imgElement.attr("src");
            if (StrUtil.isBlank(imgUrl)) {
                log.info("当前链接为空，已跳过：{}", imgUrl);
                continue;
            }
            int questionMarkIndex = imgUrl.indexOf("?");
            if (questionMarkIndex != -1) {
                imgUrl = imgUrl.substring(0, questionMarkIndex);
            }
            // 5. 上传图片
            try {
                PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
                if (StrUtil.isNotBlank(namePrefix)) {
                    // 设置图片名称，序号连续递增
                    pictureUploadRequest.setPicName(namePrefix + (uploadCount + 1));
                }
                PictureVO pictureVO = this.uploadPicture(imgUrl, pictureUploadRequest, loginUser);
                log.info("成功上传图片：id = {}", pictureVO.getId());
                uploadCount++;
            } catch (Exception e) { // 防止一个图片上传 COS 失败导致整个流程失败
                log.error("上传图片失败", e);
                continue;
            }
            if (uploadCount >= count) {
                break;
            }
        }
        return uploadCount;
    }

    /**
     * 清除图片文件
     *
     * @param oldPicture 旧图片
     */
    @Async
    @Override
    public void clearPictureFile(Picture oldPicture) {
        // 判断该图片是否被多条记录使用
        String pictureUrl = oldPicture.getUrl();
        long count = pictureRepository.lambdaQuery()
                .eq(Picture::getUrl, pictureUrl)
                .count();
        // 有不止一条记录用到了该图片，不清理
        if (count > 1) {
            return;
        }
        // 删除图片文件
        // 解析 key
        cosManager.deleteObject(this.getKeyFromUrl(pictureUrl));
        // 清理缩略图
        String thumbnailUrl = oldPicture.getThumbnailUrl();
        if (StrUtil.isNotBlank(thumbnailUrl)) {
            cosManager.deleteObject(this.getKeyFromUrl(thumbnailUrl));
        }
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
        Long spaceId = picture.getSpaceId();
        if (spaceId == null) {
            // 公共图库仅本人和管理员能操作
            if (!picture.getUserId().equals(loginUser.getId()) && loginUser.isAdmin()) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有权限");
            }
        } else {
            // 私有空间仅本人能操作
            if (!picture.getUserId().equals(loginUser.getId()))
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有权限");
        }
    }

    /**
     * 删除图片
     *
     * @param picId     图片 id
     * @param loginUser 登录用户
     */
    @Override
    public void deletePicture(Long picId, User loginUser) {
        ThrowUtils.throwIf(picId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        // 判断图片是否存在
        Picture oldPicture = pictureRepository.getById(picId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        // 校验操作权限
        // 已经改为使用注解鉴权
//        this.checkPictureAuth(loginUser, oldPicture);
        // 开启事务 - 更新空间额度
        transactionTemplate.executeWithoutResult(status -> {
            // 操作数据库 - 删除图片信息
            boolean result = pictureRepository.removeById(picId);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "删除失败");
            // 释放额度
            Long finalSpaceId = oldPicture.getSpaceId();
            if (finalSpaceId != null) {
                boolean updateResult = spaceApplicationService.lambdaUpdate()
                        .eq(Space::getId, finalSpaceId)
                        .setSql("totalCount = totalCount - 1")
                        .setSql("totalSize = totalSize - " + oldPicture.getPicSize())
                        .update();
                ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "额度更新失败");
            }
        });
        // 异步清理文件
        this.clearPictureFile(oldPicture);
    }

    /**
     * 编辑图片
     *
     * @param picture   图片
     * @param loginUser 登录用户
     */
    @Override
    public void editPicture(Picture picture, User loginUser) {
        ThrowUtils.throwIf(picture == null || picture.getId() <= 0,
                ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        // 判断图片是否存在
        Picture oldPicture = pictureRepository.getById(picture.getId());
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        // 图片校验
        picture.validPicture();
        // 设置编辑时间
        picture.setUpdateTime(new Date());
        // 校验图片空间
        // 已经改为使用注解鉴权
//        this.checkPictureAuth(loginUser, oldPicture);
        // 补充审核参数 - 已使用 AI 审图，无需再次填充审核参数
//        this.fillReviewParams(picture, loginUser);
        // 操作数据库
        boolean result = pictureRepository.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "更新失败");
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
        // 取参
        List<Long> pictureIdList = pictureEditByBatchRequest.getPictureIdList();
        Long spaceId = pictureEditByBatchRequest.getSpaceId();
        String category = pictureEditByBatchRequest.getCategory();
        List<String> tags = pictureEditByBatchRequest.getTags();
        String nameRule = pictureEditByBatchRequest.getNameRule();
        // 1. 校验必填参数
        ThrowUtils.throwIf(CollUtil.isEmpty(pictureIdList), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(spaceId == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        // 2. 校验空间权限
        Space space = spaceApplicationService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        if (!space.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间访问权限");
        }
        // 3. 查询指定图片，仅选择需要的字段
        List<Picture> pictureList = pictureRepository.lambdaQuery()
                .select(Picture::getId, Picture::getSpaceId) // 提高查询效率
                .eq(Picture::getSpaceId, spaceId)
                .in(Picture::getId, pictureIdList)
                .list();
        if (CollUtil.isEmpty(pictureList)) {
            return;
        }
        // 4.1. 批量更新分类和标签
        pictureList.forEach(picture -> {
            if (StrUtil.isNotBlank(category)) {
                picture.setCategory(category);
            }
            if (CollUtil.isNotEmpty(tags)) {
                picture.setTags(JSONUtil.toJsonStr(tags));
            }
        });
        // 4.2. 批量重命名
        fillPictureWithNameRule(pictureList, nameRule);
        // 5. 操作数据库批量更新
        boolean result = pictureRepository.updateBatchById(pictureList);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "批量更新失败");
    }

    /**
     * 根据命名规则批量重命名图片
     * nameRule 格式：图片{序号}
     *
     * @param pictureList 图片列表
     * @param nameRule    命名规则
     */
    private void fillPictureWithNameRule(List<Picture> pictureList, String nameRule) {
        if (CollUtil.isEmpty(pictureList) || StrUtil.isBlank(nameRule)) {
            return;
        }
        long count = 1;
        try {
            for (Picture picture : pictureList) {
                String pictureName = nameRule.replaceAll("\\{序号}", String.valueOf(count++));
                picture.setName(pictureName);
            }
        } catch (Exception e) { // 涉及正则表达式的匹配，可能会出错
            log.error("名称解析错误", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "名称解析错误");
        }
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
        // 1. 校验参数
        ThrowUtils.throwIf(picColor == null || picColor.isEmpty(), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(spaceId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        // 2. 校验空间权限
        Space space = spaceApplicationService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        if (!space.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有权限");
        }
        // 3. 查询该空间下所有图片（必须有主色调）
        List<Picture> pictureList = pictureRepository.lambdaQuery()
                .eq(Picture::getSpaceId, spaceId)
                .isNotNull(Picture::getPicColor)
                .list();
        if (CollUtil.isEmpty(pictureList)) {
            return Collections.emptyList();
        }
        // 将目标颜色转为 Color 对象
        Color targetColor = Color.decode(picColor);
        // 4. 计算相似度并排序
        List<PictureVO> pictureVOList = pictureList.stream()
                .sorted(Comparator.comparingDouble(picture -> {
                    // 获取图片主色调
                    String hexColor = picture.getPicColor();
                    // 没有主色调的放到最后
                    if (hexColor == null) {
                        return Double.MAX_VALUE;
                    }
                    // 转为 Color 对象
                    Color pictureColor = Color.decode(hexColor);
                    // 计算相似度 - 越大越相似
                    return ColorSimilarUtils.calculateSimilarity(targetColor, pictureColor);
                }))
                // 取前 10 个
                .limit(10)
                // 转换为 PictureVO
                .map(PictureVO::objToVo)
                .collect(Collectors.toList());
        return pictureVOList;
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
        // 获取图片信息
        Long pictureId = createPictureOutPaintingTaskRequest.getPictureId();
        Picture picture = Optional.ofNullable(pictureRepository.getById(pictureId))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在"));
        // 权限校验
        // 已经改为使用注解鉴权
//        checkPictureAuth(loginUser,picture);
        // 构造扩图请求参数
        CreateOutPaintingTaskRequest taskRequest = new CreateOutPaintingTaskRequest();
        CreateOutPaintingTaskRequest.Input input = new CreateOutPaintingTaskRequest.Input();
        input.setImageUrl(picture.getUrl());
        taskRequest.setInput(input);
        BeanUtils.copyProperties(createPictureOutPaintingTaskRequest, taskRequest);
        // 执行扩图
        return aliYunAiApi.createOutPaintingTask(taskRequest);
    }
}




