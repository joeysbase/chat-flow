package chatflow.client;

import java.util.concurrent.atomic.AtomicBoolean;

public class ThroughputMonitor implements Runnable{
    private final int intervalSeconds;
    private long startTime;
    private final AtomicBoolean running=new AtomicBoolean(true);

    public ThroughputMonitor(int intervalSeconds) {
        this.intervalSeconds = intervalSeconds;
    }

    public void stop(){
        running.set(false);
    }

    public void start(){
        running.set(true);
    }

    @Override
    public void run() {
        this.startTime = System.currentTimeMillis();
        while(running.get()){
            int succeed=Statistics.succeedMessage.get();
            int acked=Statistics.ackedMessage.get();
            int failed=Statistics.failedMessage.get();
            int sendConn=Statistics.sendConnection.get();
            int sendReconn=Statistics.sendReconnection.get();
            int recvConn=Statistics.receiveConnection.get();
            int recvReconn=Statistics.receiveReconnection.get();
            long currentTime=System.currentTimeMillis();
            long timeElapsed=(currentTime-startTime)/1000;
            System.out.println("\nx---------- Throughput Monitor Start-----x");
            System.out.println("Throughput in last "+intervalSeconds+" seconds: Throughput -> "+(float)succeed/(float)timeElapsed+" msg/s. Acked -> "+(float)acked/(float)timeElapsed+" msg/s");
            System.out.println("Statistics in last "+intervalSeconds+" seconds: succeed="+succeed+", failed="+failed+", sendConn="+sendConn+", sendReconn="+sendReconn+", recvConn="+recvConn+", recvReconn="+recvReconn);
            System.out.println("x---------- Throughput Monitor End-----x\n");
            // System.out.println(ConnectionManager.receiveConnections.keySet());
            try {
                Thread.sleep(intervalSeconds*1000);
            } catch (InterruptedException e) {
                
            }
            
        }
    }    
}
