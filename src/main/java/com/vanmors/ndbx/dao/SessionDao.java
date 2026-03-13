package com.vanmors.ndbx.dao;

public interface SessionDao {

    Boolean refreshSession(String key, int ttlSeconds);

    Boolean createSession(String key, int ttlSeconds);
}
