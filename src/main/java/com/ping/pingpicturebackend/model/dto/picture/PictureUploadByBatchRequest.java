package com.ping.pingpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 图片抓取请求体
 *
 * @author ping
 */
@Data
public class PictureUploadByBatchRequest implements Serializable {

    private static final long serialVersionUID = 8390869968441930196L;

    /**
     * 搜索词
     */
    private String searchText;

    /**
     * 抓取数量
     */
    private Integer count;

    /**
     * 名称前缀
     */
    private String namePrefix;
}
