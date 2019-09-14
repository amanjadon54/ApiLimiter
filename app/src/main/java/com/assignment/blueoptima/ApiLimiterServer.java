package com.assignment.blueoptima;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import redis.clients.jedis.Jedis;

@SpringBootApplication
public class ApiLimiterServer {
    public static void main(String[] args) {
        SpringApplication.run(ApiLimiterServer.class, args);
    }

    @Bean
    public Jedis jedis() {
        return new Jedis();
    }
}
