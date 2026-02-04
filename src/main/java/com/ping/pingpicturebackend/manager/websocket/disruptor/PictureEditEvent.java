package com.ping.pingpicturebackend.manager.websocket.disruptor;

import com.ping.pingpicturebackend.manager.websocket.model.PictureEditRequestMessage;
import com.ping.pingpicturebackend.model.entity.User;
import lombok.Data;
import org.springframework.web.socket.WebSocketSession;

/**
 * 图片编辑事件
 * <p>
 * 存储在 RingBuffer 中的数据对象，用于表示要传递的消息
 */
@Data
public class PictureEditEvent {

    /**
     * 消息
     */
    private PictureEditRequestMessage pictureEditRequestMessage;

    /**
     * 当前用户的 Session
     */
    private WebSocketSession session;

    /**
     * 当前用户
     */
    private User user;

    /**
     * 图片 id
     */
    private Long pictureId;
}
