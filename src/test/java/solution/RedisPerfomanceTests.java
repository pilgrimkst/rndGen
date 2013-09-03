package solution;

import com.lambdaworks.redis.RedisConnection;
import com.lambdaworks.redis.ScriptOutputType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import solution.dao.BaseDAO;
import solution.utils.ConcurrentActor;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Logger;

import static org.fest.assertions.api.Assertions.assertThat;

public class RedisPerfomanceTests extends BasicTest {

    @Inject
    private BaseDAO baseDAO = null;

    @Inject
    private Logger logger = null;

    final String keyBase = "test:%d";
    final Random r = new Random();
    public static final int NUMBER_OF_ITERATIONS = 100000;

    @Before
    @After
    public void cleanUp() throws Exception {
        getConnection().flushall();

    }

    private RedisConnection<String, String> getConnection() {
        return baseDAO.getConnection();
    }

    @Test
    public void testSingleSet() {
        measure(new Runnable() {
            @Override
            public void run() {
                getConnection().set(getRandomKey(), String.valueOf(r.nextInt()));
            }
        }, NUMBER_OF_ITERATIONS, "set");
    }

    @Test
    public void testBulkSet() {
        final int chunkSize = 1000;
        measure(new Runnable() {
            @Override
            public void run() {
                Map<String, String> pkg = new HashMap<String, String>();
                for (int i = 0; i < chunkSize; i++) {
                    pkg.put(getRandomKey(), String.valueOf(r.nextInt()));
                }
                getConnection().mset(pkg);
            }
        }, NUMBER_OF_ITERATIONS / chunkSize, "mset");
    }

    @Test
    public void testIncrPerfomance() {
        measure(new Runnable() {
            @Override
            public void run() {
                getConnection().incrby(getRandomKey(), r.nextInt(1000));
            }
        }, NUMBER_OF_ITERATIONS, "IncrBy");

    }

    @Test
    public void testPubSubThrouput() throws InterruptedException {
        final String channel = "test_channel.*";
        baseDAO.subscribe(channel);
        Runnable f = new Runnable() {
            @Override
            public void run() {
                baseDAO.publish("test_channel." + getRandomKey(), UUID.randomUUID().toString());
            }
        };
        ConcurrentActor actor = new ConcurrentActor(100, 20000);
        double[] results = actor.generateDuty(f);
        logger.info(String.format("Requests made %f rps %f", results[0], results[1]));
    }

    @Test
    public void testEval() throws InterruptedException {

        /* syncAndReturn list of actual quota values
        *   1. query all unmerged lists of quota updates
        *   2. For each quotaList repeat incrBy(quotaList.pop()) until the list is empty
        *   2.1 if result after all increments <= threshold push userId to list of force_sync
        *   2.2 after finish send list of userIds for forced sync (local changes invalidated and quota is set to zero)
        *  3. remove key pointing to empty list, that points to unmerged data
        * */

        String changeKeyPattern = "change_1:";
        String userPattern = "quota:";
        Random r = new Random();
        int numOfElements = 1000;
        Map<String, String> quotaValues = new HashMap<String, String>();
        Map<String, String> quotaUpdates = new HashMap<String, String>();
        for (int i = 0; i < numOfElements; i++) {
            quotaValues.put(getQuotaKey(i), String.valueOf(r.nextInt(100)));
            quotaUpdates.put(getUpdateKey(i, 1), String.valueOf(r.nextInt(100)));
        }
        getConnection().mset(quotaValues);
        long start = System.currentTimeMillis();
        getConnection().mset(quotaUpdates);

        logger.info("fill time: " + (System.currentTimeMillis() - start));
        Thread.sleep(2000);

        start = System.currentTimeMillis();
        getConnection().mset(quotaUpdates);
        List<String> o = getScriptForKeysUpdate(changeKeyPattern, userPattern);

        logger.info("bulc incr execution time: " + (System.currentTimeMillis() - start));
        checkResults(o, quotaValues, quotaUpdates);
        Thread.sleep(2000);


        //List test

        String[] quotaValuesChangesList = new String[numOfElements];
        String[] quotaUpdatesChangesList = new String[numOfElements];
        String[] quotaUpdatesIdList = new String[numOfElements];
        quotaValues = new HashMap<String, String>();
        for (int i = numOfElements; i < numOfElements*2; i++) {
            int index = i-numOfElements;
            String quota = String.valueOf(r.nextInt(100));
            quotaValues.put(getQuotaKey(index), quota );
            quotaValuesChangesList[index] = quota ;
            quotaUpdatesIdList[index] = String.valueOf(index);
            quotaUpdatesChangesList[index] = String.valueOf(r.nextInt(100));
        }
        getConnection().mset(quotaValues);
        start = System.currentTimeMillis();
        String idsKey = "update_1_ids";
        String updatesKey = "update_1_values";
        getConnection().lpush(idsKey, quotaUpdatesIdList);
        getConnection().lpush(updatesKey, quotaUpdatesChangesList);
        List<Long>other  = getScriptForListUpdate(idsKey, updatesKey, userPattern);
        checkResultsList(quotaValuesChangesList,quotaUpdatesChangesList, other);
        logger.info("list incr execution time: " + (System.currentTimeMillis() - start));
        Thread.sleep(2000);


        start = System.currentTimeMillis();
        for (int i = 0; i < numOfElements; i++) {
            getConnection().incrby(getRandomKey(), r.nextInt(100));
        }
        logger.info("sec incr execution time: " + (System.currentTimeMillis() - start));

    }

