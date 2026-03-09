package chatflow.consumer;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

public class CacheMonitor implements Runnable {
    private long timeToWait;
    private final long timeToLive;
    private final AtomicBoolean running=new AtomicBoolean(true);

    public CacheMonitor(long timeToLive){
        this.timeToLive=timeToLive;
        this.timeToWait=timeToLive;
    }

    @Override
    public void run(){
        while(running.get()){
            // System.out.println("CacheMonitor: Checking cache for expired entries...");
            // System.out.println("Current cache size: " + IdempotencyCache.size());
            Instant currentTimestamp=Instant.now();
            for(String key:IdempotencyCache.createdTime.keySet()){
                Instant timeCreated=IdempotencyCache.createdTime.get(key);
                long timeElapsed=currentTimestamp.minusMillis(timeCreated.toEpochMilli()).toEpochMilli();
                if(timeElapsed>=timeToLive){
                    IdempotencyCache.remove(key);
                }else if(timeElapsed>0){
                    timeToWait=Math.min(timeToWait,timeElapsed);
                }else{
                    timeToWait=timeToLive;
                }
            }
            try {
                Thread.sleep(timeToWait);
            } catch (InterruptedException e) {

            }
        }
    }

    public void stop(){
        running.set(false);
    }

}
