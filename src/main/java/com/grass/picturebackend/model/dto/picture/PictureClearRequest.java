package com.grass.picturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 清理图片
 */
@Data
public class PictureClearRequest implements Serializable {

    /**
     * 图片id
     */
    private Long id;


    private static final long serialVersionUID = 1L;
}