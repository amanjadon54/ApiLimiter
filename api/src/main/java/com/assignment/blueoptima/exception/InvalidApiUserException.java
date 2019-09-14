package com.assignment.blueoptima.exception;

import java.util.List;

public class InvalidApiUserException extends ApiLimitException {
    public InvalidApiUserException(String message, List<String> details) {
        super(message, details);
    }
}
