package com.assignment.blueoptima.exception;

import lombok.Data;

import java.util.List;

@Data
public class ApiLimitException extends RuntimeException {

    public ApiLimitException(String message, List<String> details) {
        super();
        this.message = message;
        this.details = details;
    }

    private String message;
    private List<String> details;
}