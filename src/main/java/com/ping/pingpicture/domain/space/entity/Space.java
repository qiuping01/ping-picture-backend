package com.ping.pingpicture.domain.space.entity;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;

import com.ping.pingpicture.domain.space.valueobject.SpaceLevelEnum;
import com.ping.pingpicture.domain.space.valueobject.SpaceTypeEnum;
import com.ping.pingpicture.infrastructure.exception.BusinessException;
import com.ping.pingpicture.infrastructure.exception.ErrorCode;
import com.ping.pingpicture.infrastructure.exception.ThrowUtils;
import lombok.Data;

/**
 * 空间
 * @TableName space
 */
@TableName(value ="space")
@Data
public class Space implements Serializable {

    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 空间名称
     */
    private String spaceName;

    /**
     * 空间级别：0-普通版 1-专业版 2-旗舰版
     */
    private Integer spaceLevel;

    /**
     * 空间类型
     */
    private Integer spaceType;

    /**
     * 空间图片的最大总大小
     */
    private Long maxSize;

    /**
     * 空间图片的最大数量
     */
    private Long maxCount;

    /**
     * 当前空间下图片的总大小
     */
    private Long totalSize;

    /**
     * 当前空间下的图片数量
     */
    private Long totalCount;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 编辑时间
     */
    private Date editTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /**
     * 填充默认值
     */
    public void fillDefaultSpace(){
        if (StrUtil.isBlank(this.getSpaceName())) {
            this.setSpaceName("默认空间");
        }
        if (this.getSpaceLevel() == null) {
            this.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
        if (this.getSpaceType() == null) {
            this.setSpaceType(SpaceTypeEnum.PRIVATE.getValue());
        }
    }

    /**
     * 校验空间
     * @param add 是否是创建空间
     */
    public void validSpace(boolean add) {
        ThrowUtils.throwIf(this == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        String spaceName = this.getSpaceName();
        Integer spaceLevel = this.getSpaceLevel();
        Integer spaceType = this.getSpaceType();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        SpaceTypeEnum spaceTypeValue = SpaceTypeEnum.getEnumByValue(spaceType);
        // 开始校验
        // 如果是创建空间
        if (add) {
            ThrowUtils.throwIf(StrUtil.isBlank(spaceName), ErrorCode.PARAMS_ERROR, "空间名不能为空");
            ThrowUtils.throwIf(spaceLevel == null, ErrorCode.PARAMS_ERROR, "空间等级错误");
            ThrowUtils.throwIf(spaceType == null, ErrorCode.PARAMS_ERROR, "空间类型错误");
        }
        // 如果要更改空间级别
        if (spaceLevel != null && spaceLevelEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间等级不存在");
        }
        if (StrUtil.isNotBlank(spaceName) && spaceName.length() > 30) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称过长");
        }
        // 如果要更改空间类型
        if (spaceType != null && spaceTypeValue == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间类型不存在");
        }
    }
}