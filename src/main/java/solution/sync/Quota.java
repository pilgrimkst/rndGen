package solution.sync;

import java.util.concurrent.atomic.AtomicLong;

public class Quota {
    public final Integer userId;
    private final AtomicLong localChanges = new AtomicLong(0);
    private final AtomicLong syncLag = new AtomicLong(0);
    private final AtomicLong quota = new AtomicLong(0);

    public Quota(Integer userId, long quota) {
        this.userId = userId;
        this.quota.set(quota);
    }

    public void incrementQuota(long inc) {
        localChanges.addAndGet(inc);
    }

    public long get() {
        return quota.get() + localChanges.get() + syncLag.get();
    }

    public void sync(long serverQuota) {
        quota.set(serverQuota);
        localChanges.addAndGet(-1 * syncLag.getAndSet(0l));
    }

    public long resetLocalChanges() {
        return syncLag.addAndGet(localChanges.getAndSet(0l));
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
}