package solution.sync;

import com.google.inject.Inject;
import solution.dao.QuotasDAO;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

public class SyncServiceNode implements SyncService {
    public static final long NEW_CLIENT_QUOTA = 10l;
    public static final int AVERAGE_CLIENTS_PER_NODE = 5000;
    public static final int AVERAGE_THREADS_PER_NODE = 10;
    private final ConcurrentMap<Integer, Quota> quotas = new ConcurrentHashMap<Integer, Quota>(AVERAGE_CLIENTS_PER_NODE, 0.75f, AVERAGE_THREADS_PER_NODE);
    private final BlockingQueue<Quota> stats = new LinkedBlockingQueue<Quota>(5000);
    private static final long LOW_QUOTA_SYNC_LIMIT = 100;
    @Inject
    private Logger logger;

    @Inject
    protected final QuotasDAO quotasDAO = null;

    @Inject
    private final SyncServiceWorker syncServiceWorker = null;

    private long syncQuotaFor(Integer userId, long quotaChangeValue) {
        return quotasDAO.incrQuota(userId, quotaChangeValue);
    }

    @Override
    public Long getQuota(Integer userId) {
        Quota q = getQuotaInner(userId);
        notifySyncService(q);
        return calculateQuota(q);
    }

    private long calculateQuota(Quota q) {
        return q.quota.get() + q.localChanges.get();
    }

    @Override
    public void addQuota(Integer userId, Long quota) {
        Quota event = getQuotaInner(userId);
        event.localChanges.addAndGet(quota);
        event.accessCounter.incrementAndGet();
        notifySyncService(event);
    }

    private void notifySyncService(Quota event) {
        if (!syncServiceWorker.isServiceStarted()) {
            syncServiceWorker.setStats(stats);
            syncServiceWorker.startService();
        }

        try {
            if (syncImmidiate(event)) {
                syncServiceWorker.syncDataWithServer(event);
            } else if (deferredSyncRequired(event)) {
                stats.put(event);
            }
        } catch (InterruptedException e) {
            syncServiceWorker.forceSync(event);
        }
    }

    private boolean deferredSyncRequired(Quota event) {
        return !stats.contains(event);
    }

    private boolean syncImmidiate(Quota event) {
        return calculateQuota(event) < LOW_QUOTA_SYNC_LIMIT;
    }


    @Override
    public boolean persistLocalChanges() {
        for (Map.Entry<Integer, Quota> entry : quotas.entrySet()) {
            if (entry.getValue().localChanges.get() != 0) {
                syncQuotaFor(entry.getKey(), entry.getValue().localChanges.get());
            }
        }
        return true;
    }

    @Override
    public void cleanUpUserQuotas() {
        quotasDAO.clearUserQuotas();
        quotas.clear();
        stats.clear();
    }

    private Quota getQuotaInner(Integer userId) {
        Quota value = quotas.get(userId);
        while (value == null) {
            Quota newQuota = new Quota(userId);
            Long quotaFromServer = quotasDAO.getQuota(newQuota.userId);
            if (quotaFromServer != null) {
                newQuota.quota.set(quotaFromServer);
                newQuota.localChanges.set(0);
            } else {
                newQuota.localChanges.set(NEW_CLIENT_QUOTA);
            }
            newQuota.accessCounter.set(0);
            quotas.putIfAbsent(userId, newQuota);
            value = quotas.get(userId);
        }
        return value;
    }


}
