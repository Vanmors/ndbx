package com.vanmors.ndbx.dao;

public interface SessionDao {

    void refreshSession(String key, int ttlSeconds);

    void attachToUser(String key, String userId, int ttlSeconds);

    void createSession(String key, int ttlSeconds);
}
