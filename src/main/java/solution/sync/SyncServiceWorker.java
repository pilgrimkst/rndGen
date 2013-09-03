package solution.sync;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import solution.dao.QuotasDAO;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.logging.Logger;

public class SyncServiceWorker {
    private static Logger logger = Logger.getLogger(SyncServiceWorker.class.getName());
    private static final int NUM_OF_PERSIST_WORKERS = 3;
    private static final long SYNC_THRESHOLD = 1000;
    private static final long SYNC_QUOTA_THRESHOLD = 100;
    private static final int CHUNK_SIZE = 100;
    private volatile boolean serviceStarted = false;
    private final BlockingQueue<Quota> stats = new PriorityBlockingQueue<Quota>(CHUNK_SIZE*3);

    @Inject
    @Named("nodeId")
    private final String nodeId = null;

    @Inject
    private final ExecutorService executorService = null;

    @Inject
    private final QuotasDAO quotasDAO = null;

    public void startService() {
        serviceStarted = true;
        for (int i = 0; i < NUM_OF_PERSIST_WORKERS; i++) {
            executorService.submit(persistWorker);
        }
    }

    private final Runnable persistWorker = new Runnable() {
        @Override
        public void run() {
            List<Quota> quotas = new ArrayList<Quota>(CHUNK_SIZE);
            while (serviceStarted) {
                for (int i = 0; i < (stats.size() < CHUNK_SIZE ? stats.size() : CHUNK_SIZE); i++) {
                    Quota entity = stats.poll();
                    if (entity != null) {
                        quotas.add(entity);
                    }
                }
                fetchQuotasFromServer(quotas);
                quotas.clear();
            }
        }
    };

    public void fetchQuotasFromServer(List<Quota> entitySet) {
        List<Long> serverQuota = quotasDAO.get(mapQuotasToUserIds(entitySet));
        syncQuotas(entitySet, serverQuota);
    }

    private void syncQuotas(List<Quota> entitySet, List<Long> serverQuota) {
        for (int i = 0; i < entitySet.size(); i++) {
            entitySet.get(i).sync(serverQuota.get(i));
        }
    }

    private List<Integer> mapQuotasToUserIds(List<Quota> entitySet) {
        List<Integer> userIDs = new ArrayList<Integer>(entitySet.size());
        for (Quota q : entitySet) {
            userIDs.add(q.userId);
        }
        return userIDs;
    }

    public void syncDataWithServer(Quota entity) {
        long localChanges = entity.resetLocalChanges();
        long serverQuota = quotasDAO.incrQuota(entity.userId, localChanges);
        entity.sync(serverQuota);
    }

    public boolean isServiceStarted() {
        return serviceStarted;
    }

    public void stopService() {
        serviceStarted = false;
    }

    public void querySyncQuota(Quota event) {
        try {
            if (!serviceStarted) {
                startService();
            }
            if (syncRequired(event)) {
                event.queuedForSync.set(true);
                stats.put(event);
            }
        } catch (InterruptedException e) {
            syncDataWithServer(event);
        }
    }

    private boolean syncRequired(Quota event) {
        return !event.queuedForSync.get();
    }
}