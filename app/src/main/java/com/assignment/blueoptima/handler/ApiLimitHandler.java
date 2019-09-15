package com.assignment.blueoptima.handler;

import com.assignment.blueoptima.ApiLimitDescriptor;
import com.assignment.blueoptima.client.HttpClient;
import com.assignment.blueoptima.exception.ApiLimitException;
import com.assignment.blueoptima.exception.InvalidApiUserException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiOperation;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import redis.clients.jedis.Jedis;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static com.assignment.blueoptima.ApiLimitConstants.*;
import static com.assignment.blueoptima.exception.ExceptionConstants.*;

@RestController
@Log4j2
public class ApiLimitHandler {

    @Autowired
    Jedis redis;

    @Autowired
    ObjectMapper mapper;


    /**
     * GROUP OF APIs to add/view/modify the configuration related to service.
     **/
    @ApiOperation(value = "Provides the functionality only to admin and is password protected. " +
            "API is to be given as - seperated values, like if the api to track is v1/v2 , " +
            "then provide apiName as v1-v2. Password protected")
    @GetMapping("/admin/config/{apiName}")
    public Object exposeConfig(@PathVariable String apiName) {
        return redis.hgetAll(apiName + API_RECORD_DATA);
    }


    @ApiOperation(value = "Service to Add configurations(default) related to endpoint + users.\n" +
            "Takes care of the threshold and default value for the users for specific APIs along with the refreshing period.\n" +
            "Provides the functionality only to admin and is password protected.")
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


    @ApiOperation(value = "Provides the functionality to Admin to modify the limit of specific user + api combination.\n" +
            "Admin can also add new configurations for users like change limit, default limit and refreshing period.\n" +
            "Password protected to allow only admin to make changes.")
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


    @ApiOperation(value = "Fetches the list of all services our limiting application is keeping a track of. Password protected.")
    @GetMapping("/admin/api")
    public Object fetchCapturedApis() {
        return redis.hgetAll(API_RECORD);
    }

    @ApiOperation(value = "Provides a mechanism to allow to track a service by our limiting application. Password protected")
    @PutMapping("/admin/api/{api}")
    public Object addApiForConfiguration(@PathVariable String api) {
        long status = 0;
        if (api != null) {
            status = redis.hset(API_RECORD, api, api + API_RECORD_DATA);
        }
        return status + RECORDS_UPDATED;
    }

    @ApiOperation(value = "To remove a service from our limiting application tracking. Will remove users mapping with this service too. Password protected.")
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


    @ApiOperation(value = "Fetches the all list of users/client. Password protected")
    @GetMapping("/admin/users")
    public Object fetchUsers() {
        return redis.smembers(USER_RECORD);
    }

    @ApiOperation(value = "To add a user in our application. Password protected")
    @PostMapping("/admin/users")
    public Object addUsers(@RequestBody String[] users) {
        long status = 0;
        if (users != null) {
            status = redis.sadd(USER_RECORD, users);
        }
        return status + RECORDS_UPDATED;
    }

    @ApiOperation(value = "To remove a user from our user tracking. Also, results in deleting the configuration of that users from limiting appliation. Password protected.")
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

    @ApiOperation(value = "Service to check and limit the access on basis of limit time for users + api combination." +
            "Also keep track of refreshing period with default limit, once the limit has been exceeded.")
    @GetMapping("/limit/{apiName}")
    public Object checkLimit(@PathVariable String apiName, HttpServletRequest request) throws IOException {
        log.info("--------   TRYING TO FETCH LIMIT FOR USER AND API  --- ");
        String user = request.getHeader("userName");
        log.info(String.format("requested path is : {}"), apiName);
        Boolean apiFound = redis.hexists(API_RECORD, apiName);
        Boolean userFound = false;
        //checking if we are capturing the provided api or not.
        if (apiFound == true && user != null) {
            userFound = redis.sismember(USER_RECORD, user);
        } else {
            throw new ApiLimitException(USER_PRIVILEGE_MESSAGE, prepareExceptionDetails(API_NOT_CAPTURING_MESSAGE, USER_NOT_VALID));
        }

        //checking if the user has access to this api at all or not.
        if (userFound == true) {
            String apiRecord = redis.hget(API_RECORD, apiName);
            if (apiRecord != null && !apiRecord.equals("")) {
                String data = redis.hget(apiRecord, user);
                if (data == null) {
                    throw new ApiLimitException(USER_PRIVILEGE_MESSAGE, prepareExceptionDetails(USER_PRIVILEGE_MESSAGE));
                }
                ApiLimitDescriptor descriptor = mapper.readValue(data, ApiLimitDescriptor.class);
                if (descriptor != null) {
                    //calculating the limit , once the user has accessed the api.
                    float time = (System.currentTimeMillis() - descriptor.getTime()) / 1000F;
                    if (time < descriptor.getRefreshTime()) {
                        if (descriptor.getLimit() > 0) {
                            descriptor.setLimit(descriptor.getLimit() - 1);
                            String apiJson = mapper.writeValueAsString(descriptor);
                            redis.hset(apiRecord, user, apiJson);
                        } else {
                            throw new ApiLimitException(API_LIMIT_REACHED_MESSAGE, prepareExceptionDetails(API_REFRESH_TIME_MESSAGE + (int) (descriptor.getRefreshTime() - time), API_CUSTOMER_CARE_MESSAGE));
                        }
                    } else {
                        //once the limit reaches the threshold and the refreshing period is up, we refresh the limit again.
                        descriptor.setLimit(descriptor.getDefaultLimit() - 1);
                        descriptor.setTime(System.currentTimeMillis());
                        String jsonData = mapper.writeValueAsString(descriptor);
                        redis.hset(apiRecord, user, jsonData);
                    }
                }
            }
        } else {
            throw new InvalidApiUserException(String.format(USER_NOT_VALID, user), prepareExceptionDetails(USER_PRIVILEGE_MESSAGE + apiName));
        }

        return "ok";
    }


    @ApiOperation(value = "Server Health check service")
    @GetMapping("/health")
    public Object health() {
        return "pong";
    }

    @ApiOperation(value = "Test service to test the limit application functionality.")
    @GetMapping("/test/{api}")
    public Object testV1(HttpServletRequest request, @PathVariable String api) throws Exception {
        HttpClient.executeHttpClient("http://localhost:9999/limit/", api, request.getHeader("userName"));
        return "OK from test";
    }

}
