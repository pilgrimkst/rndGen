package solution;

import tester.ITestTask;
import tester.QuotaExceededException;

public class TestTaskImpl implements ITestTask {

  @Override
  public int getRandom(final int userId) throws QuotaExceededException {
    return 0;
  }

  @Override
  public void addQuota(final int userId, final long quota) {
  }

  @Override
  public long getQuota(final int userId) {
    return 0;
  }

  @Override
  public void destroy() {
  }

  @Override
  public void clearAll() {
  }

}
