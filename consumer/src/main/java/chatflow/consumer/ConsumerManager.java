package chatflow.consumer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AMQP.Queue.DeclareOk;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import chatflow.utils.ChannelPool;

public class ConsumerManager implements Runnable {
  private final ChannelPool pool;
  private final ExecutorService workerPool;
  private final Broadcaster broadcaster;
  private final Map<String, Queue<MessageConsumer>> consumerAssigment = new HashMap<>();
  private final AtomicBoolean running = new AtomicBoolean(true);
  private final long monitorInterval;
  private final Set<String> queueSet = new HashSet<>();
  private final int prefetchCount;
  

  // private final int messengerPoolSize;

  public ConsumerManager(
      String MQHost,
      int consumerPoolSize,
      int messengerPoolSize,
      int workerPoolSize,
      long monitorInterval,
      int prefetchCount) {
    workerPool = Executors.newFixedThreadPool(workerPoolSize);
    pool = new ChannelPool(MQHost, consumerPoolSize);
    broadcaster = new Broadcaster(messengerPoolSize);
    this.monitorInterval = monitorInterval;
    this.prefetchCount = prefetchCount;
    // this.messengerPoolSize = messengerPoolSize;

    pool.init();
  }

  public class MessageConsumer extends DefaultConsumer {

    public MessageConsumer(Channel channel) {
      super(channel);
    }

    @Override
    public void handleCancel(String consumerTag) {
      try {
        pool.returnChannel(this.getChannel());
      } catch (InterruptedException e) {
      }
    }

    @Override
    public void handleCancelOk(String consumerTag) {
      try {
        pool.returnChannel(this.getChannel());
      } catch (InterruptedException e) {
      }
    }

    @Override
    public void handleDelivery(
        String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
      workerPool.submit(
          () -> {
            try {
              boolean succeed =
                  broadcaster.send(
                      new String(body, StandardCharsets.UTF_8), envelope.getRoutingKey());
              if (succeed) {
                this.getChannel().basicAck(envelope.getDeliveryTag(), false);
              } else {
                this.getChannel().basicNack(envelope.getDeliveryTag(), false, true);
              }
            } catch (IOException e) {
            }
          });
    }
  }

  private void subscribe(String routingKey) throws Exception {

    Channel channel = pool.borrowChannel();
    // System.out.println("Subscribing to " + routingKey);
    MessageConsumer consumer = new MessageConsumer(channel);
    channel.basicQos(prefetchCount);
    channel.exchangeDeclare("chat.exchange", "direct");
    channel.queueDeclare(routingKey, true, false, true, null);
    queueSet.add(routingKey);

    channel.queueBind(routingKey, "chat.exchange", routingKey);
    channel.basicConsume(routingKey, false, consumer);
    consumerAssigment.computeIfAbsent(routingKey, k -> new LinkedList<>()).add(consumer);
  }

  public void stop() {
    running.set(false);
  }

  @Override
  public void run() {
    for (int i = 1; i < 21; i++) {
      try {
        if (pool.isEmpty()) {
          pool.addXChannel(5);
        }
        subscribe("room." + i);
      } catch (Exception e) {
        System.err.println("Error subscribing to room." + i + ": " + e.getMessage());
      }
    }
    try {
      Channel monitorChannel = pool.borrowChannel();
      while (running.get()) {
        for (String queueName : queueSet) {
          DeclareOk queue = monitorChannel.queueDeclarePassive(queueName);
          // System.out.println(queue.getQueue() + " has " + queue.getMessageCount() + "messages.");
          if (queue.getMessageCount() > 1000) {
            // if (pool.isEmpty()) {
            //   pool.addXChannel(5);
            // }
            if(!pool.isEmpty()){
              System.out.println(queue.getQueue() + " has " + queue.getMessageCount() + "messages.");
              System.out.println("Adding one more consumer to " + queue.getQueue());
              subscribe(queue.getQueue());
            }
            
          } else if (queue.getMessageCount() < 500
              && consumerAssigment.get(queue.getQueue()).size() > 1) {
            MessageConsumer consumer = consumerAssigment.get(queue.getQueue()).poll();
            consumer.getChannel().basicCancel(consumer.getConsumerTag());
            System.out.println(queue.getQueue() + " has " + queue.getMessageCount() + "messages.");
            System.out.println("Removing one consumer from " + queue.getQueue());
          }
        }
        Thread.sleep(monitorInterval);
      }
    pool.returnChannel(monitorChannel);
    } catch (Exception e) {
      System.err.println("Error in ConsumerManager: " + e.getMessage());
    }
  }
}
