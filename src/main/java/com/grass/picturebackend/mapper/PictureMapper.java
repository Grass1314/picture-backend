package com.grass.picturebackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grass.picturebackend.model.entity.Picture;
import org.apache.ibatis.annotations.Mapper;

/**
  * @description 针对表【picture(图片)】的数据库操作Mapper
  * @createDate 2024-12-11 20:45:51
  * @Entity com.grass.picturebackend.model.entity.Picture
*/
@Mapper
public interface PictureMapper extends BaseMapper<Picture> {

}




