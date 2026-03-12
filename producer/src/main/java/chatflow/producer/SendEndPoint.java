package chatflow.producer;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rabbitmq.client.Channel;

import chatflow.utils.ChatMessage;
import chatflow.utils.IpAwareConfigurator;
import chatflow.utils.MessageValidator;
import chatflow.utils.QueueMessage;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

@ServerEndpoint(value = "/send", configurator = IpAwareConfigurator.class)
public class SendEndPoint {

  public static Set<Session> sessionSet = ConcurrentHashMap.newKeySet();
  public static Map<Session, String> sessionToIp = new ConcurrentHashMap<>();
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private String serverId="1";
  private static final String EXCHANGE_NAME = "chat.exchange";

  public SendEndPoint(String id) {
    serverId = id;
  }

  public SendEndPoint() {
}

  private void sendError(String msg, Session session) {
    ObjectNode node = MAPPER.createObjectNode();
    node.put("status", "failed");
    node.put("msg", msg);
    node.put("timestamp", Instant.now().toString());
    session.getAsyncRemote().sendText(node.toString());
  }

  private QueueMessage buildQueueMessage(ChatMessage chatMsg, Session session) {
    QueueMessage queueMsg = new QueueMessage();
    queueMsg.messageId = UUID.randomUUID().toString();
    queueMsg.roomId = chatMsg.roomId;
    queueMsg.message = chatMsg.message;
    queueMsg.messageType = chatMsg.messageType;
    queueMsg.userId = chatMsg.userId;
    queueMsg.username = chatMsg.username;
    queueMsg.timestamp = chatMsg.timestamp;
    queueMsg.clientIp = "null";
    queueMsg.serverId = this.serverId;
    return queueMsg;
  }

  private void sendAck(Session session){
    session.getAsyncRemote().sendText("ACK");
  }

  private void backoff(int attempt) {
    try {
      long sleep = (long) Math.pow(2, attempt) * 50;
      Thread.sleep(sleep);
    } catch (InterruptedException ignored) {
    }
  }

  private void publishWithRetry(
      Channel channel, String routingKey, byte[] messageBody, int maxRetries, Session session) {
    int attempt = 0;
    while (attempt < maxRetries) {
      try {
        channel.basicPublish(EXCHANGE_NAME, routingKey, null, messageBody);
        channel.waitForConfirmsOrDie(5000);
        sendAck(session);
        return; 
      } catch (IOException | InterruptedException | TimeoutException e) {
        attempt++;
        if (attempt >= maxRetries) {
          sendError("Failed to publish message after " + maxRetries + " attempts", session);
          return;
        }
        backoff(attempt);
      }
    }
  }

  @OnOpen
  public void onOpen(Session session, EndpointConfig config) {
    sessionSet.add(session);
    // sessionToIp.put(session, (String) config.getUserProperties().get("client-ip"));
    System.out.println("log: new user connected.");
  }

  @OnMessage
  public void onMessage(String message, Session session) {
    // System.out.println("Received: " + message);
    try {
      // Validate message
      ChatMessage chatMsg = MAPPER.readValue(message, ChatMessage.class);
      String errorMsg = MessageValidator.validate(chatMsg);
      if (errorMsg != null) {
        sendError(errorMsg, session);
        return;
      }

      // Publish to queue
      Channel channel = ConnectionManager.POOL.borrowChannel();
      QueueMessage queueMsg = buildQueueMessage(chatMsg, session);
      channel.queueDeclare("room." + queueMsg.roomId, true, false, true, null);
      channel.exchangeDeclare("chat.exchange", "direct");
      channel.queueBind("room." + queueMsg.roomId, EXCHANGE_NAME, "room." + queueMsg.roomId);
      publishWithRetry(channel, "room." + queueMsg.roomId, MAPPER.writeValueAsBytes(queueMsg), 5, session);
      ConnectionManager.POOL.returnChannel(channel);
      System.out.println("log: message published to room" + queueMsg.roomId +"successfully.");
    } catch (InterruptedException | IOException e) {
      sendError(e.toString(), session);
    }
  }

  @OnClose
  public void onClose(Session session) {
    System.out.println("Connection closed: " + sessionToIp.get(session));
    sessionSet.remove(session);
    sessionToIp.remove(session);
    
  }

  @OnError
  public void onError(Session session, Throwable throwable) {
    System.err.println(throwable.getMessage());
  }
}
