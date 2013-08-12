package solution;

import org.junit.Test;
import solution.config.ApplicationContext;
import solution.dao.QuotasDAO;
import tester.DutyGenerator;
import tester.ITestTask;

import javax.inject.Inject;
import java.util.Map;
import java.util.logging.Logger;

public class TaskImplPerformanceTest extends BasicTest{
    @Inject
    private Logger logger;
    @Inject
    private QuotasDAO quotasDAO;

    @Test
    public void testPerformance() throws InterruptedException {
        DutyGenerator dutyGenerator = new DutyGenerator();
        ITestTask impl = ApplicationContext.getInstance().getInstance(ITestTask.class);
        dutyGenerator.generateDuty(impl);
        Map<Integer,Long> result = quotasDAO.getAllQuotas();
        checkResults(result);
    }

    private void checkResults(Map<Integer, Long> result) {
        int correctQuotas = 0;
        int errorQuotas = 0;
        for(Map.Entry<Integer,Long> entry:result.entrySet()){
            if(entry.getValue()>=0) correctQuotas++;
                    else errorQuotas++;
        }
        logger.info(String.format("Stats: correct:%d; error:%d",correctQuotas,errorQuotas));
    }


}
