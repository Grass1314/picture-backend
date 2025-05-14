package com.grass.picturebackend.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

import java.util.Objects;

/**
 * 空间类型枚举类
 */
@Getter
public enum InvitationConfirmStatusEnum {

    PENDING("待定", "0"),
    CONFIRMED("确认", "1"),
    REJECTED("拒绝", "2");

    private final String text;

    private final String value;

    InvitationConfirmStatusEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     */
    public static InvitationConfirmStatusEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (InvitationConfirmStatusEnum spaceTypeEnum : InvitationConfirmStatusEnum.values()) {
            if (Objects.equals(spaceTypeEnum.value, value)) {
                return spaceTypeEnum;
            }
        }
        return null;
    }
}