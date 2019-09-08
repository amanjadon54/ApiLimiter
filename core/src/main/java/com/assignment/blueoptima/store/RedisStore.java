package com.assignment.blueoptima.store;

import redis.clients.jedis.Jedis;

public class RedisStore implements Storage{

    Jedis jedis;

    public RedisStore(){
        jedis = new Jedis();
    }


    @Override
    public Object fetch(String key) {
        return null;
    }

    @Override
    public Object persist() {
        return null;
    }

    public Object getList(String listName){
        return null;
    }

    public Object getMap(String mapName){
        return null;
    }

    public Object getSortedSet(String sortedSetName){
        return null;
    }


    public Object test(){
        jedis.set("events/city/rome", "32,15,223,828");
        String cachedResponse = jedis.get("events/city/rome");
        return cachedResponse;
    }
}
