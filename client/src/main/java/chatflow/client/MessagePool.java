package chatflow.client;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MessagePool {
    public static final Queue<ChatTask> messageQueue=new LinkedBlockingQueue<>();
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Random rand = new Random();
    private static final String[] MESSAGE_POOL = new String[50];
    public static int totalMessagesGenerated=0;
    static {
        for (int i=0;i<50;i++) MESSAGE_POOL[i] = "msg_" + i;
    }

    public static void generateMessage(int num){
        try {
            totalMessagesGenerated=num;
            for (int i=0;i<num;i++) {

                int userId = rand.nextInt(100000) + 1;
                String roomId = String.valueOf(rand.nextInt(20)+1);

                Map<String,Object> m = new HashMap<>();
                m.put("userId", String.valueOf(userId));
                m.put("username", "user" + userId);
                m.put("message", MESSAGE_POOL[rand.nextInt(50)]);
                m.put("timestamp", Instant.now().toString());
                m.put("roomId", roomId);

                int t = rand.nextInt(100);
                if (t <= 90) m.put("messageType","TEXT");
                else if (t>90&&t<=95) m.put("messageType","JOIN");
                else m.put("messageType","LEAVE");

                String json = mapper.writeValueAsString(m);
                

                messageQueue.add(new ChatTask(roomId, json, m.get("messageType").toString()));
            }
        } catch (JsonProcessingException e) {
        }
        
    }

}
