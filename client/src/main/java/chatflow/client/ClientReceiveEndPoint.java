package chatflow.client;

import java.io.IOException;

import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;

@ClientEndpoint
public class ClientReceiveEndPoint {
    private Session userSession;

    public void close(){
        try {
            this.userSession.close();
        } catch (IOException e) {
            
        }
    }

    public boolean isOpen(){
        return this.userSession.isOpen();
    }

    @OnOpen
    public void onOpen(Session session) {
        userSession=session;
    }

    @OnMessage
    public void onMessage(String message) {
        // System.out.println("Received from server: " + message);
        userSession.getAsyncRemote().sendText("ACK");
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        System.out.println("Receive connection closed: " + reason);
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        
    }
}