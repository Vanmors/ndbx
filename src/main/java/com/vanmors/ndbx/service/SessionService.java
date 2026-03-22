package com.vanmors.ndbx.service;

import com.vanmors.ndbx.entity.User;

import java.util.Optional;


public interface SessionService {

    String createOrRefreshSession(String existingSid);

    Optional<String> getExistingSessionId(String sid);

    void attachUserToSession(String sid, String id);

    Optional<String> getUserIdFromSession(String sid);

    void deleteSession(String sid);
}
