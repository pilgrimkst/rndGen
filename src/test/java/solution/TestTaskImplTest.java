package solution;


import com.google.inject.Guice;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import solution.config.ApplicationContext;
import solution.config.ApplicationModule;
import solution.dao.QuotasDAO;
import solution.sync.RequestsHolder;
import tester.CheckedDutyGenerator;
import tester.DutyGenerator;
import tester.ITestTask;
import tester.PerfomanceDutyGenerator;
import tester.QuotaExceededException;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import static org.fest.assertions.api.Assertions.assertThat;

public class TestTaskImplTest extends BasicTest{
    public static final int CORRECTNESS_TRESHOLD = 5;
    public static final int WAIT_TO_SYNC = 1000;
    @Inject
    private Logger logger;
    @Inject
    private QuotasDAO quotasDAO;

    @Inject private ITestTask iTestTask;

    private final List<Integer> createdUserQuotas = new CopyOnWriteArrayList<Integer>();

    @Before
    public void cleanUp() {
        clearStorage();
    }

    @After
    public void tearDown(){
        clearStorage();
    }
    @Test
    public void getRandomShouldReturnNonNegativeNumberForUserWithQuota() throws Exception {
        int userId = 1;
        long quota = 10;
        createTestUserWithQuota(userId, quota);
        int numOfRequests = 10;
        while(numOfRequests-->0){
            assertValidRandomNumber(userId);
            assertQuotaEquals(userId,numOfRequests);
        }
        assertQuotaEquals(userId, 0l);
    }

    @Test
    public void getRandomShouldCreateNewClientForNonExistentUserId() throws Exception {
        int nonExistentUser = 0;
        assertValidRandomNumber(nonExistentUser);
    }

    @Test
    public void getRandomShouldThrowExceptionForUserWithZeroQuota() throws Exception{
        int userId = 2;
        createTestUserWithQuota(userId,1);
        assertValidRandomNumber(userId);
        assertQuotaEquals(userId,0);
        assertExceptionOnGetRandomQuery(userId);
    }

    @Test
    public void testAddQuota() throws Exception {
        int userId = 3;
        createTestUserWithQuota(userId,0);
        assertExceptionOnGetRandomQuery(userId);
        assertQuotaExceededOnAddQuota(userId, 2);
        assertValidRandomNumber(userId);
    }

    @Test
    public void testGetQuota() throws Exception {
       int existingUserId = 4;
       long existingUserQuota = 100l;
       createTestUserWithQuota(existingUserId,existingUserQuota);
       assertThat(iTestTask.getQuota(existingUserId)).isEqualTo(existingUserQuota);
    }
    @Ignore
    @Test
    public void testDestroy() throws Exception {

    }

    @Test
    public void testClearAll() throws Exception {
        int defaultQuotaForNewUser = 10;
        int numOfUsers = 100;
        int userIdFrom = 1000;
        int userN = numOfUsers;
        List<Integer> userIds = new ArrayList<Integer>(numOfUsers);
        Random r = new Random();
        while(userN-->0){
            int userId = userIdFrom+userN;
            long quota = r.nextInt(1000);
            userIds.add(userId);
            createTestUserWithQuota(userId,quota);
        }
        iTestTask.clearAll();
        Thread.sleep(WAIT_TO_SYNC);
        for(Integer userId:userIds){
            assertThat(iTestTask.getQuota(userId)).isEqualTo(defaultQuotaForNewUser);
        }
    }

    @Test
    public void testPerformance() throws InterruptedException {
        DutyGenerator dutyGenerator = new PerfomanceDutyGenerator();
        ITestTask impl = ApplicationContext.getInstance().getInstance(ITestTask.class);
        double[] statistics = dutyGenerator.generateDuty(Arrays.asList(impl));
        logger.info(String.format("Performance is: requests{%f} requests per second: {%f}",statistics[0],statistics[1]));
        Map<Integer, Long> result = quotasDAO.getAllQuotas();
        checkResults(result);
    }

    @Test
    public void testConcurrentCorrectness() throws Exception {
        ConcurrentHashMap<Integer, RequestsHolder> userStatistics = new ConcurrentHashMap<Integer, RequestsHolder>();
        CheckedDutyGenerator dutyGenerator = new CheckedDutyGenerator(userStatistics);
        ITestTask impl1 = getNewInstance();
        ITestTask impl2 = getNewInstance();
        double[] statistics = dutyGenerator.generateDuty(Arrays.asList(impl1, impl2));
        Map<Integer, RequestsHolder> estimatedResult = dutyGenerator.getResults();
        Thread.sleep(WAIT_TO_SYNC);
        logger.info(String.format("Performance is: requests{%f} requests per second: {%f}",statistics[0],statistics[1]));
        Map<Integer, Long> result = quotasDAO.getAllQuotas();
        checkResults(result);
        assertResultsSame(result, estimatedResult);
    }

    private ITestTask getNewInstance() {
        return Guice.createInjector(new ApplicationModule()).getInstance(ITestTask.class);
    }

    private void assertResultsSame(Map<Integer, Long> result, Map<Integer, RequestsHolder> estimatedResult) {
        assertThat(result.size()).isEqualTo(estimatedResult.size());
        logger.info(String.format("Size of storage {%d} estimated size is: {%d}",result.keySet().size(),estimatedResult.keySet().size()));
        long difference = 0;
        for (Integer userId : estimatedResult.keySet()) {
            Long quotaFromDatabase = result.get(userId);
            RequestsHolder estimatedTestResult = estimatedResult.get(userId);
            assertThat(quotaFromDatabase).isNotNull();
            assertThat(estimatedTestResult).isNotNull();
            difference +=estimatedTestResult.getQuota().get() - quotaFromDatabase;
            assertThat(estimatedTestResult.getQuota().get() - quotaFromDatabase).isLessThan(CORRECTNESS_TRESHOLD);
        }
        logger.info(String.format("Total difference amount: {%d}",difference));
    }

    private void checkResults(Map<Integer, Long> result) {
        int correctQuotas = 0;
        int errorQuotas = 0;
        for (Map.Entry<Integer, Long> entry : result.entrySet()) {
            if (entry.getValue() >= 0) correctQuotas++;
            else errorQuotas++;
        }
        logger.info(String.format("Stats: correct:%d; error:%d", correctQuotas, errorQuotas));
    }

    private void assertExceptionOnGetRandomQuery(int userId) {
        try{
            iTestTask.getRandom(userId);
            assertThat(false);
        } catch (Exception e) {
            assertThat(e).isInstanceOf(QuotaExceededException.class);
        }
    }

    private void createTestUserWithQuota(int userId, long quota) {
        assertThat(createdUserQuotas).doesNotContain(userId);
        assertThat(quotasDAO.getQuota(userId)).isNull();
        quotasDAO.incrQuota(userId, quota);
        registerNewUser(userId);
    }

    private boolean registerNewUser(int userId) {
        return createdUserQuotas.add(userId);
    }

    private void assertQuotaExceededOnAddQuota(int userId, int incrementBy) {
        long currentQuota = iTestTask.getQuota(userId);
        iTestTask.addQuota(userId,incrementBy);
        assertThat(iTestTask.getQuota(userId)).isEqualTo(currentQuota+incrementBy);
    }

    private void assertQuotaEquals(int userId, long quota) {
        assertThat(iTestTask.getQuota(userId)).isEqualTo(quota);
    }

    private void assertValidRandomNumber(int userId) throws QuotaExceededException {
        try{
            assertThat(iTestTask.getRandom(userId)).isGreaterThanOrEqualTo(0);
        }catch (Exception e){
            assertThat(false);
        }
    }
}
