package com.assignment.blueoptima.handler;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApiLimitHandler {

    @GetMapping("/health")
    public Object health(){
        return "pong";
    }

}
