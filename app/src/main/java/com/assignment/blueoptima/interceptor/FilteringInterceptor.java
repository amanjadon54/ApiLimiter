package com.assignment.blueoptima.interceptor;

import com.assignment.blueoptima.AllowedApi;
import com.assignment.blueoptima.exception.ApiLimitException;
import com.assignment.blueoptima.exception.InvalidApiUserException;
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
        //path needs to be checked in list api record

        Boolean found = redis.sismember(USER_RECORD, user);
        if (found == true) {
            log.info(String.format("MATCHED IN ENUM THE {} : {}"), path, user);
            String apiRecord = redis.hget("apiRecord", path);
            if (apiRecord != null && !apiRecord.equals("")) {
                String data = redis.hget(apiRecord, user);
                if (data != null && !data.equals("")) {
                    String values[] = data.split(",");
                    float time = (System.currentTimeMillis() - Long.parseLong(values[1])) / 1000F;
                    if (time < 60) {
                        if ((Integer.parseInt(values[0])) > 0) {
                            values[0] = String.valueOf(Integer.parseInt(values[0]) - 1);
                            redis.hset(apiRecord, user, values[0] + "," + values[1] + "," + values[2]);
                        } else {
                            throw new ApiLimitException(API_LIMIT_REACHED_MESSAGE, prepareExceptionDetails(API_REFRESH_TIME_MESSAGE, API_CUSTOMER_CARE_MESSAGE));
                        }
                    } else {
                        values[0] = String.valueOf(Integer.parseInt(values[2]) - 1);
                        values[1] = String.valueOf(System.currentTimeMillis());
                        redis.hset(apiRecord, user, values[0] + "," + values[1] + "," + values[2]);
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