    private void checkResultsList(String[] quotaValuesChangesList, String[] quotaUpdatesChangesList, List<Long> o) {
        for(int i=0;i<quotaValuesChangesList.length;i++){
            assertThat(o.get(i)).isNotNull();
            assertThat(Long.valueOf(o.get(i))).isEqualTo(Long.valueOf(quotaValuesChangesList[i])+Long.valueOf(quotaUpdatesChangesList[i]));
        }
    }

    private List<String> getScriptForKeysUpdate(String changeKeyPattern, String userPattern) {
        String script = String.format("" +
                " local changesKeys = redis.call('KEYS', ARGV[1]..'*')  " +
                " local index = #changesKeys " +
                " local patternKeyLength = #(ARGV[1])" +
                " local res = {}" +
                " while index>0 do " +
                "   local userId = tonumber(string.sub(changesKeys[index], patternKeyLength+1))" +
                "   local entry={}" +
                "   entry[0] = userId" +
                "   entry[1] = redis.call('incrby', ARGV[2]..userId, tonumber(redis.call('get', changesKeys[index])))" +
                "   res[index] = entry[0]..':'..entry[1]" +
                "   redis.call('DEL', changesKeys[index])" +
                " index = index-1" +
                " end" +
                " return res");

        List<String> o = getConnection().eval(script, ScriptOutputType.MULTI, new String[]{}, changeKeyPattern, userPattern);
        return o;
    }

    private List<Long> getScriptForListUpdate(String idsKey, String updatesKey, String userPattern) {
        //ARGV_1 ids key ARGV_2 updates key ARGV_3 user quotas pattern
        String script =
                " local index = redis.call('LLEN', ARGV[1]) " +
                " local res = {}" +
                " while index>0 do " +
                "   local userId = tonumber(redis.call('lpop', ARGV[1]))" +
                "   local updateValue = tonumber(redis.call('lpop', ARGV[2]))" +
                "   res[index] = redis.call('incrby', ARGV[3]..userId, updateValue)" +
                "   index = index-1" +
                " end" +
                " return res";
        List<Long> o = getConnection().eval(script, ScriptOutputType.MULTI, new String[]{}, idsKey, updatesKey, userPattern);
        return o;
    }

    private void checkResults(List<String> result, Map<String, String> quotaValues, Map<String, String> quotaUpdates) {
        for (String item : result) {
            String[] splitted = item.split(":");
            Integer userId = Integer.parseInt(splitted[0]);
            Long newVal = Long.parseLong(splitted[1]);
            assertThat(userId).isNotNull();
            String quotaKey = getQuotaKey(userId);
            String updateKey = getUpdateKey(userId, 1);
            assertThat(quotaUpdates.containsKey(updateKey) && quotaValues.containsKey(quotaKey));
            assertThat(newVal).isEqualTo(Long.parseLong(quotaValues.get(quotaKey)) + Long.parseLong(quotaUpdates.get(updateKey)));
        }
    }

    private String getQuotaKey(int i) {
        return String.format("quota:%d", i);
    }

    private String getUpdateKey(int userid, int nodeId) {
        return String.format("change_1:%d", userid);
    }

    private String getRandomKey() {
        return String.format(keyBase, r.nextInt());
    }

    private void measure(Runnable func, int iterations, String message) {
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            func.run();
        }
        logger.info(String.format("%s execution time is: %s sec", message, (System.currentTimeMillis() - startTime) / 1000));
    }
}


