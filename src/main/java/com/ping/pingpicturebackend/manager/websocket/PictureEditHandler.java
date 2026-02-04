package com.ping.pingpicturebackend.manager.websocket;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.ping.pingpicturebackend.manager.websocket.disruptor.PictureEditEventProducer;
import com.ping.pingpicturebackend.manager.websocket.model.PictureEditActionEnum;
import com.ping.pingpicturebackend.manager.websocket.model.PictureEditMessageTypeEnum;
import com.ping.pingpicturebackend.manager.websocket.model.PictureEditRequestMessage;
import com.ping.pingpicturebackend.manager.websocket.model.PictureEditResponseMessage;
import com.ping.pingpicturebackend.model.entity.User;
import com.ping.pingpicturebackend.service.UserService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 图片编辑处理器
 */
@Component
public class PictureEditHandler extends TextWebSocketHandler {

    @Resource
    private UserService userService;

    @Resource
    private PictureEditEventProducer pictureEditEventProducer;

    /**
     * 每张图片的编辑状态
     * <p>
     * key: PictureId，value: 当前正在编辑的用户 ID
     */
    private final Map<Long, Long> pictureEditingUsers = new ConcurrentHashMap<>();

    /**
     * 保存所有连接的会话
     * <p>
     * key: PictureId，value: 用户会话集合
     */
    private final Map<Long, Set<WebSocketSession>> pictureEditingSessions = new ConcurrentHashMap<>();

    /**
     * 连接建立后调用 - 保存会话到集合中，并且给其他会话发送消息
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 获取参数
        User user = (User) session.getAttributes().get("user");
        Long pictureId = (Long) session.getAttributes().get("pictureId");
        // 首次加入集合初始化后加入属性
        pictureEditingSessions.putIfAbsent(pictureId, ConcurrentHashMap.newKeySet());
        pictureEditingSessions.get(pictureId).add(session);
        // 构造响应
        PictureEditResponseMessage responseMessage = new PictureEditResponseMessage();
        responseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        String message = String.format("%s加入编辑", user.getUserName());
        responseMessage.setMessage(message);
        responseMessage.setUser(userService.getUserVO(user));
        // 广播给同一张图片的用户
        broadcastToPicture(pictureId, responseMessage);
    }

    /**
     * 处理接收的客户端消息
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 获取消息内容，将 JSON 转换为 PictureRequestEditMessage
        PictureEditRequestMessage pictureEditRequestMessage = JSONUtil.toBean(message.getPayload(), PictureEditRequestMessage.class);
        String type = pictureEditRequestMessage.getType();
        PictureEditMessageTypeEnum pictureEditMessageTypeEnum = PictureEditMessageTypeEnum.getEnumByValue(type);
        // 从 Session 属性中获取公共参数
        User user = (User) session.getAttributes().get("user");
        Long pictureId = (Long) session.getAttributes().get("pictureId");
        // 调用对应的消息处理方法 - 改为使用事件生产者
        // 生产消息
        pictureEditEventProducer.publishEvent(pictureEditRequestMessage, session, user, pictureId);
    }

    /**
     * 关闭会话移除信息
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // 获取参数
        User user = (User) session.getAttributes().get("user");
        Long pictureId = (Long) session.getAttributes().get("pictureId");
        // 移除当前用户的编辑状态
        handleExitEditMessage(null, session, user, pictureId);
        // 删除会话
        Set<WebSocketSession> sessionSet = pictureEditingSessions.get(pictureId);
        if (sessionSet == null) {
            return;
        }
        sessionSet.remove(session);
        if (sessionSet.isEmpty()) {
            pictureEditingSessions.remove(pictureId);
        }
        // 构造响应
        PictureEditResponseMessage responseMessage = new PictureEditResponseMessage();
        responseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        String message = String.format("%s离开编辑", user.getUserName());
        responseMessage.setMessage(message);
        responseMessage.setUser(userService.getUserVO(user));
        // 广播给同一张图片的用户
        broadcastToPicture(pictureId, responseMessage);
    }

    /**
     * 广播给该图片的所有用户 - 支持排除某个 session
     */
    private void broadcastToPicture(Long pictureId, PictureEditResponseMessage responseMessage,
                                    WebSocketSession excludeSession) throws JsonProcessingException, IOException {
        Set<WebSocketSession> sessionSet = pictureEditingSessions.get(pictureId);
        if (CollUtil.isEmpty(sessionSet)) {
            return;
        }
        // 创建Jackson的ObjectMapper实例
        ObjectMapper objectMapper = new ObjectMapper();
        // 配置序列化：将 Long 类型转为 String，解决丢失精度问题
        // 创建 SimpleModule 用于自定义序列化规则
        SimpleModule module = new SimpleModule();
        // 配置 Long 类型序列化为字符串
        // Long.class 对应包装类型 Long
        module.addSerializer(Long.class, ToStringSerializer.instance);
        // Long.TYPE对应基本类型long（防止遗漏）
        module.addSerializer(Long.TYPE, ToStringSerializer.instance); // 支持 long 基本类型
        // 将模块注册到ObjectMapper
        objectMapper.registerModule(module);
        // 序列化为 JSON 字符串
        String messageJsonStr = objectMapper.writeValueAsString(responseMessage);
        TextMessage textMessage = new TextMessage(messageJsonStr);
        for (WebSocketSession session : sessionSet) {
            // 排除掉的 session 不发送消息
            if (excludeSession != null && excludeSession.equals(session)) {
                continue;
            }
            if (session.isOpen()) {
                session.sendMessage(textMessage);
            }
        }
    }

