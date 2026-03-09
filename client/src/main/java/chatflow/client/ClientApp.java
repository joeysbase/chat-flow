package chatflow.client;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ClientApp {
    public static void main(String[] args) {
        ThroughputMonitor monitor = new ThroughputMonitor(10);
        ProgressMonitor progressMonitor = new ProgressMonitor(1);
        ExecutorService testPool = Executors.newFixedThreadPool(64);
        ExecutorService warmupPool = Executors.newFixedThreadPool(32);
        ExecutorService monitorPool = Executors.newFixedThreadPool(5);
        List<ClientThread> clients = new LinkedList<>();
        try {
            if (args.length != 2) {
                System.err.println("Usage: java ClientApp <send_server_ip> <receive_server_ip>");
                System.exit(1);
            }
            long totalTime = 0;
            String sendServerIp = args[0];
            String receiveServerIp = args[1];
            
            System.out.println("Generating warmup messages...");
            MessagePool.generateMessage(32000);
            System.out.println("Warmup phase...");
            monitorPool.submit(progressMonitor);
            monitorPool.submit(monitor);
            long warmupStartTime = System.currentTimeMillis();
            for (int i = 0; i < 10; i++) {
                ClientThread clientThread = new ClientThread(sendServerIp, receiveServerIp);
                clients.add(clientThread);
                warmupPool.submit(clientThread);
            }
            warmupPool.shutdown();
            warmupPool.awaitTermination(10, TimeUnit.MINUTES);
            long warmupEndTime = System.currentTimeMillis();
            totalTime += warmupEndTime - warmupStartTime;
            monitor.stop();
            progressMonitor.stop();
            System.out.println("Warmup completed in " + (warmupEndTime - warmupStartTime) / 1000 + " s");

            System.out.println("Generating test messages...");
            MessagePool.generateMessage(500000);
            System.out.println("Test phase...");
            monitorPool.submit(progressMonitor);
            // monitorPool.submit(monitor);
            long testStartTime = System.currentTimeMillis();
            for (int i = 0; i < 10; i++) {
                ClientThread clientThread = new ClientThread(sendServerIp, receiveServerIp);
                clients.add(clientThread);
                testPool.submit(clientThread);
            }
            testPool.shutdown();
            testPool.awaitTermination(10, TimeUnit.MINUTES);
            long testEndTime = System.currentTimeMillis();
            monitor.stop();
            // progressMonitor.stop();
            
            totalTime += testEndTime - testStartTime;
            System.out.println("Test completed in " + (testEndTime - testStartTime) / 1000 + " s");
            System.out.println("Total time: " + totalTime / 1000 + " s");
            System.out.println("Throughput: " + (Statistics.succeedMessage.get() / (totalTime / 1000)) + " msg/s");
            Thread.sleep(1000*60*10);
            for(ClientThread client : clients){
                client.cleanup();
            }
        } catch (InterruptedException e) {
            System.err.println("ClientApp interrupted: " + e.getMessage());
            monitor.stop();
            progressMonitor.stop();
            warmupPool.shutdownNow();
            testPool.shutdownNow();
        }finally {
            for(ClientThread client : clients){
                client.cleanup();
            }
            monitor.stop();
            progressMonitor.stop();
            warmupPool.shutdownNow();
            testPool.shutdownNow();
        }

    }
}
