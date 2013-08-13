package starter;

import solution.config.ApplicationContext;
import tester.DutyGenerator;
import tester.ITestTask;
import tester.PerfomanceDutyGenerator;

import java.util.Arrays;

public class Main {

  public static void main(String[] args) throws InterruptedException {
    DutyGenerator dutyGenerator = new PerfomanceDutyGenerator();

    ITestTask impl = ApplicationContext.getInstance().getInstance(ITestTask.class);
    dutyGenerator.generateDuty(Arrays.asList(impl));
  }

}
