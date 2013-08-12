package solution.sync;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import solution.dao.QuotasDAO;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

public class SyncServiceNode implements SyncService {
    public static final long NEW_CLIENT_QUOTA = 10l;
    public static final double SYNC_TRESHOLD = NEW_CLIENT_QUOTA * 0.1;
    private final ConcurrentMap<Integer, AtomicLong> localQuotas = new ConcurrentHashMap<Integer, AtomicLong>();
    private final ConcurrentMap<Integer, AtomicLong> localQuotaChanges = new ConcurrentHashMap<Integer, AtomicLong>();
    private final ConcurrentMap<Integer, AtomicLong> quotasAccessLog = new ConcurrentHashMap<Integer, AtomicLong>();
    private final BlockingQueue<QuotaStatsEntity> stats = new PriorityBlockingQueue<QuotaStatsEntity>();
    private volatile boolean serviceStarted = false;

    @Inject
    private ExecutorService executorService = null;

    @Inject
    @Named("nodeId")
    private final String nodeId = null;

    @Inject
    protected final QuotasDAO quotasDAO = null;

    private long syncQuotaFor(Integer userId, long quotaChangeValue) {
        return quotasDAO.incrQuota(userId, quotaChangeValue);
    }

    @Override
    public Long getQuota(Integer userId) {
        return getQuotaInner(userId).get();
    }

    @Override
    public void addQuota(Integer userId, Long quota) {
        getQuotaInner(userId).addAndGet(quota);
        QuotaStatsEntity event = updateLocalChanges(userId, quota);
        notifySyncService(event);
    }

    private void notifySyncService(QuotaStatsEntity event) {
        if (!serviceStarted) {
            serviceStarted=true;
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    while (serviceStarted){
                        QuotaStatsEntity entity = stats.poll();
                        AtomicLong changes = entity.localChanges;
                        AtomicLong quota = localQuotas.get(entity.userId);
                        long serverQuota = quotasDAO.incrQuota(entity.userId, changes.getAndSet(0));
                        if(serverQuota<0){
                            quotasDAO.setQuota(entity.userId,0l);
                            serverQuota = 0l;
                        }
                        quota.set(serverQuota);
                    }
                }
            };
            executorService.submit(r);
        }

        try {
            stats.put(event);
            localQuotaChanges.get(event.userId).set(0);
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private QuotaStatsEntity updateLocalChanges(Integer userId, Long quota) {
        AtomicLong changesSinceLastUpdate = localQuotaChanges.get(userId);
        AtomicLong counter = quotasAccessLog.get(userId);

        if (counter == null) {
            quotasAccessLog.putIfAbsent(userId, new AtomicLong(1));
        } else {
            quotasAccessLog.get(userId).incrementAndGet();
        }

        if (changesSinceLastUpdate == null) {
            localQuotaChanges.putIfAbsent(userId, new AtomicLong(quota));
        } else {
            changesSinceLastUpdate.addAndGet(quota);
        }

        return new QuotaStatsEntity(userId, quotasAccessLog.get(userId).get(), localQuotaChanges.get(userId));
    }

    @Override
    public boolean persistLocalChanges() {
        for (Map.Entry<Integer, AtomicLong> entry : localQuotas.entrySet()) {
            if (entry.getValue().get() != 0) {
                syncQuotaFor(entry.getKey(), entry.getValue().get());
            }
        }
        return true;
    }

    @Override
    public void cleanUpUserQuotas() {
        quotasDAO.clearUserQuotas();
    }

    private AtomicLong getQuotaInner(Integer userId) {
        AtomicLong value = localQuotas.get(userId);
        if (value != null) {
            return value;
        } else {
            Long quotaFromServer = quotasDAO.getQuota(userId);
            localQuotas.putIfAbsent(userId, new AtomicLong(quotaFromServer == null ? NEW_CLIENT_QUOTA : quotaFromServer));
            return getQuotaInner(userId);
        }
    }

    private class QuotaStatsEntity implements Comparable<QuotaStatsEntity> {
        final Integer userId;
        final long stats;
        final AtomicLong localChanges;

        private QuotaStatsEntity(Integer userId, long stats, AtomicLong localChanges) {
            this.stats = stats;
            this.userId = userId;
            this.localChanges = localChanges;
        }

        @Override
        public int compareTo(QuotaStatsEntity o) {
            return stats > 0 ? 1 : stats < 0 ? -1 : 0;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof QuotaStatsEntity)) return false;

            QuotaStatsEntity that = (QuotaStatsEntity) o;

            if (!userId.equals(that.userId)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return userId.hashCode();
        }
    }
}
