package tester;
public interface ITestTask {

  int getRandom(int userId) throws QuotaExceededException;
  void addQuota(int userId, long quota);
  long getQuota(int userId);

  void destroy();
  void clearAll();

}
