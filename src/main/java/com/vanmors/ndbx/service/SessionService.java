package com.vanmors.ndbx.service;

import java.util.Optional;


public interface SessionService {

    String createOrRefreshSession(String existingSid);

    Optional<String> getExistingSessionId(String sid);
}
