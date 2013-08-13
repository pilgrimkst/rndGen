package solution;

import com.google.inject.Inject;
import solution.sync.SyncService;
import tester.ITestTask;
import tester.QuotaExceededException;

import java.util.Random;

public class TestTaskImpl implements ITestTask {
    public static final int MAX_RANDOM_NUMBER = 999;
    //Set counter value, that will be sent to backend once a second
    // change this variable(or pool of variables) whenever request goes
    // We need to sync state with backend (maybe before sending data to server) to avoid
    // unconsistent data if some other node will change data
    // need to implement fast and simple way to notify nodes for exceeding limits


    @Inject
    private final SyncService syncService = null;

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
