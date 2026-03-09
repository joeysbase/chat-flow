package chatflow.producer;

import chatflow.utils.ChannelPool;

public class ConnectionManager {
    public static String MQ_HOST = "";
    public static ChannelPool POOL;
    public static void init(String host) {
        MQ_HOST = host;
        POOL=new ChannelPool(MQ_HOST, 10);
        POOL.init();
    }
}

