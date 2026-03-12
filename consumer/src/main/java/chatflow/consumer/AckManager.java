package chatflow.consumer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import jakarta.websocket.Session;

public class AckManager {
    private static final Map<String, CountDownLatch> ackLatches = new HashMap<>();
    private static final Map<Map<String, Session>, CountDownLatch> sessionLatches = new HashMap<>();

    public static void addLatch(String messageId, CountDownLatch latch) {
        ackLatches.put(messageId, latch);
    }

    public static CountDownLatch getLatch(String messageId) {
        return ackLatches.get(messageId);
    }

    public static void removeLatch(String messageId) {
        ackLatches.remove(messageId);
    }

    public static void addSessionLatch(String messageId, Session session, CountDownLatch latch) {
        Map<String, Session> key = new HashMap<>();
        key.put(messageId, session);
        sessionLatches.put(key, latch);
    }

    public static CountDownLatch getSessionLatch(String messageId, Session session) {
        Map<String, Session> key = new HashMap<>();
        key.put(messageId, session);
        return sessionLatches.get(key);
    }

    public static void removeSessionLatch(String messageId, Session session) {
        Map<String, Session> key = new HashMap<>();
        key.put(messageId, session);
        sessionLatches.remove(key);
    }
}
