package chatflow.client;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import jakarta.websocket.ContainerProvider;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.WebSocketContainer;

public class ClientThread implements Runnable {
    private ClientSendEndPoint sendConnection = null;
    private final Map<String, ClientReceiveEndPoint> receiveConnections = new HashMap<>();
    private final String sendServerIp;
    private final String receiveServerIp;

    public ClientThread(String sendIp, String receiveIp) {
        this.sendServerIp = sendIp;
        this.receiveServerIp = receiveIp;
    }

    private void connectToSendServer() {
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            ClientSendEndPoint client = new ClientSendEndPoint();
            container.connectToServer(client, URI.create("ws://"+sendServerIp+":8080/send"));
            sendConnection = client;
        } catch (DeploymentException | IOException e) {
        }
    }

    private void connectToReceiveServer(String roomId) {
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            ClientReceiveEndPoint client = new ClientReceiveEndPoint();
            container.connectToServer(client, URI.create("ws://"+receiveServerIp+":9090/receive/room/" + roomId));
            receiveConnections.put(roomId, client);
        } catch (DeploymentException | IOException e) {

        }
    }

    private void backoff(int attempt) {
        try {
            long sleep = (long) Math.pow(2, attempt) * 50;
            Thread.sleep(sleep);
        } catch (InterruptedException ignored) {
        }
    }

    private boolean sendWithRetry(ChatTask chatTask, int maxRetries) {
        int attempt = 0;
        while (attempt < maxRetries) {
            try {
                if(sendConnection==null||!sendConnection.isOpen()){
                    if(sendConnection==null){
                        Statistics.sendConnection.incrementAndGet();
                    }else{
                        Statistics.sendReconnection.incrementAndGet();
                    }
                    connectToSendServer();
                }
                if(!receiveConnections.containsKey(chatTask.roomId)||!receiveConnections.get(chatTask.roomId).isOpen()){
                    if(!receiveConnections.containsKey(chatTask.roomId)){
                        Statistics.receiveConnection.incrementAndGet();
                    }else{
                        Statistics.receiveReconnection.incrementAndGet();
                    }
                    connectToReceiveServer(chatTask.roomId);
                }
                boolean succeed=sendConnection.sendMessage(chatTask.json);
                if(succeed){
                    return true;
                }else{
                    backoff(attempt);
                }
            } catch (Exception e) {
                attempt++;
                backoff(attempt);
            }
        }
        return false;
    }

    public void cleanup(){
        sendConnection.close();
        for(ClientReceiveEndPoint conn:receiveConnections.values()){
            conn.close();
        }
    }

    @Override
    public void run() {
        while(!MessagePool.messageQueue.isEmpty()){
            ChatTask chatTask=MessagePool.messageQueue.poll();
            long startTime=System.currentTimeMillis();
            boolean succeed=sendWithRetry(chatTask, 5);
            long endTime=System.currentTimeMillis();
            if(succeed){
                Statistics.succeedMessageRoundtripTime.add(endTime-startTime);
                Statistics.succeedMessage.incrementAndGet();
            }else{
                Statistics.failedMessage.incrementAndGet();
            }
        }
        // cleanup();
    }

}
