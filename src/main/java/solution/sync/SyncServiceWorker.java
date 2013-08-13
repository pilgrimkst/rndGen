package solution.sync;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import solution.dao.QuotasDAO;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

public class SyncServiceWorker {
    private static Logger logger = Logger.getLogger(SyncServiceWorker.class.getName());
    private static final int NUM_OF_PERSIST_WORKERS = 1;
    private volatile boolean serviceStarted = false;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private BlockingQueue<Quota> stats;

    @Inject
    @Named("nodeId")
    private final String nodeId = null;

    @Inject
    private final ExecutorService executorService = null;

    @Inject
    private final QuotasDAO quotasDAO = null;

    public void setStats(BlockingQueue<Quota> stats) {
        this.stats = stats;
    }

    public void startService() {
        serviceStarted = true;
        for (int i = 0; i < NUM_OF_PERSIST_WORKERS; i++) {
            executorService.submit(persistWorker);
        }
    }

    private final Runnable persistWorker = new Runnable() {
        @Override
        public void run() {
            while (serviceStarted) {
                Quota entity = stats.poll();
                if (entity != null) {
                    logger.fine(String.format("node: {%s}: Queue size:%d",nodeId, stats.size()));
                    syncDataWithServer(entity);
                }
            }
        }
    };

    public void syncDataWithServer(Quota entity) {
        long localChanges = entity.localChanges.get();
        long serverQuota = quotasDAO.incrQuota(entity.userId, localChanges);
        forceSync(localChanges, serverQuota, entity);
    }

    private void forceSync(long localChanges, long serverQuota, Quota userEvent) {
        lock.writeLock().lock();
        userEvent.quota.set(serverQuota);
        userEvent.localChanges.addAndGet(-1 * localChanges);
        userEvent.accessCounter.lazySet(0);
        lock.writeLock().unlock();

    }

    public boolean isServiceStarted() {
        return serviceStarted;
    }

    public void forceSync(Quota event) {
        if (event != null) {
            syncDataWithServer(event);
        }
    }
}
