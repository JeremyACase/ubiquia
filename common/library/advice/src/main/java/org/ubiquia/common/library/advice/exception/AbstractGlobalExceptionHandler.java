package org.ubiquia.common.library.advice.exception;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import jakarta.persistence.PersistenceException;
import jakarta.validation.ConstraintViolationException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Objects;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.Nullable;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;


/**
 * Global exception handler that will catch REST errors and communicate them
 * appropriately.
 */
@ControllerAdvice
public abstract class AbstractGlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * Generic exception handler that will catch anything not more-specifically
     * defined elsewhere.
     *
     * @param ex      The exception that occurred.
     * @param request The originating request.
     * @return A response to the REST request.
     */
    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnkownException(
        Exception ex,
        WebRequest request) {
        var error = this.handlerHelper(ex, "Unknown exception occurred: ");
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Catch any IO exceptions and return the response to the client.
     *
     * @param ex      The exception.
     * @param request The originating request.
     * @return The error response.
     */
    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(IOException.class)
    public ResponseEntity<ErrorResponse> handleIOException(
        Exception ex,
        WebRequest request) {
        var error = this.handlerHelper(ex, "IO exception occurred.");
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Catch any IO exceptions and return the response to the client.
     *
     * @param ex      The exception.
     * @param request The originating request.
     * @return The error response.
     */
    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({TransactionSystemException.class})
    public ResponseEntity<ErrorResponse> handleTransactionException(
        Exception ex,
        WebRequest request) {
        var cast = (TransactionSystemException) ex;
        var error = this.handlerHelper(ex, cast.getRootCause().getMessage());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * Catch any exceptions regarding interruptions.
     *
     * @param ex      The exception.
     * @param request The originating request.
     * @return The error response.
     */
    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler({InterruptedException.class})
    public ResponseEntity<ErrorResponse> handleInterruptionException(
        Exception ex,
        WebRequest request) {
        var error = this.handlerHelper(ex, "Interruption exception occurred.");
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Catch any exceptions regarding multipart exceptions.
     *
     * @param ex      The exception.
     * @param request The originating request.
     * @return The error response.
     */
    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({MultipartException.class})
    public ResponseEntity<ErrorResponse> handleMultipartException(
        Exception ex,
        WebRequest request) {
        var error = this.handlerHelper(ex, "Multipart exception occurred.");
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * Catch any exceptions regarding http client errors.
     *
     * @param ex      The exception.
     * @param request The originating request.
     * @return The error response.
     */
    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({HttpClientErrorException.class,
        ConstraintViolationException.class,
        UnrecognizedPropertyException.class,
        NoSuchFieldException.class,
        IllegalAccessException.class,
        IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> handleClientError(
        Exception ex,
        WebRequest request) {
        var error = this.handlerHelper(ex, ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * Catch any exceptions regarding server-side database errors.
     *
     * @param ex      The exception.
     * @param request The originating request.
     * @return The error response.
     */
    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler({SQLException.class, PersistenceException.class})
    public ResponseEntity<ErrorResponse> handleDatabaseException(
        Exception ex,
        WebRequest request) {
        var error = this.handlerHelper(ex, "Database exception occurred.");
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Catch any exceptions regarding client-side database errors.
     *
     * @param ex      The exception.
     * @param request The originating request.
     * @return The error response.
     */
    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({DataIntegrityViolationException.class})
    public ResponseEntity<ErrorResponse> handleDatabaseException(
        DataIntegrityViolationException ex,
        WebRequest request) {
        var error = this.handlerHelper(ex, "Database exception occurred.");
        var cause = ex.getCause();
        while (Objects.nonNull(cause)) {
            error.getDetails().add(cause.getMessage());
            cause = cause.getCause();
        }
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * Catch any exceptions regarding http server errors.
     *
     * @param ex      The exception.
     * @param request The originating request.
     * @return The error response.
     */
    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler({HttpServerErrorException.class})
    public ResponseEntity<ErrorResponse> handleServerError(
        Exception ex,
        WebRequest request) {
        var error = this.handlerHelper(ex, "HTTP server exception occurred.");
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
        HttpMessageNotReadableException ex,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request) {
        var error = this.handlerHelper(ex, ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
        Exception ex,
        @Nullable Object body,
        HttpHeaders headers,
        HttpStatusCode statusCode,
        WebRequest request) {

        var error = this.handlerHelper(ex, ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Helper method to build and return an error response.
     *
     * @param ex            The exception that occurred.
     * @param helperMessage The message passed in from our exception handler.
     * @return A populated error message.
     */
    private ErrorResponse handlerHelper(Exception ex, String helperMessage) {
        var details = new ArrayList<String>();
        details.add(ex.getLocalizedMessage());
        var error = new ErrorResponse();
        error.setDetails(details);
        error.setMessage(helperMessage);
        return error;
    }
}
