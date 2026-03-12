package chatflow.client;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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
    private final ObjectMapper mapper=new ObjectMapper();

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
        Map<String,Object> response=new HashMap<>();
        try {
            Map<String,Object> msgJson=mapper.readValue(message, HashMap.class);
            response.put("messageId", msgJson.get("messageId"));
            // response.put("status","ACK");
            userSession.getBasicRemote().sendText(mapper.writeValueAsString(response));
            Statistics.ackedMessage.incrementAndGet();
        } catch (JsonProcessingException e) {
            
        } catch (IOException e) {
		}
        
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        System.out.println("Receive connection closed: " + reason);
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        
    }
}