package solution.sync;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Quota implements Comparable<Quota> {
    final ReadWriteLock rwl = new ReentrantReadWriteLock();
    final Integer userId;
    private final AtomicLong lastTimeSync = new AtomicLong(System.currentTimeMillis());
    private final AtomicLong localChanges = new AtomicLong(0);
    private final AtomicLong syncLag = new AtomicLong(0);
    private final AtomicLong quota = new AtomicLong(0);
    final AtomicBoolean queuedForSync = new AtomicBoolean(false);

    public Quota(Integer userId, long quota) {
        this.userId = userId;
        this.quota.set(quota);
    }

    public void incrementQuota(long inc) {
        localChanges.addAndGet(inc);
    }

    public long getQuota() {
        return quota.get() + localChanges.get() + syncLag.get();
    }

    public void sync(long serverQuota) {
        quota.set(serverQuota);
        localChanges.addAndGet(-1*syncLag.getAndSet(0l));
        queuedForSync.set(false);
        lastTimeSync.set(System.currentTimeMillis());
    }

    public long resetLocalChanges() {
        return syncLag.addAndGet(localChanges.getAndSet(0l));
    }

    @Override
    public int compareTo(Quota o) {
        rwl.readLock().lock();
        try {
            double priority = getPriority();
            double theirPriority = o.getPriority();
            return priority > theirPriority ? 1 : priority < theirPriority ? -1 : 0;
        } finally {
            rwl.readLock().unlock();
        }
    }

    private double getPriority() {
        return localChanges.get();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Quota)) return false;

        Quota that = (Quota) o;

        if (!userId.equals(that.userId)) return false;

        return true;
    }


    @Override
    public int hashCode() {
        return userId.hashCode();
    }

    public long getLastTimeSync() {
        return lastTimeSync.get();
    }
}