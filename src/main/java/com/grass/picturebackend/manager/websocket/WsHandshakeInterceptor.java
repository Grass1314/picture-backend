package com.grass.picturebackend.manager.websocket;

import cn.hutool.core.util.StrUtil;
import com.grass.picturebackend.manager.auth.SpaceUserAuthManager;
import com.grass.picturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.grass.picturebackend.model.entity.Picture;
import com.grass.picturebackend.model.entity.Space;
import com.grass.picturebackend.model.entity.User;
import com.grass.picturebackend.model.enums.SpaceTypeEnum;
import com.grass.picturebackend.service.PictureService;
import com.grass.picturebackend.service.SpaceService;
import com.grass.picturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * @author Mr.Liuxq
 * @description: websocket拦截器
 * @date 2025年05月15日 09:52
 */
@Component
@Slf4j
public class WsHandshakeInterceptor implements HandshakeInterceptor {

    @Resource
    private PictureService  pictureService;

    @Resource
    private UserService  userService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;


    /**
     * 在WebSocket握手之前执行的方法，用于验证请求的合法性并设置WebSocket会话的属性。
     *
     * @param request     ServerHttpRequest对象，表示HTTP请求
     * @param response    ServerHttpResponse对象，表示HTTP响应
     * @param wsHandler   WebSocketHandler对象，表示WebSocket处理器
     * @param attributes  Map<String, Object>对象，用于存储WebSocket会话的属性
     * @return boolean    如果验证通过并成功设置属性，返回true；否则返回false
     * @throws Exception  如果在处理过程中发生异常，则抛出
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        if (request instanceof ServletServerHttpRequest) {
            HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
            // 获取请求参数
            String pictureId = servletRequest.getParameter("pictureId");
            if (StrUtil.isBlank(pictureId)) {
                log.error("pictureId is null");
                return false;
            }
            User loginUser = userService.getLoginUser(servletRequest);
            if (loginUser == null) {
                log.error("用户未登录");
                return false;
            }
            Picture picture = pictureService.getById(pictureId);
            if (picture == null) {
                log.error("图片不存在");
                return false;
            }
            Long spaceId = picture.getSpaceId();
            Space space = null;
            if (spaceId != null) {
                space = spaceService.getById(spaceId);
                if (space == null) {
                    log.error("空间不存在");
                    return false;
                }
                if (space.getSpaceType() != SpaceTypeEnum.TEAM.getValue()) {
                    log.error("不是团队空间");
                    return false;
                }
            }
            List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
            if (!permissionList.contains(SpaceUserPermissionConstant.PICTURE_EDIT)) {
                log.error("用户没有编辑图片权限");
                return false;
            }
            // 设置attributes
            attributes.put("user", loginUser);
            attributes.put("userId", loginUser.getId());
            attributes.put("pictureId", Long.parseLong(pictureId));
        }
        return true;
    }

    /**
     * @param request
     * @param response
     * @param wsHandler
     * @param exception
     */
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {

    }
}
