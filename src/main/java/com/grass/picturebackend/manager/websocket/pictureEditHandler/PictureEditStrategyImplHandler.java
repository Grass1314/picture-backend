package com.grass.picturebackend.manager.websocket.pictureEditHandler;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.grass.picturebackend.manager.websocket.model.PictureEditActionEnum;
import com.grass.picturebackend.manager.websocket.model.PictureEditMessageTypeEnum;
import com.grass.picturebackend.manager.websocket.model.PictureEditRequestMessage;
import com.grass.picturebackend.manager.websocket.model.PictureEditResponseMessage;
import com.grass.picturebackend.manager.websocket.pictureEditHandler.handler.EditActionHandler;
import com.grass.picturebackend.manager.websocket.pictureEditHandler.handler.EnterEditHandler;
import com.grass.picturebackend.manager.websocket.pictureEditHandler.handler.ExitEditHandler;
import com.grass.picturebackend.model.entity.User;
import com.grass.picturebackend.service.UserService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Resource;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Mr.Liuxq
 * @description: 图库websocket处理器
 * @date 2025年05月15日 10:14
 */
@Component
public class PictureEditStrategyImplHandler extends TextWebSocketHandler {

    @Resource
    private UserService userService;

    @Resource
    private EnterEditHandler enterEditHandler;

    @Resource
    private ExitEditHandler exitEditHandler;

    @Resource
    private EditActionHandler editActionHandler;

    // 每张图片的编辑状态，key: pictureId, value: 当前正在编辑的用户 ID
    public final Map<Long, Long> pictureEditingUsers = new ConcurrentHashMap<>();

    // 保存所有连接的会话，key: pictureId, value: 用户会话集合
    public final Map<Long, Set<WebSocketSession>> pictureSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Map<String, Object> attributes = session.getAttributes();
        Long pictureId = (Long) attributes.get("pictureId");
        User user = (User) attributes.get("user");

        // 处理用户退出编辑状态的消息
        handleExitEditMessage(user, pictureId);

        // 从图片会话集合中移除当前会话
        Set<WebSocketSession> sessionSet = pictureSessions.get(pictureId);
        if (CollUtil.isNotEmpty(sessionSet)) {
            sessionSet.remove(session);
            if (CollUtil.isEmpty(sessionSet)) {
                pictureSessions.remove(pictureId);
            }
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        PictureEditRequestMessage requestMessage = JSONUtil.toBean(message.getPayload(), PictureEditRequestMessage.class);
        String type = requestMessage.getType();
        Map<String, Object> attributes = session.getAttributes();
        Long pictureId = (Long) attributes.get("pictureId");
        User user = (User) attributes.get("user");

        switch (PictureEditMessageTypeEnum.getEnumByValue(type)) {
            case ENTER_EDIT:
                enterEditHandler.handle(user, pictureId, requestMessage, this);
                break;
            case EXIT_EDIT:
                exitEditHandler.handle(user, pictureId, requestMessage, this);
                break;
            case EDIT_ACTION:
                editActionHandler.handle(user, pictureId, requestMessage, this);
                break;
            default:
                PictureEditResponseMessage errorResponse = new PictureEditResponseMessage();
                errorResponse.setType(PictureEditMessageTypeEnum.ERROR.getValue());
                errorResponse.setMessage("未知消息类型");
                errorResponse.setUser(userService.getUserVO(user));
                session.sendMessage(new TextMessage(JSONUtil.toJsonPrettyStr(errorResponse)));
                break;
        }
    }


