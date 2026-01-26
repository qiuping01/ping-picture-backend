package com.ping.pingpicturebackend.manager.auth.model;

/**
 * 空间成员权限常量类
 */
public interface SpaceUserPermissionConstant {

    /**
     * 空间成员管理权限
     */
    String SPACE_USER_MANAGE = "spaceUser:manage";

    /**
     * 查看图片权限
     */
    String PICTURE_VIEW = "picture:view";

    /**
     * 上传图片权限
     */
    String PICTURE_UPLOAD = "picture:upload";

    /**
     * 编辑图片权限
     */
    String PICTURE_EDIT = "picture:edit";

    /**
     * 删除图片权限
     */
    String PICTURE_DELETE = "picture:delete";
}
