package com.assignment.blueoptima.excetion;

import com.assignment.blueoptima.exception.ApiLimitException;
import com.assignment.blueoptima.exception.ErrorResponse;
import com.assignment.blueoptima.exception.InvalidApiUserException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.ArrayList;
import java.util.List;

/***
 * Handler to take care of the custom exception.
 * Takes care of the appropriate HTTP status code in case of unfortunate events.
 */
@ControllerAdvice
public class CustomExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(InvalidApiUserException.class)
    public final ResponseEntity<Object> handleInvalidApiUser(InvalidApiUserException ex, WebRequest request) {
        List<String> details = new ArrayList<>();
        ErrorResponse error = new ErrorResponse(ex.getMessage(), ex.getDetails());
        return new ResponseEntity(error, HttpStatus.PRECONDITION_FAILED);
    }

    @ExceptionHandler(ApiLimitException.class)
    public final ResponseEntity<Object> handleApiLimitException(ApiLimitException ex, WebRequest request) {
        List<String> details = new ArrayList<>();
        ErrorResponse error = new ErrorResponse(ex.getMessage(), ex.getDetails());
        return new ResponseEntity(error, HttpStatus.INSUFFICIENT_STORAGE);
    }


    @ExceptionHandler(Exception.class)
    public final ResponseEntity<Object> handleAllExceptions(Exception ex, WebRequest request) {
        List<String> details = new ArrayList<>();
        details.add(ex.getLocalizedMessage());
        ErrorResponse error = new ErrorResponse("Server Error", details);
        return new ResponseEntity(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
