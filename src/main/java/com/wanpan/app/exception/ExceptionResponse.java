package com.wanpan.app.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

@Getter
public class ExceptionResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String method;
    private String path;

    public ExceptionResponse(ResponseStatusException exception, ServletWebRequest request) {
        HttpStatus httpStatus = exception.getStatus();
        timestamp = LocalDateTime.now();
        status = httpStatus.value();
        error = httpStatus.getReasonPhrase();
        message = exception.getMessage();
        method = Optional.ofNullable(request.getHttpMethod()).map(Enum::name).orElse("UNKNOWN");
        path = request.getRequest().getRequestURI();
    }

    public ExceptionResponse(HttpStatus errorHttpStatus, String errorMessage, ServletWebRequest request) {
        timestamp = LocalDateTime.now();
        status = errorHttpStatus.value();
        error = errorHttpStatus.getReasonPhrase();
        message = errorMessage;
        method = Optional.ofNullable(request.getHttpMethod()).map(Enum::name).orElse("UNKNOWN");
        path = request.getRequest().getRequestURI();
    }
}
