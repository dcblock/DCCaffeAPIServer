package com.digicap.dcblock.caffeapiserver.handler;

import com.digicap.dcblock.caffeapiserver.dto.ApiError;
import com.digicap.dcblock.caffeapiserver.exception.ExpiredTimeException;
import com.digicap.dcblock.caffeapiserver.exception.ForbiddenException;
import com.digicap.dcblock.caffeapiserver.exception.IncludedMenusException;
import com.digicap.dcblock.caffeapiserver.exception.InvalidParameterException;
import com.digicap.dcblock.caffeapiserver.exception.JwtException;
import com.digicap.dcblock.caffeapiserver.exception.NotFindException;
import com.digicap.dcblock.caffeapiserver.exception.NotImplementedException;
import com.digicap.dcblock.caffeapiserver.exception.NotSupportedException;
import com.digicap.dcblock.caffeapiserver.exception.UnknownException;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.MyBatisSystemException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Global Exception Handler Class.
 * exclude Authentication Exception(AuthenticationFailureHandler.java). 
 *
 * @author DigiCAP
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public ApiError handleException(Exception e) {
        log.error(e.getMessage());
        e.printStackTrace();
        return new ApiError(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
    }

    @ExceptionHandler(UnknownException.class)
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public ApiError handleException(UnknownException e) {
        log.error(e.getReason());
        e.printStackTrace();
        return new ApiError(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getReason());
    }

    @ExceptionHandler(NotFindException.class)
    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    @ResponseBody
    public ApiError handleException(NotFindException e) {
        log.error(e.getReason());
        e.printStackTrace();
        return new ApiError(HttpStatus.NOT_FOUND.value(), e.getReason());
    }

    @ExceptionHandler(InvalidParameterException.class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ApiError handleException(InvalidParameterException e) {
        log.error(e.getReason());
        e.printStackTrace();
        return new ApiError(HttpStatus.BAD_REQUEST.value(), e.getReason());
    }

    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(value = HttpStatus.FORBIDDEN)
    @ResponseBody
    public ApiError handleException(ForbiddenException e) {
        log.error(e.getReason());
        e.printStackTrace();
        return new ApiError(HttpStatus.FORBIDDEN.value(), e.getReason());
    }

    @ExceptionHandler(ExpiredTimeException.class)
    @ResponseStatus(value = HttpStatus.NOT_ACCEPTABLE)
    @ResponseBody
    public ApiError handleException(ExpiredTimeException e) {
        log.error(e.getReason());
        e.printStackTrace();
        return new ApiError(HttpStatus.NOT_ACCEPTABLE.value(), e.getReason());
    }

    @ExceptionHandler(NotSupportedException.class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ApiError handleException(NotSupportedException e) {
        log.error(e.getReason());
        e.printStackTrace();
        return new ApiError(HttpStatus.BAD_REQUEST.value(), e.getReason());
    }

    @ExceptionHandler(IncludedMenusException.class)
    @ResponseStatus(value = HttpStatus.NOT_ACCEPTABLE)
    @ResponseBody
    public ApiError handleException(IncludedMenusException e) {
        log.error(e.getReason());
        e.printStackTrace();
        return new ApiError(HttpStatus.NOT_ACCEPTABLE.value(), e.getReason());
    }

    @ExceptionHandler(NotImplementedException.class)
    @ResponseStatus(value = HttpStatus.NOT_IMPLEMENTED)
    @ResponseBody
    public ApiError handleException() {
        return new ApiError(HttpStatus.NOT_IMPLEMENTED.value(), "not supported uri");
    }

    @ExceptionHandler(MyBatisSystemException.class)
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public ApiError handleException(MyBatisSystemException e) {
        log.error(e.getMessage());
        e.printStackTrace();
        return new ApiError(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ApiError handleException(MethodArgumentNotValidException e) {
        log.error(e.getMessage());
        e.printStackTrace();
        return new ApiError(HttpStatus.BAD_REQUEST.value(), e.getMessage());
    }

    @ExceptionHandler(JwtException.class)
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public ApiError handleException(JwtException e) {
        log.error(e.getMessage());
        e.printStackTrace();
        return new ApiError(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ApiError handleException(HttpMessageNotReadableException e) {
        log.error(e.getMessage());
        e.printStackTrace();
        return new ApiError(HttpStatus.BAD_REQUEST.value(), "invalid input values");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ApiError handleException(IllegalArgumentException e) {
        log.error(e.getMessage());
        e.printStackTrace();
        return new ApiError(HttpStatus.BAD_REQUEST.value(), e.getMessage());
    }
}
