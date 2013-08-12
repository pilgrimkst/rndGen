package solution.dao;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class QuotasDAO extends BaseDAO {
    public static final String USER_QUOTAS_PATTERN = "user_quotas*";
    public static final String USER_QUOTAS = "user_quotas:%d";
    @Inject
    private Logger logger;
    @Inject
    protected QuotasDAO(@Named("db.redis.host") String redisHost, @Named("db.redis.port") int redisPort) {
        super(redisHost, redisPort);
    }

    public long incrQuota(Integer userId, long quota) {
        long newVal =connection.incrby(getUserQuotaKey(userId), quota);
        logger.fine(String.format("adding %d to user %d new quotaValue is %d", quota, userId, newVal));
        return newVal;
    }

    public Long getQuota(Integer userId) {
        String c = connection.get(getUserQuotaKey(userId));
        return getNullOrLong(c);
    }

    public void clearUserQuotas() {
        List<String> keys = connection.keys(USER_QUOTAS_PATTERN);
        for (String key : keys) {
            connection.del(key);
        }
        logger.info("Keys affected:"+keys.size());
    }

    public void remove(Integer userId) {
        connection.del(getUserQuotaKey(userId));
    }

    public Map<Integer, Long> getAllQuotas(){
        List<String> keys = connection.keys(USER_QUOTAS_PATTERN);
        Map<Integer,Long> quotas = new HashMap<Integer, Long>();
        for(String key:keys){
            Integer userId = getUserIdFromKey(key);
            Long quota = getQuota(userId);
             quotas.put(userId,quota);
        }
        return quotas;
    }

    private Integer getUserIdFromKey(String key) {
        String s = key.substring(key.indexOf(':')+1);
        return Integer.parseInt(s);
    }

    public void close() {
        client.shutdown();
        connection.close();
    }

    private Long getNullOrLong(String c) {
        return c == null ? null : Long.parseLong(c);
    }

    private String getUserQuotaKey(int userId) {
        return String.format(USER_QUOTAS, userId);
    }

    public void setQuota(Integer userId,long quota) {
        connection.set(getUserQuotaKey(userId),String.valueOf(quota));
    }
}
