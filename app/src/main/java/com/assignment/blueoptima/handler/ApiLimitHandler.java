package com.assignment.blueoptima.handler;

import com.assignment.blueoptima.ApiLimitDescriptor;
import com.assignment.blueoptima.exception.InvalidApiUserException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import redis.clients.jedis.Jedis;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static com.assignment.blueoptima.ApiLimitConstants.*;

@RestController
@Log4j2
public class ApiLimitHandler {

    @Autowired
    Jedis redis;

    @Autowired
    ObjectMapper mapper;

    @GetMapping("/health")
    public Object health() {
        return "pong";
    }

    @GetMapping("v1")
    public Object testV1() {
        return "OK from V1";
    }

    @GetMapping("v2")
    public Object testV2() {
        return "OK from V2";
    }

    @PostMapping("/admin/config")
    public Object addDefaultConfig(@RequestBody ArrayList<ApiLimitDescriptor> apiLimitDescriptors) {
        log.info("/admin/config invoked");
        for (ApiLimitDescriptor limitDescriptor : apiLimitDescriptors) {
            redis.hset(API_RECORD, limitDescriptor.getApiName(), limitDescriptor.getApiName() + API_RECORD_DATA);
            Set<String> userRecord = redis.smembers(USER_RECORD);
            for (String user : userRecord) {
                log.info(String.format("/Inserting record of user {}, for api {} with limit to {} and default value of {}"), user, limitDescriptor.getApiName(), limitDescriptor.getLimit(), limitDescriptor.getLimit());
                redis.hset(limitDescriptor.getApiName() + API_RECORD_DATA, user,
                        limitDescriptor.getLimit() + "," + System.currentTimeMillis() + "," + limitDescriptor.getLimit());
            }
        }
        return RECORDS_UPDATED;

    }

    @PutMapping("/admin/config/{user}")
    public Object resetUserConfig(@RequestBody @NotNull ApiLimitDescriptor apiLimitDescriptor,
                                  @PathVariable @NotNull String user) {
        String mapName = redis.hget(API_RECORD, apiLimitDescriptor.getApiName());
        if (mapName != null) {
            String dataMap = redis.hget(mapName, user);
            if (dataMap != null && !dataMap.equals("")) {
                String[] values = dataMap.split(",");
                values[0] = String.valueOf(apiLimitDescriptor.getLimit());
                values[2] = String.valueOf(apiLimitDescriptor.getLimit());
                redis.hset(mapName, user, values[0] + "," + values[1] + "," + values[2]);
            } else {
                Boolean isMember = redis.sismember(USER_RECORD, user);
                if (isMember) {
                    redis.hset(mapName, user, apiLimitDescriptor.getLimit() + "," + System.currentTimeMillis() + "," + apiLimitDescriptor.getDefaultLimit());
                } else {
                    throw new InvalidApiUserException("User data not valid", null);
                }
            }
        } else {
            throw new InvalidApiUserException("Api does not exist", null);
        }
        return redis.hget(mapName, user);
    }


    /***
     *
     * @param apiName
     * @return
     */

    @GetMapping("/admin/config/{apiName}")
    public Object exposeRedis(@PathVariable String apiName) {
        return redis.hgetAll(apiName + API_RECORD_DATA);
    }


    /**
     * APIS RELATED TO USERS MANAGEMENT
     */

    @GetMapping("/admin/users")
    public Object fetchUsers() {
        return redis.smembers(USER_RECORD);
    }

    @PostMapping("/admin/users")
    public Object addUsers(@RequestBody String[] users) {
        long status = 0;
        if (users != null) {
            status = redis.sadd(USER_RECORD, users);
        }
        return status + RECORDS_UPDATED;
    }

    @DeleteMapping("/admin/users")
    public Object deleteUsers(@RequestBody String[] users) {
        long status = 0;
        if (users != null && users.length > 0) {
            status = redis.srem(USER_RECORD, users);
            if (status != 0) {
                Map<String, String> apiRecord = redis.hgetAll(API_RECORD);
                Collection<String> apiUsers = apiRecord.values();
                for (String apiDataName : apiUsers) {
                    redis.hdel(apiDataName, users);
                }
            }
        }
        return status + RECORDS_UPDATED;
    }
}
