package solution;

import com.google.inject.Inject;
import solution.sync.SyncServiceNode;
import tester.ITestTask;
import tester.QuotaExceededException;

import java.util.Random;

public final class TestTaskImpl implements ITestTask {
    public static final int MAX_RANDOM_NUMBER = 999;

    @Inject
    private final SyncServiceNode syncService = null;

    private final Random generator = new Random();

    @Override
    public int getRandom(final int userId) throws QuotaExceededException {
        if (getQuota(userId) > 0) {
            addQuota(userId, -1);
            return generator.nextInt(MAX_RANDOM_NUMBER);
        }
        throw new QuotaExceededException();
    }

    @Override
    public void addQuota(final int userId, final long quota) {
        syncService.addQuota(userId, quota);
    }

    @Override
    public long getQuota(final int userId) {
        return syncService.getQuota(userId);
    }

    @Override
    public void destroy() {
        syncService.persistLocalChanges();
    }

    @Override
    public void clearAll() {
        syncService.cleanUpUserQuotas();
    }   
}
