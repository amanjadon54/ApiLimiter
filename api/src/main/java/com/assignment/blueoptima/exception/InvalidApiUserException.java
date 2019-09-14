package com.assignment.blueoptima.exception;

import java.util.List;

/***
 * InvalidApiException for either user or end point is invalid.
 */
public class InvalidApiUserException extends ApiLimitException {
    public InvalidApiUserException(String message, List<String> details) {
        super(message, details);
    }
}