    /**
     * 处理用户进入编辑图片状态的请求。
     * 如果当前没有其他用户在编辑该图片，则将当前用户设置为编辑用户，并广播进入编辑状态的消息。
     *
     * @param user      请求进入编辑状态的用户对象
     * @param pictureId 需要编辑的图片ID
     * @throws Exception 如果处理过程中发生异常
     */
    public void handleEnterEditMessage(User user, Long pictureId) throws Exception {
        // 检查当前是否有用户在编辑该图片，如果没有，则允许当前用户进入编辑状态
        if (!pictureEditingUsers.containsKey(pictureId)) {
            // 将当前用户设置为编辑用户
            pictureEditingUsers.put(pictureId, user.getId());

            // 创建并设置进入编辑状态的响应消息
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.ENTER_EDIT.getValue());
            String message = String.format("用户 %s 进入编辑状态", user.getUserName());
            pictureEditResponseMessage.setMessage(message);
            pictureEditResponseMessage.setUser(userService.getUserVO(user));

            // 广播进入编辑状态的消息给所有相关用户
            broadcastToPicture(pictureId, pictureEditResponseMessage);
        }
    }

    /**
     * 处理图片编辑操作消息。
     * 该方法根据传入的图片编辑请求消息，判断当前用户是否有权限执行编辑操作，并将编辑操作的结果广播给其他用户。
     *
     * @param pictureEditRequestMessage 图片编辑请求消息，包含用户请求的编辑操作信息
     * @param session WebSocket会话，表示当前用户的连接
     * @param user 当前用户对象，包含用户的基本信息
     * @param pictureId 图片的唯一标识符，用于确定当前编辑的图片
     * @throws Exception 如果处理过程中发生异常，则抛出
     */
    public void handleEditActionMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) throws Exception {
        Long editingUserId = pictureEditingUsers.get(pictureId);
        String editAction = pictureEditRequestMessage.getEditAction();
        PictureEditActionEnum actionEnum = PictureEditActionEnum.getEnumByValue(editAction);
        if (actionEnum == null) {
            return;
        }
        if (editingUserId != null && editingUserId.equals(user.getId())) {
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EDIT_ACTION.getValue());
            String message = String.format("用户 %s 执行了 %s 操作", user.getUserName(), actionEnum.getText());
            pictureEditResponseMessage.setMessage(message);
            pictureEditResponseMessage.setEditAction(editAction);
            pictureEditResponseMessage.setUser(userService.getUserVO(user));
            // 广播给除了当前客户端之外的其他用户
            broadcastToPicture(pictureId, pictureEditResponseMessage, session);
        }
    }

    /**
     * 处理用户退出图片编辑状态的消息。
     * 当用户退出编辑状态时，会检查当前是否有其他用户正在编辑同一张图片，如果是当前用户，则广播退出编辑的消息。
     *
     * @param user      退出编辑状态的用户对象，包含用户的基本信息。
     * @param pictureId 正在编辑的图片的唯一标识符。
     * @throws Exception 如果在处理过程中发生错误，抛出异常。
     */
    public void handleExitEditMessage(User user, Long pictureId) throws Exception {
        // 获取当前正在编辑该图片的用户ID
        Long editingUserId = pictureEditingUsers.get(pictureId);

        // 检查当前用户是否是正在编辑该图片的用户
        if (editingUserId != null && editingUserId.equals(user.getId())) {
            // 创建退出编辑的响应消息
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EXIT_EDIT.getValue());

            // 设置退出编辑的消息内容
            String message = String.format("用户 %s 退出编辑状态", user.getUserName());
            pictureEditResponseMessage.setMessage(message);

            // 设置用户信息
            pictureEditResponseMessage.setUser(userService.getUserVO(user));

            // 广播退出编辑的消息给所有相关用户
            broadcastToPicture(pictureId, pictureEditResponseMessage);
        }
    }



    /**
     * 向指定图片的所有WebSocket会话广播图片编辑响应消息，排除指定的会话。
     *
     * @param pictureId 图片的唯一标识符，用于查找相关的WebSocket会话。
     * @param pictureEditResponseMessage 需要广播的图片编辑响应消息对象。
     * @param excludeSession 需要排除的WebSocket会话，不向其发送广播消息。
     * @throws Exception 如果在广播过程中发生错误，抛出异常。
     */
    public void broadcastToPicture(Long pictureId, PictureEditResponseMessage pictureEditResponseMessage, WebSocketSession excludeSession) throws Exception {
        Set<WebSocketSession> sessionSet = pictureSessions.get(pictureId);
        if (CollUtil.isNotEmpty(sessionSet)) {
            // 创建ObjectMapper实例
            ObjectMapper objectMapper = new ObjectMapper();
            // 配置序列化，将Long转成String，防止丢失精度
            SimpleModule module = new SimpleModule();
            module.addSerializer(Long.class, ToStringSerializer.instance);
            module.addSerializer(Long.TYPE, ToStringSerializer.instance);
            objectMapper.registerModule(module);
            // 序列化为JSON字符串
            String message = objectMapper.writeValueAsString(pictureEditResponseMessage);
            TextMessage textMessage = new TextMessage(message);
            for (WebSocketSession session : sessionSet) {
                // 排除的会话不进行广播
                if (excludeSession != null && excludeSession.equals(session)) {
                    continue;
                }
                if (session.isOpen()) {
                    session.sendMessage(textMessage);
                }
            }
        }
    }

    /**
     * 向指定图片的所有WebSocket会话广播图片编辑响应消息。
     *
     * @param pictureId 图片的唯一标识符，用于查找相关的WebSocket会话。
     * @param pictureEditResponseMessage 需要广播的图片编辑响应消息对象。
     * @throws Exception 如果在广播过程中发生错误，抛出异常。
     */
    public void broadcastToPicture(Long pictureId, PictureEditResponseMessage pictureEditResponseMessage) throws Exception {
        broadcastToPicture(pictureId, pictureEditResponseMessage, null);
    }


}
