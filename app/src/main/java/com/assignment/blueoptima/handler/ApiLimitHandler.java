package com.assignment.blueoptima.handler;

import com.assignment.blueoptima.ApiLimitDescriptor;
import com.assignment.blueoptima.exception.InvalidApiUserException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import redis.clients.jedis.Jedis;

import javax.validation.constraints.NotNull;
import java.io.IOException;
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

    /***
     * Provides the functionality to view the configuration of a service(api).
     * Only allowed for Admin with password protection.
     * @param apiName
     * @return
     */
    @GetMapping("/admin/config/{apiName}")
    public Object exposeConfig(@PathVariable String apiName) {
        return redis.hgetAll(apiName + API_RECORD_DATA);
    }

    /***
     * Service to Add configurations(default) related to endpoint and users.
     * Takes care of the threshold and default value for the users for specific APIs.
     * Provides the functionality only to admin and is password protected.
     * @param apiLimitDescriptors
     * @return
     */
    @PostMapping("/admin/config")
    public Object addDefaultConfig(@RequestBody ArrayList<ApiLimitDescriptor> apiLimitDescriptors) throws JsonProcessingException {
        log.info("/admin/config invoked");
        for (ApiLimitDescriptor limitDescriptor : apiLimitDescriptors) {
            redis.hset(API_RECORD, limitDescriptor.getApiName(), limitDescriptor.getApiName() + API_RECORD_DATA);
            Set<String> userRecord = redis.smembers(USER_RECORD);
            for (String user : userRecord) {
                log.info(String.format("/Inserting record of user {}, for api {} with limit to {} and default value of {}"), user, limitDescriptor.getApiName(), limitDescriptor.getLimit(), limitDescriptor.getLimit());
                String apiJson = mapper.writeValueAsString(limitDescriptor);
                log.info("apiJson" + apiJson);
                redis.hset(limitDescriptor.getApiName() + API_RECORD_DATA, user, apiJson);
            }
        }
        return RECORDS_UPDATED;

    }

    /***
     * Provides the functionality to Admin to modify the limit of specific user and api combination.
     * Admin can also add new configurations for users.
     * Password protected to allow only admin to make changes.
     * @param apiLimitDescriptor
     * @param user
     * @return
     */
    @PutMapping("/admin/config/{user}")
    public Object resetUserConfig(@RequestBody @NotNull ApiLimitDescriptor apiLimitDescriptor,
                                  @PathVariable @NotNull String user) throws JsonProcessingException, IOException {
        String mapName = redis.hget(API_RECORD, apiLimitDescriptor.getApiName());
        if (mapName != null) {
            String dataMap = redis.hget(mapName, user);
            //Add/Modify user configuration if it doesnot exist for this Api , provided user is valid
            Boolean isMember = redis.sismember(USER_RECORD, user);
            if (isMember) {
                String apiData = mapper.writeValueAsString(apiLimitDescriptor);
                redis.hset(mapName, user, apiData);
            } else {
                throw new InvalidApiUserException("User data not valid", null);
            }
        } else {
            throw new InvalidApiUserException("Api does not exist", null);
        }
        return redis.hget(mapName, user);
    }

    @GetMapping("/admin/api")
    public Object fetchCapturedApis() {
        return redis.hgetAll(API_RECORD);
    }

    @PutMapping("/admin/api/{api}")
    public Object addApiForConfiguration(@PathVariable String api) {
        long status = 0;
        if (api != null) {
            status = redis.hset(API_RECORD, api, api + API_RECORD_DATA);
        }
        return status + RECORDS_UPDATED;
    }

    @DeleteMapping("/admin/api/{api}")
    public Object removeApiFromCapturing(@PathVariable String api) {
        long status = 0;
        if (api != null) {
            //delete the api from api record.
            String apiData = redis.hget(API_RECORD, api);
            //Also, remove the api related configuration
            if (apiData != null && !apiData.equals("")) {
                redis.del(apiData);
                status = redis.hdel(API_RECORD, api);
            }
        }
        return status + RECORDS_UPDATED;
    }

    /**
     * APIS RELATED TO USERS MANAGEMENT
     */
    /**
     * Admin can get all users/client
     *
     * @return
     */
    @GetMapping("/admin/users")
    public Object fetchUsers() {
        return redis.smembers(USER_RECORD);
    }

    /**
     * Admin can add users/clients.
     *
     * @param users
     * @return
     */
    @PostMapping("/admin/users")
    public Object addUsers(@RequestBody String[] users) {
        long status = 0;
        if (users != null) {
            status = redis.sadd(USER_RECORD, users);
        }
        return status + RECORDS_UPDATED;
    }

    /**
     * Admin can delete multiple users/clients in a single go.
     * Also, results in deleting the configuration of that users
     *
     * @param users
     * @return
     */
    @DeleteMapping("/admin/users")
    public Object deleteUsers(@RequestBody String[] users) {
        long status = 0;
        if (users != null && users.length > 0) {
            //delete users from user database.
            status = redis.srem(USER_RECORD, users);
            if (status != 0) {
                //Also, remove this deleted user configuration of apis.
                Map<String, String> apiRecord = redis.hgetAll(API_RECORD);
                Collection<String> apiUsers = apiRecord.values();
                for (String apiDataName : apiUsers) {
                    redis.hdel(apiDataName, users);
                }
            }
        }
        return status + RECORDS_UPDATED;
    }


    /**
     * Health check of server api
     *
     * @return
     */
    @GetMapping("/health")
    public Object health() {
        return "pong";
    }

    /**
     * Test api to test the functionality.
     * This api takes userName in header so as to allow user to access it.
     *
     * @return
     */
    @GetMapping("v1")
    public Object testV1() {
        return "OK from V1";
    }

    /**
     * Another api to test the functionality.
     * This api takes userName in header so as to allow user to access it.
     *
     * @return
     */
    @GetMapping("v2")
    public Object testV2() {
        return "OK from V2";
    }

    @GetMapping("redis")
    public Object redisTest() {
        return "OK from redis";
    }


}
