package com.assignment.blueoptima.store;

public interface Storage {

    Object fetch(String key);

    Object persist();


}
