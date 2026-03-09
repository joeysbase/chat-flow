package chatflow.client;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;

@ClientEndpoint
public class ClientSendEndPoint {
    private Session userSession = null;
    private CountDownLatch latch;

    public boolean sendMessage(String message) {
        try {
            this.latch=new CountDownLatch(1);
            userSession.getBasicRemote().sendText(message);
            boolean succeed=this.latch.await(2, TimeUnit.SECONDS);
            return succeed;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    public boolean isOpen(){
        return this.userSession!=null && this.userSession.isOpen();
    }

    public void close(){
        try {
            this.userSession.close();
        } catch (IOException e) {
            
        }
    }

    @OnOpen
    public void onOpen(Session session) {
        this.userSession = session;
    }

    @OnMessage
    public void onMessage(String message) {
        if(message.equals("ACK")){
            this.latch.countDown();
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        System.out.println("Send connection closed: " + reason);
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        
    }
}
