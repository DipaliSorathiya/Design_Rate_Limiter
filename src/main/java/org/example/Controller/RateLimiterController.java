package org.example.Controller;

import org.example.Algorithms.IRateLimiter;
import org.example.CommonEnums.RateLimiterType;
import org.example.Factory.RateLimiterFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class RateLimiterController {

    private final IRateLimiter rateLimiter;
    private final ExecutorService executor;


    public RateLimiterController(RateLimiterType type, Map<String, Object> config, ExecutorService executorService){
        this.rateLimiter = RateLimiterFactory.createLimiter(type,config);
        this.executor = executorService;
    }

    public CompletableFuture<Boolean> processRequest(String rateLimitKey){
        return CompletableFuture.supplyAsync( () -> {
            boolean allowed = rateLimiter.giveAccess(rateLimitKey);
            if(allowed){
                System.out.printf("Request with Key ALlowed", rateLimitKey);
            }else {
                System.out.printf("Request with key Blocked", rateLimitKey);
            }
            return allowed;
        },executor);
    }

    public void updateConfiguration(Map<String,Object> config){
        rateLimiter.updateConfiguration(config);
    }

    public void shutdown() {
        rateLimiter.shutdown();
        executor.shutdownNow();
    }
}
