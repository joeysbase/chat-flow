package chatflow.client;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.websocket.ContainerProvider;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.WebSocketContainer;

public class ConnectionManager {
    public ClientSendEndPoint sendConnection = null;
    public final Map<String, ClientReceiveEndPoint> receiveConnections = new ConcurrentHashMap<>();
    private final String sendServerIp;
    private final String receiveServerIp;

    public ConnectionManager(String sendServerIp, String receiveServerIp) {
        this.sendServerIp = sendServerIp;
        this.receiveServerIp = receiveServerIp;
    }

    public ClientSendEndPoint getSendConnection() {
        return sendConnection;
    }

    public synchronized boolean connectToSendIfClosed() {
        if (sendConnection == null || !sendConnection.isOpen()) {
            if (sendConnection == null) {
                Statistics.sendConnection.incrementAndGet();
            } else {
                Statistics.sendReconnection.incrementAndGet();
            }
            connectToSendServer();
            return true;
        }
        return false;
    }

    public synchronized boolean connectToReceiveIfClosed(String roomId) {
        if (!receiveConnections.containsKey(roomId) || !receiveConnections.get(roomId).isOpen()) {
            if (!receiveConnections.containsKey(roomId)) {
                Statistics.receiveConnection.incrementAndGet();
            } else {
                Statistics.receiveReconnection.incrementAndGet();
            }
            connectToReceiveServer(roomId);
            return true;
        }
        return false;
    }

    private void connectToReceiveServer(String roomId) {
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            ClientReceiveEndPoint client = new ClientReceiveEndPoint();
            container.connectToServer(client, URI.create("ws://"+receiveServerIp+":8080/receive/room/" + roomId));
            receiveConnections.put(roomId, client);
        } catch (DeploymentException | IOException e) {

        }
    }

    private void connectToSendServer() {
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            ClientSendEndPoint client = new ClientSendEndPoint();
            container.connectToServer(client, URI.create("ws://" + sendServerIp + ":8080/send"));
            sendConnection = client;
        } catch (DeploymentException | IOException e) {
        }
    }

    public void cleanup() {
        sendConnection.close();
        for (ClientReceiveEndPoint conn : receiveConnections.values()) {
            conn.close();
        }
    }
}
