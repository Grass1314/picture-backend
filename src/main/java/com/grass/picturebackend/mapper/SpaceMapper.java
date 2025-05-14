package com.grass.picturebackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grass.picturebackend.model.entity.Space;
import org.apache.ibatis.annotations.Mapper;

/**
 * @description 针对表【space(空间)】的数据库操作Mapper
 * @Entity com.grass.picturebackend.model.entity.Space
*/
@Mapper
public interface SpaceMapper extends BaseMapper<Space> {

}




