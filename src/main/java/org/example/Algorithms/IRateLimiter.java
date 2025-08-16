package org.example.Algorithms;

import java.util.Map;

public interface IRateLimiter  {

    boolean giveAccess(String rateLimitKey);

    void updateConfiguration(Map<String,Object> config);

    void shutdown();
}
