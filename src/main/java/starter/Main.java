package starter;

import solution.config.ApplicationContext;
import tester.DutyGenerator;
import tester.ITestTask;

public class Main {

  public static void main(String[] args) throws InterruptedException {
    DutyGenerator dutyGenerator = new DutyGenerator();

    ITestTask impl = ApplicationContext.getInstance().getInstance(ITestTask.class);
    dutyGenerator.generateDuty(impl);
  }

}
