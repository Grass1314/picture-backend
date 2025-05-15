package com.grass.picturebackend.manager.websocket.pictureEditHandler.handler;


import com.grass.picturebackend.manager.websocket.model.PictureEditActionEnum;
import com.grass.picturebackend.manager.websocket.model.PictureEditMessageTypeEnum;
import com.grass.picturebackend.manager.websocket.model.PictureEditRequestMessage;
import com.grass.picturebackend.manager.websocket.model.PictureEditResponseMessage;
import com.grass.picturebackend.manager.websocket.pictureEditHandler.PictureEditMessageHandler;
import com.grass.picturebackend.manager.websocket.pictureEditHandler.PictureEditStrategyImplHandler;
import com.grass.picturebackend.model.entity.User;
import com.grass.picturebackend.service.UserService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author Mr.Liuxq
 * @description: 执行编辑
 * @date 2025年05月15日 14:30
 */
// EditActionHandler.java
@Component
public class EditActionHandler implements PictureEditMessageHandler {

    @Resource
    private UserService userService;

    @Override
    public void handle(User user, Long pictureId, PictureEditRequestMessage requestMessage, PictureEditStrategyImplHandler handler) throws Exception {
        Long editingUserId = handler.pictureEditingUsers.get(pictureId);
        String editAction = requestMessage.getEditAction();
        PictureEditActionEnum actionEnum = PictureEditActionEnum.getEnumByValue(editAction);

        if (actionEnum != null && editingUserId != null && editingUserId.equals(user.getId())) {
            PictureEditResponseMessage responseMessage = new PictureEditResponseMessage();
            responseMessage.setType(PictureEditMessageTypeEnum.EDIT_ACTION.getValue());
            responseMessage.setMessage(String.format("用户 %s 执行了 %s 操作", user.getUserName(), actionEnum.getText()));
            responseMessage.setEditAction(editAction);
            responseMessage.setUser(userService.getUserVO(user));
            handler.broadcastToPicture(pictureId, responseMessage, null);
        }
    }
}

