package com.grass.picturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * @author Mr.Liuxq注册
 * @Description: 用户
 * @date 2025年04月24日 16:16
 */
@Data
public class UserRegisterRequest implements Serializable {

    private static final long serialVersionUID = 8735650154179439661L;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */
    private String userPassword;

    /**
     * 确认密码
     */
    private String checkPassword;
}
