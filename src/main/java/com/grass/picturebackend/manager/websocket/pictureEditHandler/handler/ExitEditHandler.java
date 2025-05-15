package com.grass.picturebackend.manager.websocket.pictureEditHandler.handler;


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
 * @description: 退出编辑状态
 * @date 2025年05月15日 14:29
 */
// ExitEditHandler.java
@Component
public class ExitEditHandler implements PictureEditMessageHandler {

    @Resource
    private UserService userService;

    @Override
    public void handle(User user, Long pictureId, PictureEditRequestMessage requestMessage, PictureEditStrategyImplHandler handler) throws Exception {
        Long editingUserId = handler.pictureEditingUsers.get(pictureId);
        if (editingUserId != null && editingUserId.equals(user.getId())) {
            PictureEditResponseMessage responseMessage = new PictureEditResponseMessage();
            responseMessage.setType(PictureEditMessageTypeEnum.EXIT_EDIT.getValue());
            responseMessage.setMessage(String.format("用户 %s 退出编辑状态", user.getUserName()));
            responseMessage.setUser(userService.getUserVO(user));

            handler.broadcastToPicture(pictureId, responseMessage);
        }
    }
}

