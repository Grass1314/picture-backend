package com.grass.picturebackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grass.picturebackend.model.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * @description: 用户mapper
 * @author grass
 * @date 2023-01-08 21:05
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

}




