package com.gcx.team.exception;

import com.gcx.team.common.BaseResponse;
import com.gcx.team.common.ErrorCode;
import com.gcx.team.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 *
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler { // 定义一个全局异常处理类

    @ExceptionHandler(BusinessException.class) // 当捕获到 BusinessException 类型的异常时执行此方法
    public BaseResponse businessExceptionHandler(BusinessException e) { // 定义处理 BusinessException 的方法
        log.error("businessException: " + e.getMessage(), e); // 记录异常信息到日志
        return ResultUtils.error(e.getCode(), e.getMessage(), e.getDescription()); // 返回异常响应信息
    }

    @ExceptionHandler(RuntimeException.class) // 当捕获到 RuntimeException 类型的异常时执行此方法
    public BaseResponse runtimeExceptionHandler(RuntimeException e) { // 定义处理 RuntimeException 的方法
        log.error("runtimeException", e); // 记录异常信息到日志
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, e.getMessage(), ""); // 返回系统错误的响应信息
    }
}

