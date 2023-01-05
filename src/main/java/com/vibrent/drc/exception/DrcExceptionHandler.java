package com.vibrent.drc.exception;

import com.vibrent.drc.dto.ErrorDTO;
import com.vibrent.drc.dto.ValidationErrorDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;


@ControllerAdvice
@Slf4j
public class DrcExceptionHandler {

    @Order(value = Ordered.LOWEST_PRECEDENCE)
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ResponseBody
    public void processAccessDeniedException(AccessDeniedException e) {
        log.warn("drc-service: Attempted to access an endpoint with insufficient privileges", e);
    }

    @ExceptionHandler(BusinessProcessingException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public void processBusinessProcessingException(BusinessProcessingException e) {
        log.error("drc-service: Internal server error occurred. More details - {}", e.getMessage(), e);
    }

    @ExceptionHandler(BusinessValidationException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    @ResponseBody
    public ErrorDTO processBusinessValidationException(BusinessValidationException e) {
        log.warn("drc-service: Unprocessable entity", e);
        return new ErrorDTO(e.getMessage());
    }

    @ExceptionHandler(HttpClientValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ErrorDTO processHttpClientValidationException(HttpClientValidationException e) {
        log.warn("drc-service: Bad request", e);
        return new ErrorDTO(e.getMessage());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ErrorDTO processHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        String message = e.getMessage() != null ? e.getMessage().split(";")[0] : "Parse Error";
        log.warn("drc-service: Bad request -{} ", message, e);

        return new ErrorDTO("Bad Request");
    }

    @ExceptionHandler({MethodArgumentNotValidException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ErrorDTO processMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        BindingResult result = e.getBindingResult();
        return processFieldErrors(result.getFieldErrors());
    }

    private ErrorDTO processFieldErrors(List<FieldError> fieldErrors) {
        ValidationErrorDTO dto = new ValidationErrorDTO("BAD REQUEST");
        for (FieldError fieldError : fieldErrors) {
            String message = StringUtils.isEmpty(fieldError.getDefaultMessage()) ? fieldError.getCode() : fieldError.getDefaultMessage();
            dto.add(fieldError.getObjectName(), fieldError.getField(), message);
        }
        return dto;
    }
}