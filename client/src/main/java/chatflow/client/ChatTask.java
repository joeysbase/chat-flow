package chatflow.client;

public class ChatTask {
    public final String roomId;
    public final String json;
    public final String messageType;

    public ChatTask(String r, String j, String mt) {
        roomId = r;
        json = j;
        messageType = mt;
    }
}