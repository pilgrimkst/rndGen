package solution.dao;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.lambdaworks.redis.RedisAsyncConnection;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisConnection;
import com.lambdaworks.redis.pubsub.RedisPubSubConnection;

public class BaseDAO {
    protected final RedisClient client;
    protected final RedisConnection<String, String> connection;
    protected final RedisAsyncConnection<String, String> connectionAsync;
    protected final RedisPubSubConnection<String, String> pubSubConnection;

    @Inject
    protected BaseDAO(@Named("db.redis.host") String redisHost, @Named("db.redis.port") int redisPort) {
//        client = new RedisClient(redisHost, redisPort);
//        connection = client.connect();
//        connectionAsync = client.connectAsync();
//        pubSubConnection = client.connectPubSub();
//        RedisPubSubListener<String, String> pubSubListener = new LoggerListener();
//        pubSubConnection.addListener(pubSubListener);
        client = null;
        connection = null;
        connectionAsync = null;
        pubSubConnection = null;
    }

    public RedisConnection<String, String> getConnection() {
        return connection;
    }

    public void subscribe(String channel) {
        pubSubConnection.psubscribe(channel);
    }


    public void publish(String channel, String message) {
        connectionAsync.publish(channel, message);
    }

}
