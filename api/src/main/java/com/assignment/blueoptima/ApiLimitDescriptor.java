package com.assignment.blueoptima;

import lombok.Data;

/**
 * POJO class to hold the data related to API+USER combination
 */
@Data
public class ApiLimitDescriptor {
    private String apiName;
    private int limit;
    private long time;
    private int defaultLimit;
    private float refreshTime;
}
