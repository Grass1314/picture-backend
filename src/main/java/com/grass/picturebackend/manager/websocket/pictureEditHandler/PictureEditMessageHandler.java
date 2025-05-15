package com.grass.picturebackend.manager.websocket.pictureEditHandler;

import com.grass.picturebackend.manager.websocket.model.PictureEditRequestMessage;
import com.grass.picturebackend.model.entity.User;

/**
 * 策略接口
 * @author grass
 * @date 2021/9/21
 */
public interface PictureEditMessageHandler {
    void handle(User user, Long pictureId, PictureEditRequestMessage requestMessage, PictureEditStrategyImplHandler handler) throws Exception;
}


