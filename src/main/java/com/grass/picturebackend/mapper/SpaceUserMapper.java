package com.grass.picturebackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grass.picturebackend.model.entity.SpaceUser;
import org.apache.ibatis.annotations.Mapper;


/**
* @author Mr.Grass
* @description 针对表【space_user(空间用户关联)】的数据库操作Mapper
*/
@Mapper
public interface SpaceUserMapper extends BaseMapper<SpaceUser> {

}




