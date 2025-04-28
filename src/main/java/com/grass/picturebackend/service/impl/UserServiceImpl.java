package com.grass.picturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.grass.picturebackend.constant.UserConstant;
import com.grass.picturebackend.exception.BusinessException;
import com.grass.picturebackend.exception.ErrorCode;
import com.grass.picturebackend.mapper.UserMapper;
import com.grass.picturebackend.model.dto.user.UserLoginRequest;
import com.grass.picturebackend.model.dto.user.UserQueryRequest;
import com.grass.picturebackend.model.dto.user.UserRegisterRequest;
import com.grass.picturebackend.model.entity.User;
import com.grass.picturebackend.model.enums.UserRoleEnum;
import com.grass.picturebackend.model.vo.LoginUserVO;
import com.grass.picturebackend.model.vo.UserVO;
import com.grass.picturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Mr.Liuxq
 * @description: 用户信息
 * @date 2025年04月24日 16:31
 */

@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    // 盐值，混淆密码
    private static final String SALT = "grass";

    /**
     * 用户注册
     *
     * @param userRegisterRequest 注册请求体
     * @return 新用户 id
     */
    @Override
    public long userRegister(UserRegisterRequest userRegisterRequest) {

        if (StrUtil.hasBlank(userRegisterRequest.getUserAccount(), userRegisterRequest.getUserPassword(), userRegisterRequest.getCheckPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        // 校验账户长度是否小于4
        if (userRegisterRequest.getUserAccount().length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        // 校验密码长度是否小于8
        if (userRegisterRequest.getUserPassword().length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        // 校验两次输入的密码是否一致
        if (!userRegisterRequest.getUserPassword().equals(userRegisterRequest.getCheckPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        // 校验账户是否重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userRegisterRequest.getUserAccount());
        Long count = baseMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }
        // 加密密码
        String encryptPassword = getEncryptPassword(userRegisterRequest.getUserPassword());
        // 插入数据
        User user = new User();
        BeanUtil.copyProperties(userRegisterRequest, user);
        user.setUserName("无名");
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean save = this.save(user);
        if (!save) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败, 数据库错误");
        }
        return user.getId();
    }

    /**
     * 用户注册
     * @param userPassword 用户密码
     * @return 加密后的密码
     */
    @Override
    public String getEncryptPassword(String userPassword) {
        return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
    }

    /**
     * 用户登录
     *
     * @param userLoginRequest 登录请求体
     * @return 登录用户信息
     * @author Mr.Liuxq
     */
    @Override
    public LoginUserVO userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request) {
        if (StrUtil.hasBlank(userLoginRequest.getUserAccount(), userLoginRequest.getUserPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userLoginRequest.getUserAccount().length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号错误");
        }
        if (userLoginRequest.getUserPassword().length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }
        // 密码加密
        String encryptPassword = getEncryptPassword(userLoginRequest.getUserPassword());
        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userLoginRequest.getUserAccount());
        queryWrapper.eq("userPassword", encryptPassword);
        User user = baseMapper.selectOne(queryWrapper);
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, user);
        return this.getLoginUserVO(user);
    }

    /**
     * 获取脱敏类的用户信息
     *
     * @param user 用户
     * @return 脱敏后的用户信息
     */
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    /**
     * 获取当前登录用户
     *
     * @param request http 请求
     * @return 用户信息
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 查询用户信息
        User user = this.getById(currentUser.getId());
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return user;
    }

    /**
     * 用户注销
     *
     * @param request http 请求
     * @return 是否注销成功
     */
    @Override
    public boolean userLoginOut(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);
        return true;
    }

    /**
     * 获取脱敏类的用户信息
     *
     * @param user 用户
     * @return 脱敏后的用户信息
     * @author Mr.Liuxq
     */
    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }

    /**
     * 批量获取脱敏类的用户信息
     *
     * @param userList 用户列表
     * @return 脱敏后的用户信息
     * @author Mr.Liuxq
     */
    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollectionUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    /**
     * 获取查询条件
     *
     * @param userQueryRequest 用户查询请求
     * @return 查询条件
     * @author Mr.Liuxq
     */
    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(Objects.nonNull(id), "id", id);
        queryWrapper.like(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
        queryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName);
        queryWrapper.like(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.eq(StrUtil.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);

        return queryWrapper;
    }

    /**
     * 是否为管理员
     *
     * @param user 用户
     * @return 是否管理员
     */
    @Override
    public boolean isAdmin(User user) {
        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }


}
