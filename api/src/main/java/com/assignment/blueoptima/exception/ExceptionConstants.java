package com.assignment.blueoptima.exception;

import java.util.ArrayList;

public class ExceptionConstants {

    public static final String USER_NOT_VALID = "Invalid User details";
    public static final String USER_PRIVILEGE_MESSAGE = "Please check with team if the user has access to the current api service";
    public static final String API_LIMIT_REACHED_MESSAGE = "Accessing service reached maximum limit";
    public static final String API_CUSTOMER_CARE_MESSAGE = "2. If the issue persist, please contact customer care.";
    public static final String API_REFRESH_TIME_MESSAGE = "1. Please wait for time in seconds : ";
    public static final String API_NOT_CAPTURING_MESSAGE = "1. We are not capturing the service provided by you. ";

    public static ArrayList<String> prepareExceptionDetails(String... details) {
        ArrayList<String> errorDetails = new ArrayList<>();
        for (String error : details) {
            errorDetails.add(error);
        }
        return errorDetails;
    }
}
