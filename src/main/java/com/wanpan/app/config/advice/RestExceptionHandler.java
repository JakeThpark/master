package com.wanpan.app.config.advice;

import com.wanpan.app.exception.ExceptionResponse;
import com.wanpan.app.exception.InvalidRequestException;
import feign.FeignException;
import feign.RetryableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolationException;
import java.io.IOException;

@Slf4j
@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {
    /*
     * Feign client에 의한 Exception
     */
    @ExceptionHandler(FeignException.Unauthorized.class)
    public ResponseEntity<Object> handleFeignStatusException(FeignException e, ServletWebRequest request) {
        ResponseStatusException ioExceptionWithMessage = new ResponseStatusException(HttpStatus.valueOf(e.status()));
        return ResponseEntity.status(e.status()).body(new ExceptionResponse(ioExceptionWithMessage, request));
    }
//    @ExceptionHandler(FeignException.class)
//    public ResponseEntity<Object> handleFeignException(FeignException e, ServletWebRequest request) {
//        log.info("handleFeignExceptionhandleFeignExceptionhandleFeignException");
//        ResponseStatusException ioExceptionWithMessage = new ResponseStatusException(HttpStatus.valueOf(e.status()));
//        return ResponseEntity.status(e.status()).body(new ExceptionResponse(ioExceptionWithMessage, request));
//        ResponseEntity
//                .status(e.getResponse().status())
//                .body(e.getResponse().body().toString());
//    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Object> handleExceptionWithCodeAndMessage(ResponseStatusException exception, ServletWebRequest request) {
        return ResponseEntity.status(exception.getStatus()).body(new ExceptionResponse(exception, request));
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<Object> handleIOException(IOException ioException, ServletWebRequest request) {
        ResponseStatusException ioExceptionWithMessage = new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "일시적으로 서비스를 사용할 수 없습니다. 잠시후에 다시 시도해주세요.");

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ExceptionResponse(ioExceptionWithMessage, request));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolationException(ConstraintViolationException e, ServletWebRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ExceptionResponse(
                new InvalidRequestException(e.getConstraintViolations().stream().findFirst().get().getMessage()), request
        ));
    }

    //메세지만 내려주는 simple case
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Object> handleMethodArgumentTypeMismatchException(ServletWebRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ExceptionResponse(HttpStatus.BAD_REQUEST,"파라미터 형식이 잘못되었습니다.",request));
    }

    @Override
    protected ResponseEntity<Object> handleBindException(BindException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ExceptionResponse(
                new InvalidRequestException(ex.getBindingResult().getAllErrors().get(0).getDefaultMessage()),
                (ServletWebRequest) request)
        );
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ExceptionResponse(
                new InvalidRequestException(ex.getBindingResult().getAllErrors().get(0).getDefaultMessage()),
                (ServletWebRequest) request)
        );
    }

}
