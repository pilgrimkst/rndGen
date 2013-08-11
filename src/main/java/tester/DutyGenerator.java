package tester;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class DutyGenerator {

  private static final Random R = new Random();

  public static final int TEST_TIME_IN_MS = 20000;
  public static final int THREADS_COUNT = 100;

  private final AtomicLong requestsMade = new AtomicLong(0);
  private final List<Thread> runningThreads = new ArrayList<Thread>();

  public void generateDuty(final ITestTask impl) throws InterruptedException {
    final long startTime = System.currentTimeMillis();
    System.out.println("Started at: " + startTime);

    for (int i = 0; i < THREADS_COUNT; i++) {
      Thread t = new Thread(new Runnable() {
        @Override
        public void run() {
          while (true) {
            if (System.currentTimeMillis() >= startTime + TEST_TIME_IN_MS) {
              break;
            }

            try {
              doRandomQuery(impl);
            } catch (Throwable t) {
              System.out.println("Exception thrown from implementation!\n" + t);
              t.printStackTrace(System.err);
            }
            requestsMade.incrementAndGet();
          }
        }
      });
      t.start();
      runningThreads.add(t);
    }

    for (Thread t : runningThreads) {
      t.join();
    }

    System.out.println("Requests made: " + requestsMade + " (" + (requestsMade.get() * 1.0 / TEST_TIME_IN_MS) + " per second)");
  }

  private void doRandomQuery(ITestTask impl) {
    if (R.nextInt(10) == 0) {
      impl.addQuota(getRandomUserId(), 10);
    } else {
      try {
        impl.getRandom(getRandomUserId());
      } catch (QuotaExceededException ignored) {
      }
    }
  }

  public int getRandomUserId() {
    int r = R.nextInt(100);
    if (r < 80) {
      return 1;
    } else if (r < 99) {
      return R.nextInt(100) + 1;
    } else {
      return R.nextInt(1000000) + 1;
    }
  }
}
