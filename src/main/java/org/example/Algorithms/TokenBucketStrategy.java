package org.example.Algorithms;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class TokenBucketStrategy implements IRateLimiter {

    private final int bucketCapacity;  // maximum tokens per buckets
    private volatile int refreshRate; // tokens added per refill interval (per second)

    private final Bucket globalBucket;
    private final ConcurrentHashMap<String,Bucket> userBuckets;

    private final ScheduledExecutorService scheduler;
    private final long refillIntervalMillis;



    private class Bucket {
        private int tokens;
        private final ReentrantLock lock = new ReentrantLock();

        public Bucket(int initialTokens){
            this.tokens = initialTokens;
        }

        public boolean tryConsume() {
            lock.lock();
            try{
                if(tokens > 0) {
                    tokens--;
                    return true;
                }
                return false;
            } finally {
                 lock.unlock();
            }
        }

        /*
         refills the bucket by adding refreshRate tokens without exceeeding bucketCapacity

         */

        public void refill(){
            lock.lock();
            try {
                tokens = Math.min(bucketCapacity,tokens + refreshRate);
            }finally {
                {
                    lock.unlock();
                }
            }
        }




    }

    /*
       constructs a TokenBucketStrategy
     */
    public TokenBucketStrategy(int bucketCapacity, int refreshRate) {
        this.bucketCapacity = bucketCapacity;
        this.refreshRate = refreshRate;
        this.globalBucket = new Bucket(bucketCapacity);
        this.userBuckets = new ConcurrentHashMap<>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.refillIntervalMillis = 1000;
        startRefillTask();
    }

    private void startRefillTask() {
        scheduler.scheduleAtFixedRate(() -> {
            globalBucket.refill();
            for(Bucket bucket:userBuckets.values()){
                bucket.refill();
            }
        },refillIntervalMillis,refillIntervalMillis, TimeUnit.MILLISECONDS);
    }




    @Override
    public boolean giveAccess(String rateLimitKey) {
        if(rateLimitKey != null && rateLimitKey.isEmpty()){
            Bucket bucket = userBuckets.computeIfAbsent(rateLimitKey,key -> new Bucket(bucketCapacity));
            return bucket.tryConsume();
        }
        else {
            return globalBucket.tryConsume();
        }
    }

    @Override
    public void updateConfiguration(Map<String, Object> config) {
            if(config.containsKey("refreshRate")){
                this.refreshRate = (int)config.get("refreshRate");
            }
    }

    @Override
    public void shutdown() {
        scheduler.shutdownNow();
    }
}
