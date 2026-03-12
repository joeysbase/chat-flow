package chatflow.client;

public class ClientThread implements Runnable {

    private final ConnectionManager connectionManager;

    public ClientThread(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    private void backoff(int attempt) {
        try {
            long sleep = (long) Math.pow(2, attempt) * 50;
            Thread.sleep(sleep);
        } catch (InterruptedException ignored) {
        }
    }

    private boolean sendWithRetry(ChatTask chatTask, int maxRetries) {
        int attempt = 0;
        ClientSendEndPoint client = connectionManager.borrowSendConn();
        while (attempt < maxRetries) {
            try {
                while (client == null) {
                    client = connectionManager.borrowSendConn();
                }
                while (!client.isOpen()) {
                    connectionManager.addOneSendConn();
                    Statistics.sendReconnection.incrementAndGet();
                    client = connectionManager.borrowSendConn();
                }
                connectionManager.connectToReceiveIfClosed(chatTask.roomId);
                boolean succeed = client.sendMessage(chatTask.json);
                if (succeed) {
                    connectionManager.returnSendConn(client);
                    return true;
                } else {
                    backoff(attempt);
                }
            } catch (Exception e) {
                attempt++;
                backoff(attempt);
            }
        }
        connectionManager.returnSendConn(client);
        return false;
    }

    @Override
    public void run() {
        while (!MessagePool.messageQueue.isEmpty()) {
            ChatTask chatTask = MessagePool.messageQueue.poll();
            long startTime = System.currentTimeMillis();
            boolean succeed = sendWithRetry(chatTask, 5);
            long endTime = System.currentTimeMillis();
            if (succeed) {
                Statistics.succeedMessageRoundtripTime.add(endTime - startTime);
                Statistics.succeedMessage.incrementAndGet();
            } else {
                Statistics.failedMessage.incrementAndGet();
            }
        }
    }

}
