package chatflow.consumer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;

@ServerEndpoint("/receive/room/{roomId}")
public class ReceiveEndPoint {
  private final ObjectMapper mapper = new ObjectMapper();

  @OnOpen
  public void onOpen(Session session, @PathParam("roomId") String roomId) {
    // System.out.println("Session opened, roomId: " + roomId);
    RoomManager.addSessionToRoom(roomId, session);
    session.getUserProperties().put("roomId", roomId);
  }

  @OnMessage
  public void onMessage(String message, Session session) {
    try {
      Map<String, Object> msgJson = mapper.readValue(message, HashMap.class);
      String messageId = (String) msgJson.get("messageId");
      CountDownLatch latch = AckManager.getLatch(messageId);
      CountDownLatch sessionLatch = AckManager.getSessionLatch(messageId, session);
      sessionLatch.countDown();
      latch.countDown();
    } catch (JsonProcessingException e) {

    }
  }

  @OnClose
  public void onClose(Session session) {
    RoomManager.removeSessionFromRoom((String) session.getUserProperties().get("roomId"), session);
  }

  @OnError
  public void onError(Session session, Throwable throwable) {
    RoomManager.removeSessionFromRoom((String) session.getUserProperties().get("roomId"), session);
  }
}
