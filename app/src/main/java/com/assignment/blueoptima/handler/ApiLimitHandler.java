package com.assignment.blueoptima.handler;

import com.assignment.blueoptima.store.RedisStore;
import com.assignment.blueoptima.store.Storage;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApiLimitHandler {

    @GetMapping("/health")
    public Object health(){
        return "pong";
    }

    @GetMapping("/redis")
    public Object testRedis(){
        throw new RuntimeException();
//        Storage storage = new RedisStore();
//        return storage.test();
    }

}
