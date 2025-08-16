package org.example.Factory;

import org.example.Algorithms.IRateLimiter;
import org.example.Algorithms.TokenBucketStrategy;
import org.example.CommonEnums.RateLimiterType;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class RateLimiterFactory {

    private static final Map<RateLimiterType, Function<Map<String,Object>, IRateLimiter>> limiterFactories= new HashMap<>();

    static {
        limiterFactories.put(RateLimiterType.TOKEN_BUCKET,config -> {
            int capacity = (int) config.getOrDefault("capacity",10);
            int refreshRate;
            if(config.containsKey("refreshRate")){
                refreshRate = (int)config.get("refreshRate");
            }else {
                double tokensPerSecond = (double) config.getOrDefault("tokensPerSecond",10.0);
                refreshRate = (int)Math.round(tokensPerSecond);
            }
            return new TokenBucketStrategy(capacity,refreshRate);
        });
    }

    public static IRateLimiter createLimiter(RateLimiterType type, Map<String,Object> config){
        Function<Map<String,Object>,IRateLimiter> factory = limiterFactories.get(type);
        if(factory == null){
            throw new IllegalArgumentException("Unsupported rate limitor type: "+type);
        }
        return factory.apply(config);
    }

    public static void registerLimiterFactory(RateLimiterType type,Function<Map<String,Object>,IRateLimiter> factory){
        limiterFactories.put(type,factory);
    }
}
