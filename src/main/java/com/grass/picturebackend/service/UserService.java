package com.grass.picturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.grass.picturebackend.model.dto.user.UserLoginRequest;
import com.grass.picturebackend.model.dto.user.UserQueryRequest;
import com.grass.picturebackend.model.dto.user.UserRegisterRequest;
import com.grass.picturebackend.model.entity.User;
import com.grass.picturebackend.model.vo.LoginUserVO;
import com.grass.picturebackend.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public interface UserService extends IService<User> {

    /**
     * 用户注册
     * @param userRegisterRequest 注册请求体
     * @return 新用户 id
     */
    long userRegister(UserRegisterRequest userRegisterRequest);

    /**
     * 用户注册
     * @param userPassword 用户密码
     * @return 加密后的密码
     */
    String getEncryptPassword(String userPassword);

    /**
     * 用户登录
     * @param userLoginRequest 登录请求体
     * @return 登录用户信息
     * @author Mr.Liuxq
     */
    LoginUserVO userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request);

    /**
     * 获取脱敏类的用户信息
     *
     * @param user 用户
     * @return 脱敏后的用户信息
     * @author Mr.Liuxq
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 获取当前登录用户
     * @param request http 请求
     * @return 用户信息
     * @author Mr.Liuxq
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 用户注销
     * @param request http 请求
     * @return 是否注销成功
     * @author Mr.Liuxq
     */
    boolean userLoginOut(HttpServletRequest request);

    /**
     * 获取脱敏类的用户信息
     * @param user 用户
     * @return 脱敏后的用户信息
     * @author Mr.Liuxq
     */
    UserVO getUserVO(User user);

    /**
     * 批量获取脱敏类的用户信息
     * @param userList 用户列表
     * @return 脱敏后的用户信息
     * @author Mr.Liuxq
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 获取查询条件
     * @param userQueryRequest 用户查询请求
     * @return 查询条件
     * @author Mr.Liuxq
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * 是否为管理员
     *
     * @param user 用户
     * @return 是否管理员
     */
    boolean isAdmin(User user);


}
