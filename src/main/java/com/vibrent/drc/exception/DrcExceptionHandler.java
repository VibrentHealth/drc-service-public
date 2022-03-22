package com.vibrent.drc.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;


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
        log.warn("drc-service: Internal server error occurred", e);
    }

    @ExceptionHandler(BusinessValidationException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    @ResponseBody
    public void processBusinessValidationException(BusinessValidationException e) {
        log.warn("drc-service: Unprocessable entity", e);
    }

    @ExceptionHandler(HttpClientValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public String processHttpClientValidationException(HttpClientValidationException e) {
        log.warn("drc-service: Bad request", e);
        return e.getMessage();
    }
}