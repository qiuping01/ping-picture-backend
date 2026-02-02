package com.ping.pingpicturebackend.manager.websocket.model;

import com.ping.pingpicturebackend.model.vo.UserVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图片编辑响应消息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PictureEditResponseMessage {

    /**
     * 消息类型，例如 "ENTER_EDIT", "EXIT_EDIT", "EDIT_ACTION"
     */
    private String type;

    /**
     * 执行的编辑动作
     */
    private String editAction;

    /**
     * 编辑用户
     */
    private UserVO user;

    /**
     * 信息 - 例如 "用户已进入编辑模式", "用户已退出编辑模式"
     */
    private String message;
}