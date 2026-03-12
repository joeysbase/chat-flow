package chatflow.consumer;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.websocket.Session;

public class Broadcaster {
  private final ExecutorService messengerPool;
  private final ObjectMapper mapper = new ObjectMapper();

  private class Messenger implements Runnable {

    private final String message;
    private final Session session;
    private final String messageId;

    public Messenger(String message, Session session, String messageId) {
      this.message = message;
      this.session = session;
      this.messageId = messageId;
    }

    private void backoff(int attempt) {
      try {
        long sleep = (long) Math.pow(2, attempt) * 50;
        Thread.sleep(sleep);
      } catch (InterruptedException ignored) {
      }
    }

    private boolean sendWithRetry(String message, int maxRetries, Session session) {
      int attempt = 0;
      CountDownLatch latch = new CountDownLatch(1);
      while (attempt < maxRetries) {
        try {
          if (session.isOpen()) {
            AckManager.addSessionLatch(messageId, session, latch);
            // Thread.sleep(500);
            session.getAsyncRemote().sendText(message);
          } else {
            System.out.println("Connection not open");
            return true; // Consider closed connection as successful delivery for retry logic
          }
          boolean succeed= latch.await(10, TimeUnit.SECONDS);
          if(succeed){
            AckManager.removeSessionLatch(messageId, session);
            return true;
          }
          
          attempt++;
          backoff(attempt);
        } catch (InterruptedException e) {
          attempt++;
          backoff(attempt);
        }
      }
      return false;
    }

    @Override
    public void run() {
      boolean succeed = sendWithRetry(message, 5, session);
      if (succeed) {
        IdempotencyCache.add(messageId, session);
      }
    }
  }

  public Broadcaster(int messengerPoolSize) {
    messengerPool = Executors.newFixedThreadPool(messengerPoolSize);
  }

  public boolean send(String message, String routingKey) {
    String roomId = routingKey.substring(routingKey.lastIndexOf(".") + 1);
    Set<Session> sessions = RoomManager.getSessionsByRoomId(roomId);
    try {
      Map<String, Object> msgMap = mapper.readValue(message, HashMap.class);
      String messageId = (String) msgMap.get("messageId");
      if (sessions!=null&&!sessions.isEmpty()) {
        CountDownLatch latch = new CountDownLatch(sessions.size());
        AckManager.addLatch(messageId, latch);
        for (Session session : sessions) {
          if (!IdempotencyCache.isMessageSent(messageId, session)) {
            messengerPool.submit(new Messenger(message, session, messageId));
          } else {
            latch.countDown();
          }
        }
        boolean succeed = latch.await(1, TimeUnit.MINUTES);
        AckManager.removeLatch(messageId);
        return succeed;
      }
    } catch (JsonProcessingException | InterruptedException e) {

    }
    return false;
  }
}
