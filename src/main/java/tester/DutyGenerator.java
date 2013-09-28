package tester;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public abstract class DutyGenerator {

    protected static final Random R = new Random();

    public static final int TEST_TIME_IN_MS = 20000;
    public static final int THREADS_COUNT = 10;

    private final AtomicLong requestsMade = new AtomicLong(0);
    private final List<Thread> runningThreads = new ArrayList<Thread>();
    private final long waitBeforeSigTerm;

    protected DutyGenerator(long waitBeforeSigTerm) {
        this.waitBeforeSigTerm = waitBeforeSigTerm;
    }

    public double[] generateDuty(final List<ITestTask> impl) throws InterruptedException {
        final long startTime = System.currentTimeMillis();
        System.out.println("Started at: " + startTime);

        for (int i = 0; i < THREADS_COUNT; i++) {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        if (waitBeforeSigTerm > 0 && System.currentTimeMillis() >= startTime + waitBeforeSigTerm) {
                            for (ITestTask task : impl) {
                                task.destroy();
                            }
                        }

                        if (System.currentTimeMillis() >= startTime + TEST_TIME_IN_MS) {
                            break;
                        }
                        try {
                            doRandomQuery(getRandomNode(impl));
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
        return new double[]{requestsMade.get(), requestsMade.get() * 1.0 / TEST_TIME_IN_MS};
    }

    private ITestTask getRandomNode(List<ITestTask> impl) {
        if (impl == null || impl.size() < 1)
            throw new IllegalStateException();
        if (impl.size() == 1)
            return impl.get(0);
        return impl.get(R.nextInt(impl.size()));
    }

    protected abstract void doRandomQuery(ITestTask impl);

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
