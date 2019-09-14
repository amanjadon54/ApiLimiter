package com.assignment.blueoptima.interceptor;

import com.assignment.blueoptima.AllowedApi;
import com.assignment.blueoptima.exception.ApiLimitException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import redis.clients.jedis.Jedis;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;

@Log4j2
public class FilteringInterceptor extends HandlerInterceptorAdapter {

    @Autowired
    Jedis redis;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        log.info("\n--------   Interception For API LIMIT  --- ");
        String user = request.getHeader("userName");
        String servletPath = request.getServletPath();
        log.info(String.format("requested url is : "), servletPath);
        String path = servletPath.substring(1, servletPath.length());
        if (AllowedApi.valueOfApiLabel(path) != null && user != null) {
            log.info(String.format("MATCHED IN ENUM THE {} : ,{}"), path, user);
            String userRecord = redis.hget("apiRecord", path);
            if (userRecord != null && !userRecord.equals("")) {
                String data = redis.hget(userRecord, user);
                String values[] = data.split(",");

                float time = (System.currentTimeMillis() - Long.parseLong(values[1])) / 1000F;

                if (time < 120) {
                    if ((Integer.parseInt(values[0])) > 0) {
                        values[0] = String.valueOf(Integer.parseInt(values[0]) - 1);
                        redis.hset(userRecord, user, values[0] + "," + values[1]);
                    } else {
                        ArrayList<String> errorDetails = new ArrayList<>();
                        errorDetails.add("1. Please wait for refreshing time as alloted in your plan");
                        errorDetails.add("2. If the issue persist, please contact customer care");
                        throw new ApiLimitException("Limit for Accessing service reached maximum limit", errorDetails);
                    }
                } else {
                    values[0] = String.valueOf(Integer.parseInt(values[0]) - 1);
                    values[1] = String.valueOf(System.currentTimeMillis());
                    redis.hset(userRecord, user, values[0] + "," + values[1]);
                }
            }
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {
        System.out.println("\n-------- LogInterception.afterCompletion --- ");

        long startTime = (Long) request.getAttribute("startTime");
        long endTime = System.currentTimeMillis();
        System.out.println("Request URL: " + request.getRequestURL());
        System.out.println("End Time: " + endTime);

        System.out.println("Time Taken: " + (endTime - startTime));
    }
}