    /**
     * 广播给该图片的所有用户 - 全部广播
     */
    private void broadcastToPicture(Long pictureId, PictureEditResponseMessage responseMessage) throws Exception {
        broadcastToPicture(pictureId, responseMessage, null);
    }

    /**
     * 进入编辑状态
     */
    public void handleEnterEditMessage(PictureEditRequestMessage pictureEditRequestMessage,
                                       WebSocketSession session, User user, Long pictureId) throws Exception {
        // 没有用户正在编辑该图片，才能进入编辑
        if (pictureEditingUsers.containsKey(pictureId)) {
            return;
        }
        // 设置当前用户为编辑用户
        pictureEditingUsers.put(pictureId, user.getId());
        // 构造响应
        PictureEditResponseMessage responseMessage = new PictureEditResponseMessage();
        responseMessage.setType(PictureEditMessageTypeEnum.ENTER_EDIT.getValue());
        String message = String.format("%s开始编辑图片", user.getUserName());
        responseMessage.setMessage(message);
        responseMessage.setUser(userService.getUserVO(user));
        // 广播给所有用户
        broadcastToPicture(pictureId, responseMessage);
    }

    /**
     * 处理编辑操作
     */
    public void handleEditActionMessage(PictureEditRequestMessage pictureEditRequestMessage,
                                        WebSocketSession session, User user, Long pictureId) throws IOException {
        // 获取编辑操作信息
        Long editingUserId = pictureEditingUsers.get(pictureId);
        String editAction = pictureEditRequestMessage.getEditAction();
        PictureEditActionEnum actionEnum = PictureEditActionEnum.getEnumByValue(editAction);
        if (actionEnum == null) {
            return;
        }
        // 确认是当前编辑者
        if (editingUserId != null && editingUserId.equals(user.getId())) {
            // 构造响应
            PictureEditResponseMessage responseMessage = new PictureEditResponseMessage();
            responseMessage.setType(PictureEditMessageTypeEnum.EDIT_ACTION.getValue());
            String message = String.format("%s执行%s", user.getUserName(), actionEnum.getText());
            responseMessage.setMessage(message);
            responseMessage.setEditAction(editAction);
            responseMessage.setUser(userService.getUserVO(user));
            // 广播给除了当前客户端之外的其他用户，否则会造成重复编辑
            broadcastToPicture(pictureId, responseMessage, session);
        }
    }

    /**
     * 退出编辑状态
     */
    public void handleExitEditMessage(PictureEditRequestMessage pictureEditRequestMessage,
                                      WebSocketSession session, User user, Long pictureId) throws Exception {
        // 获取编辑操作信息
        Long editingUserId = pictureEditingUsers.get(pictureId);
        if (editingUserId != null && editingUserId.equals(user.getId())) {
            // 移除当前用户的编辑状态
            pictureEditingUsers.remove(pictureId);
            // 构造响应
            PictureEditResponseMessage responseMessage = new PictureEditResponseMessage();
            responseMessage.setType(PictureEditMessageTypeEnum.EXIT_EDIT.getValue());
            String message = String.format("%s退出编辑图片", user.getUserName());
            responseMessage.setMessage(message);
            responseMessage.setUser(userService.getUserVO(user));
            // 广播给所有用户
            broadcastToPicture(pictureId, responseMessage);
        }
    }
}
