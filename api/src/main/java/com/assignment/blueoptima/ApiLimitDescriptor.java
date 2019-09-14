package com.assignment.blueoptima;

import lombok.Data;

@Data
public class ApiLimitDescriptor {
    private String apiName;
    private int limit;
    private long time;
    private int defaultLimit;
    private float refreshTime;
}
