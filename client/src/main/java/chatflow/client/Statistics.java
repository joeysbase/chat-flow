package chatflow.client;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class Statistics {
    public static final AtomicInteger sendReconnection=new AtomicInteger(0);
    public static final AtomicInteger receiveReconnection=new AtomicInteger(0);
    public static final AtomicInteger sendConnection=new AtomicInteger(0);
    public static final AtomicInteger receiveConnection=new AtomicInteger(0);
    public static final AtomicInteger succeedMessage=new AtomicInteger(0);
    public static final AtomicInteger failedMessage=new AtomicInteger(0);
    public static final Queue<Long> succeedMessageRoundtripTime=new ConcurrentLinkedQueue<>();
    public static final Queue<Long> throughputoverTime=new ConcurrentLinkedQueue<>();
}
