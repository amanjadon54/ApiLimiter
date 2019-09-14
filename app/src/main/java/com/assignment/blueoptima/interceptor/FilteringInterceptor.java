package com.assignment.blueoptima.interceptor;

import com.assignment.blueoptima.ApiLimitDescriptor;
import com.assignment.blueoptima.exception.ApiLimitException;
import com.assignment.blueoptima.exception.InvalidApiUserException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import redis.clients.jedis.Jedis;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;

import static com.assignment.blueoptima.ApiLimitConstants.USER_RECORD;
import static com.assignment.blueoptima.exception.ExceptionConstants.*;

@Log4j2
@Component
public class FilteringInterceptor extends HandlerInterceptorAdapter {

    @Autowired
    private Jedis redis;

    @Autowired
    ObjectMapper mapper;

    private ArrayList<String> errorDetails = new ArrayList<>();

    /***
     * Intercept the request before passing it to specific api controller.
     * Intercepts all APIs except the one containing admin.
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        log.info("\n--------   Interception For API LIMIT  --- ");
        String user = request.getHeader("userName");
        String servletPath = request.getServletPath();
        log.info(String.format("requested url is : {}"), servletPath);
        String path = servletPath.substring(1);
        log.info(String.format("requested path is : {}"), path);

        Boolean found = redis.sismember(USER_RECORD, user);
        if (found == true) {
            log.info(String.format("MATCHED IN ENUM THE {} : {}"), path, user);
            String apiRecord = redis.hget("apiRecord", path);
            if (apiRecord != null && !apiRecord.equals("")) {
                String data = redis.hget(apiRecord, user);
                if (data == null) {
                    throw new ApiLimitException(USER_PRIVILEGE_MESSAGE, prepareExceptionDetails(USER_PRIVILEGE_MESSAGE));
                }
                ApiLimitDescriptor descriptor = mapper.readValue(data, ApiLimitDescriptor.class);
                if (descriptor != null) {
                    float time = (System.currentTimeMillis() - descriptor.getTime()) / 1000F;
                    if (time < descriptor.getRefreshTime()) {
                        if (descriptor.getLimit() > 0) {
                            descriptor.setLimit(descriptor.getLimit() - 1);
                            String apiJson = mapper.writeValueAsString(descriptor);
                            redis.hset(apiRecord, user, apiJson);
                        } else {
                            throw new ApiLimitException(API_LIMIT_REACHED_MESSAGE, prepareExceptionDetails(API_REFRESH_TIME_MESSAGE + (descriptor.getRefreshTime() - time), API_CUSTOMER_CARE_MESSAGE));
                        }
                    } else {
                        descriptor.setLimit(descriptor.getDefaultLimit() - 1);
                        descriptor.setTime(System.currentTimeMillis());
                        String jsonData = mapper.writeValueAsString(descriptor);
                        redis.hset(apiRecord, user, jsonData);
                    }
                }
            }
        } else {
            throw new InvalidApiUserException(String.format(USER_NOT_VALID, user), prepareExceptionDetails(USER_PRIVILEGE_MESSAGE + servletPath));
        }

        return true;
    }


    /**
     * Prepares the error details for the unfortunate event which results in Exception.
     *
     * @param details
     * @return
     */
    public ArrayList<String> prepareExceptionDetails(String... details) {
        errorDetails.clear();
        for (String error : details) {
            errorDetails.add(error);
        }
        return errorDetails;
    }
}
