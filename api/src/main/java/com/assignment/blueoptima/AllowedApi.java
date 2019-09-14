package com.assignment.blueoptima;

public enum AllowedApi {
    REDIS("redis"),
    HEALTH("health"),
    V1("v1"),
    V2("v2");

    public final String apiLabel;

    AllowedApi(String apiLabel) {
        this.apiLabel = apiLabel;
    }

    public static AllowedApi valueOfApiLabel(String apiLabel) {
        for (AllowedApi api : values()) {
            if (api.apiLabel.equals(apiLabel)) {
                return api;
            }
        }
        return null;
    }

}
