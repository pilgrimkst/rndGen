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

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static org.fest.assertions.api.Assertions.assertThat;

public class IntegrationTest extends BasicTest{
    public static final int CORRECTNESS_TRESHOLD = 5;
    public static final int WAIT_TO_SYNC = 5000;
    public static final int WAIT_BEFORE_SIG_TERM = 5000;

    @Inject
    private Logger logger;
    @Inject
    private QuotasDAO quotasDAO;

    @Before
    public void cleanUp() {
        clearStorage();
    }

    @After
    public void tearDown(){
        clearStorage();
    }

    @Test
    public void testPerformance() throws InterruptedException {
        Thread.sleep(WAIT_TO_SYNC*2);
        DutyGenerator dutyGenerator = new PerfomanceDutyGenerator();
        ITestTask impl = ApplicationContext.getInstance().getInstance(ITestTask.class);
        double[] statistics = dutyGenerator.generateDuty(Arrays.asList(impl));
//        Map<Integer, Long> result = quotasDAO.getAllQuotas();
//        checkResults(result);
        Thread.sleep(WAIT_TO_SYNC);
        logger.info(String.format("Performance is: requests{%f} requests per second: {%f}",statistics[0],statistics[1]));
    }

    @Test
    @Ignore
    public void testConcurrentCorrectness() throws Exception {
        ConcurrentHashMap<Integer, RequestsHolder> userStatistics = new ConcurrentHashMap<Integer, RequestsHolder>();
        CheckedDutyGenerator dutyGenerator = new CheckedDutyGenerator(userStatistics,-1);
        ITestTask impl1 = getNewInstance();
        ITestTask impl2 = getNewInstance();
        double[] statistics = dutyGenerator.generateDuty(Arrays.asList(impl1, impl2));
        Map<Integer, RequestsHolder> estimatedResult = dutyGenerator.getResults();
        Thread.sleep(WAIT_TO_SYNC);
        logger.info(String.format("Performance is: requests{%f} requests per second: {%f}", statistics[0],statistics[1]));
        Map<Integer, Long> result = quotasDAO.getAllQuotas();
        checkResults(result);
        assertResultsSame(result, estimatedResult);
        Thread.sleep(WAIT_TO_SYNC);
    }

    @Test
    @Ignore
    public void testDestroy() throws Exception {
        ConcurrentHashMap<Integer, RequestsHolder> userStatistics = new ConcurrentHashMap<Integer, RequestsHolder>();
        CheckedDutyGenerator dutyGenerator = new CheckedDutyGenerator(userStatistics, WAIT_BEFORE_SIG_TERM);
        ITestTask impl1 = getNewInstance();
        double[] statistics = dutyGenerator.generateDuty(Arrays.asList(impl1));
        Map<Integer, RequestsHolder> estimatedResult = dutyGenerator.getResults();
        logger.info(String.format("Performance is: requests{%f} requests per second: {%f}",statistics[0],statistics[1]));
        Thread.sleep(WAIT_TO_SYNC);
        Map<Integer, Long> result = quotasDAO.getAllQuotas();
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

}
