package solution.dao;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
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
        //        long newVal = connection.incrby(getUserQuotaKey(userId), quota);
        //        logger.info(String.format("adding %d to user %d new quotaValue is %d", quota, userId, newVal));
        return Long.parseLong(connection.get(getUserQuotaKey(userId)));
    }

    public Future<Long> incrQuotaAsync(Integer userId, long quota) {
        return connectionAsync.incrby(getUserQuotaKey(userId), quota);
    }

    public Long getQuota(Integer userId) {
        String c = connection.get(getUserQuotaKey(userId));
        return getNullOrLong(c);
    }

    public void clearUserQuotas() {
        try {
            connection.flushall();
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public void remove(Integer userId) {
        connection.del(getUserQuotaKey(userId));
    }

    public Map<Integer, Long> getAllQuotas() {
        List<String> keys = connection.keys(USER_QUOTAS_PATTERN);
        Map<Integer, Long> quotas = new HashMap<Integer, Long>();
        for (String key : keys) {
            Integer userId = getUserIdFromKey(key);
            Long quota = getQuota(userId);
            quotas.put(userId, quota);
        }
        return quotas;
    }

    private Integer getUserIdFromKey(String key) {
        String s = key.substring(key.indexOf(':') + 1);
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

    public void setQuota(Integer userId, long quota) {
        connection.set(getUserQuotaKey(userId), String.valueOf(quota));
    }

    public List<Long> get(List<Integer> ids) {
        String[] chunk = new String[ids.size()];
        int index = 0;
        for (Integer id : ids) {
            chunk[index] = getUserQuotaKey(id);
        }
        List<String> result = connection.mget(chunk);
        return mapStringToLong(result);
    }

    public void deferredSave(Map<Integer,Long> changesFromNode){
        Map<String,String> changes = toMapOfString(changesFromNode);
        connectionAsync.mset(changes);
    }

    public void deferredSync(List<Integer> ids, String nodeId){

    }

    private Map<String, String> toMapOfString(Map<Integer, Long> changesFromNode) {
        Map<String,String> mapped = new HashMap<String, String>(changesFromNode.size());
        for(Integer key:changesFromNode.keySet()){
            mapped.put(String.valueOf(key),String.valueOf(changesFromNode.get(key)));
        }
        return mapped;
    }

    private List<Long> mapStringToLong(List<String> valuesFromDatabase) {
        List<Long> result = new ArrayList<Long>(valuesFromDatabase.size());
        for (String s : valuesFromDatabase) {
            result.add(Long.parseLong(s));
        }
        return result;
    }

    public interface CallbackLong {
        void apply(long l);
    }
}
