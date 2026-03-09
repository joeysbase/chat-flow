package chatflow.client;

import java.util.concurrent.atomic.AtomicBoolean;

public class ProgressMonitor implements Runnable{
    private final int intervalSeconds;
    private final AtomicBoolean running=new AtomicBoolean(true);

    public ProgressMonitor(int intervalSeconds) {
        this.intervalSeconds = intervalSeconds;
    }

    public void stop(){
        running.set(false);
    }

    @Override
    public void run() {
        while(running.get()){
            System.out.println("x---------- Progress Monitor Start-----x");
            System.out.println(Float.valueOf(MessagePool.messageQueue.size())/Float.valueOf(MessagePool.totalMessagesGenerated)+"% messages left.");
            System.out.println("x---------- Progress Monitor End-----x");
            try {
                Thread.sleep(intervalSeconds*1000);
            } catch (InterruptedException e) {
                
            }
        }
    }
    
}
