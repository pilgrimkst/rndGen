package solution.dao;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisConnection;

public class BaseDAO {
    protected final RedisClient client;
    protected final RedisConnection<String, String> connection;

    @Inject
    protected BaseDAO(@Named("db.redis.host")String redisHost, @Named("db.redis.port") int redisPort){
        client = new RedisClient(redisHost,redisPort);
        connection = client.connect();
    }


}
