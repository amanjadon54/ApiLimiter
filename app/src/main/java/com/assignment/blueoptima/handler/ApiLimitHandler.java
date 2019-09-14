package com.assignment.blueoptima.handler;

import com.assignment.blueoptima.ApiLimitDescriptor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import redis.clients.jedis.Jedis;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@RestController
@Log4j2
public class ApiLimitHandler {

    @Autowired
    Jedis redis;

    private static final String USER_RECORD = "userRecord";
    private static final String API_RECORD = "apiRecord";
    private static final String API_RECORD_DATA = "Data";

    @GetMapping("/health")
    public Object health() {
        return "pong";
    }

    @GetMapping("/redis")
    public Object testRedis() {
        throw new RuntimeException();
//        Storage storage = new RedisStore();
//        return storage.test();
    }

    @GetMapping("v1")
    public Object testV1() {
        return "OK";
    }

    @GetMapping("v2")
    public Object testV2() {
        return "OK";
    }

    @PostMapping("/admin/config")
    public Object addDefaultConfig(@RequestBody ArrayList<ApiLimitDescriptor> apiLimitDescriptors) {
        for (ApiLimitDescriptor limitDescriptor : apiLimitDescriptors) {
            redis.hset(API_RECORD, limitDescriptor.getApiName(), limitDescriptor.getApiName() + API_RECORD_DATA);
            List<String> userRecord = redis.lrange(USER_RECORD, 0, -1);
            for (String user : userRecord) {
                redis.hset(limitDescriptor.getApiName() + API_RECORD_DATA, user,
                        limitDescriptor.getLimit() + "," + System.currentTimeMillis());
            }
        }
        return null;

    }

    @PutMapping("/admin/config/{apiName}/{user}/{limit}")
    public Object resetUserConfig(@PathVariable @NotNull String apiName,
                                  @PathVariable @NotNull String user,
                                  @PathVariable @NotNull Integer limit) {
        String mapName = redis.hget(API_RECORD, apiName);
        if (mapName != null) {
            String dataMap = redis.hget(mapName, user);
            if (dataMap != null && !dataMap.equals("")) {
                String[] values = dataMap.split(",");
                values[0] = String.valueOf(limit);
                redis.hset(mapName, user, values[0] + "," + values[1]);
            } else {
                throw new RuntimeException("User data not valid");
            }
        } else {
            throw new RuntimeException("Api name not valid");
        }
        return redis.hget(mapName, user);
    }


    /***
     *
     * @param apiName
     * @return
     */

    @GetMapping("/admin/config/{api}")
    public Object exposeRedis(@PathVariable String apiName) {
        return redis.hgetAll(apiName + API_RECORD_DATA);
    }


    /**
     * APIS RELATED TO USERS MANAGEMENT
     */

    @GetMapping("/admin/users")
    public Object fetchUsers() {
        return redis.lrange(USER_RECORD, 0, -1);
    }

    @PostMapping("/admin/users")
    public Object addUsers(@RequestBody ArrayList<String> users) {
        long status = 0;
        if (users != null && users.size() != 0) {
            for (String user : users) {
                status = redis.lpush(USER_RECORD, user);
            }
        }
        return status;
    }

    @DeleteMapping("/admin/users/{userId}")
    public Object addUsers(@PathVariable String user) {
        long status = 0;
        if (user != null && !user.equals("")) {
            status = redis.lrem(USER_RECORD, 1, user);
        }
        return status;
    }
}
