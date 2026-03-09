package chatflow.producer;

import chatflow.utils.ChannelPool;

public class ConnectionManager {
    public static final ChannelPool POOL = new ChannelPool("localhost", 10);
    static {
        POOL.init();
    }
}
