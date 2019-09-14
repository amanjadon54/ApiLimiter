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

@Log4j2
@Component
public class FilteringInterceptor extends HandlerInterceptorAdapter {

    @Autowired
    Jedis redis;

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

        ArrayList<String> errorDetails = new ArrayList<>();
        Boolean found = redis.sismember(USER_RECORD, user);
        if (AllowedApi.valueOfApiLabel(path) != null && found == true) {
            log.info(String.format("MATCHED IN ENUM THE {} : {}"), path, user);
            String userRecord = redis.hget("apiRecord", path);
            if (userRecord != null && !userRecord.equals("")) {
                String data = redis.hget(userRecord, user);
                if (data != null && !data.equals("")) {
                    String values[] = data.split(",");
                    float time = (System.currentTimeMillis() - Long.parseLong(values[1])) / 1000F;
                    if (time < 60) {
                        if ((Integer.parseInt(values[0])) > 0) {
                            values[0] = String.valueOf(Integer.parseInt(values[0]) - 1);
                            redis.hset(userRecord, user, values[0] + "," + values[1] + "," + values[2]);
                        } else {
                            errorDetails.clear();
                            errorDetails.add("1. Please wait for refreshing time as alloted in your plan");
                            errorDetails.add("2. If the issue persist, please contact customer care");
                            throw new ApiLimitException("Limit for Accessing service reached maximum limit", errorDetails);
                        }
                    } else {
                        //assign the count with default count
                        values[0] = String.valueOf(Integer.parseInt(values[2]) - 1);
                        values[1] = String.valueOf(System.currentTimeMillis());
                        redis.hset(userRecord, user, values[0] + "," + values[1] + "," + values[2]);
                    }
                } else {
                    throw new InvalidApiUserException("user does not have privilege to this api", null);
                }
            }
        } else {
            errorDetails.clear();
            errorDetails.add(" Please check with team if the user has access to the current api endpoint " + servletPath);
            throw new InvalidApiUserException(String.format("Invalid User details {}", user), errorDetails);
        }

        return true;
    }

}
