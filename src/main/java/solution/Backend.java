package solution;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisConnection;

public class Backend {
    public static final String USER_QUOTAS = "user_quotas:%d";
    private final RedisClient client;
    private final RedisConnection<String, String> connection;

    @Inject
    public Backend(@Named("redis_host") String redisHost, @Named("redis_port") int redisPort) {
        client = new RedisClient(redisHost, redisPort);
        connection = client.connect();
    }

    public Long getQuota(int userId) {
        String c = connection.get(getUserQuotaKey(userId));
        return c != null ? Long.parseLong(c) : null;
    }

    public void setQuota(int userId, long amount) {
        connection.set(String.valueOf(userId), String.valueOf(amount));
    }

    public long incrQuota(int userId, long quota) {
        return connection.incrby(getUserQuotaKey(userId), quota);
    }

    public void remove(int userId){
        connection.del(getUserQuotaKey(userId));
    }

    private String getUserQuotaKey(int userId) {
        return String.format(USER_QUOTAS, userId);
    }

    public void close() {
        connection.close();
    }
}
