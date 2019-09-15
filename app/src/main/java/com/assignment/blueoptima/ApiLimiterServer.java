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

    /**
     * Component for the connection to redis.
     * using default configuration of redis which says host:localhost and port : 6379
     * @return
     */
    @Bean
    public Jedis jedis() {
        return new Jedis();
    }
}
