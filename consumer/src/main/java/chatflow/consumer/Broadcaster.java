package chatflow.consumer;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import jakarta.websocket.Session;

public class Broadcaster {
  private final ExecutorService messengerPool;

  private class Messenger implements Runnable {

    private final String message;
    private final Session session;
    private final CountDownLatch latch;

    public Messenger(String message, Session session, CountDownLatch latch) {
      this.message = message;
      this.session = session;
      this.latch = latch;
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
      while (attempt < maxRetries) {
        try {
          if (session.isOpen()) {
            // System.out.println("Sending message to session " + session.getId() + ": ");
            session.getBasicRemote().sendText(message);
            Thread.sleep(100);
          } else {
            System.out.println("Connection not open");
          }
          return true;
        } catch (IOException e) {
          attempt++;
          if (attempt >= maxRetries) {
            // Do nothing for now
          }
          backoff(attempt);
        } catch (InterruptedException e) {

        }
      }
      return false;
    }

    // public void send(){
    //   boolean succeed = sendWithRetry(message, 5, session);
    //   if (succeed) {
    //     IdempotencyCache.add(message, session);
    //   }
    // }

    @Override
    public void run() {
      boolean succeed = sendWithRetry(message, 5, session);
      if (succeed) {
        IdempotencyCache.add(message, session);
      }
      this.latch.countDown();
    }
  }

  public Broadcaster(int messengerPoolSize) {
    messengerPool = Executors.newFixedThreadPool(messengerPoolSize);
  }

  private boolean isAllSent(String message, Set<Session> sessionSet) {
    for (Session session : sessionSet) {
      if (!IdempotencyCache.isMessageSent(message, session)) {
        return false;
      }
    }
    return true;
  }

  public boolean send(String message, String routingKey) {
    String roomId = routingKey.substring(routingKey.lastIndexOf(".") + 1);
    // System.out.println("Broadcasting message to room: " + roomId);
    Set<Session> sessionSet = RoomManager.getSessionsByRoomId(roomId);
    if (!sessionSet.isEmpty()) {
      CountDownLatch latch = new CountDownLatch(sessionSet.size());
      for (Session session : sessionSet) {
        if (!IdempotencyCache.isMessageSent(message, session)) {
          // Messenger m=new Messenger(message, session, latch);
          // m.send();
          messengerPool.submit(new Messenger(message, session, latch));
        }
      }

      try {
        latch.await(1, TimeUnit.MINUTES);
      } catch (InterruptedException e) {
        // Handle interruption if necessary
      }
    }
    return isAllSent(message, sessionSet);
  }
}
