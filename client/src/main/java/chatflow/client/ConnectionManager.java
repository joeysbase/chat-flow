package chatflow.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionManager {
    public static ClientSendEndPoint sendConnection = null;
    public static final Map<String, ClientReceiveEndPoint> receiveConnections = new ConcurrentHashMap<>();

    public static void cleanup(){
        sendConnection.close();
        for(ClientReceiveEndPoint conn:receiveConnections.values()){
            conn.close();
        }
    }
}
