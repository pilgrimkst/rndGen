package solution.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class ConcurrentActor {

    protected static final Random R = new Random();

    public final long testTimeInMs;
    public final int threadsCount;

    private final AtomicLong requestsMade = new AtomicLong(0);
    private final List<Thread> runningThreads = new ArrayList<Thread>();
    private static final long WAIT_BEFORE_RETURN = 3000;

    public ConcurrentActor(int numberOfThreads, long testTimeInMs) {
        threadsCount = numberOfThreads;
        this.testTimeInMs = testTimeInMs;
    }

    public double[] generateDuty(final Runnable impl) throws InterruptedException {
        final long startTime = System.currentTimeMillis();
        System.out.println("Started at: " + startTime);
        for (int i = 0; i < threadsCount; i++) {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        if (System.currentTimeMillis() >= startTime + testTimeInMs) {
                            break;
                        }
                        try {
                            impl.run();
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
        Thread.sleep(WAIT_BEFORE_RETURN);
        return new double[]{requestsMade.get(), requestsMade.get() * 1.0 / testTimeInMs};
    }
}

