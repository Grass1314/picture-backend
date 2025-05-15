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
 * @description: 进入编辑状态
 * @date 2025年05月15日 14:27
 */
// EnterEditHandler.java
@Component
public class EnterEditHandler implements PictureEditMessageHandler {

    @Resource
    private UserService userService;

    @Override
    public void handle(User user, Long pictureId, PictureEditRequestMessage requestMessage, PictureEditStrategyImplHandler handler) throws Exception {
        // 检查当前是否有用户在编辑该图片，如果没有，则允许当前用户进入编辑状态
        if (!handler.pictureEditingUsers.containsKey(pictureId)) {
            handler.pictureEditingUsers.put(pictureId, user.getId());

            PictureEditResponseMessage responseMessage = new PictureEditResponseMessage();
            responseMessage.setType(PictureEditMessageTypeEnum.ENTER_EDIT.getValue());
            responseMessage.setMessage(String.format("用户 %s 进入编辑状态", user.getUserName()));
            responseMessage.setUser(userService.getUserVO(user));

            handler.broadcastToPicture(pictureId, responseMessage);
        }
    }
}

