package com.ping.pingpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 图片上传请求体
 *
 * @author ping
 */
@Data
public class PictureUploadRequest implements Serializable {

    private static final long serialVersionUID = 8390869968441930196L;

    /**
     * 图片 id（用于修改）
     */
    private Long id;
}
