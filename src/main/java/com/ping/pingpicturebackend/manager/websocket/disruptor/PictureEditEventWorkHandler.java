package com.ping.pingpicturebackend.manager.websocket.disruptor;

import cn.hutool.json.JSONUtil;
import com.lmax.disruptor.WorkHandler;
import com.ping.pingpicturebackend.manager.websocket.PictureEditHandler;
import com.ping.pingpicturebackend.manager.websocket.model.PictureEditMessageTypeEnum;
import com.ping.pingpicturebackend.manager.websocket.model.PictureEditRequestMessage;
import com.ping.pingpicturebackend.manager.websocket.model.PictureEditResponseMessage;
import com.ping.pingpicturebackend.model.entity.User;
import com.ping.pingpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.Resource;

/**
 * 事件处理器 - 消费者
 * <p>
 * 消费者的作用：读取并处理数据的角色
 */
@Slf4j
@Component
public class PictureEditEventWorkHandler implements WorkHandler<PictureEditEvent> {

    @Resource
    @Lazy
    private PictureEditHandler pictureEditHandler;

    @Resource
    private UserService userService;

    /**
     * 将不同类型的消息分发到对应的处理器中
     *
     * @param pictureEditEvent 图片编辑事件
     */
    @Override
    public void onEvent(PictureEditEvent pictureEditEvent) throws Exception {
        // 获取事件中的数据
        PictureEditRequestMessage pictureEditRequestMessage = pictureEditEvent.getPictureEditRequestMessage();
        WebSocketSession session = pictureEditEvent.getSession();
        User user = pictureEditEvent.getUser();
        Long pictureId = pictureEditEvent.getPictureId();
        // 获取消息类别
        String type = pictureEditRequestMessage.getType();
        PictureEditMessageTypeEnum pictureEditMessageTypeEnum = PictureEditMessageTypeEnum.getEnumByValue(type);
        // 调用对应的消息处理方法
        switch (pictureEditMessageTypeEnum) {
            case ENTER_EDIT:
                pictureEditHandler.handleEnterEditMessage(pictureEditRequestMessage, session, user, pictureId);
                break;
            case EXIT_EDIT:
                pictureEditHandler.handleExitEditMessage(pictureEditRequestMessage, session, user, pictureId);
                break;
            case EDIT_ACTION:
                pictureEditHandler.handleEditActionMessage(pictureEditRequestMessage, session, user, pictureId);
                break;
            default:
                // 其他消息类型，给当前前端返回错误提示
                PictureEditResponseMessage responseMessage = new PictureEditResponseMessage();
                responseMessage.setType(PictureEditMessageTypeEnum.ERROR.getValue());
                responseMessage.setMessage("无效的消息类型");
                responseMessage.setUser(userService.getUserVO(user));
                // 考虑到只是通知错误信息不考虑补全精度
                session.sendMessage(new TextMessage(JSONUtil.toJsonStr(responseMessage)));
        }
    }
}
