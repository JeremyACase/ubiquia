package org.ubiquia.core.flow.controller.advice;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import jakarta.validation.ConstraintViolationException;
import net.jimblackler.jsonschemafriend.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.context.request.WebRequest;
import org.ubiquia.common.library.advice.exception.AbstractGlobalExceptionHandler;
import org.ubiquia.common.library.advice.exception.ErrorResponse;

/**
 * Global exception handler that will catch REST errors and communicate them
 * appropriately.
 */
@ControllerAdvice
public class GlobalExceptionHandler extends AbstractGlobalExceptionHandler {

    /**
     * Catch any exceptions regarding http client errors.
     *
     * @param ex      The exception.
     * @param request The originating request.
     * @return The error response.
     */
    @Override
    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({HttpClientErrorException.class,
        ConstraintViolationException.class,
        UnrecognizedPropertyException.class,
        NoSuchFieldException.class,
        ValidationException.class,
        IllegalAccessException.class,
        IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> handleClientError(
        Exception ex,
        WebRequest request) {
        var error = super.handlerHelper(ex, ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }
}
