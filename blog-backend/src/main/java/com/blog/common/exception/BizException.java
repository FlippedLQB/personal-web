package com.blog.common.exception;

import lombok.Getter;

@Getter
public class BizException extends RuntimeException {
    private final int code;

    public BizException(BizCodeEnum bizCodeEnum) {
        super(bizCodeEnum.getMessage());
        this.code = bizCodeEnum.getCode();
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(BizCodeEnum bizCodeEnum, String customMessage) {
        super(customMessage);
        this.code = bizCodeEnum.getCode();
    }
}
