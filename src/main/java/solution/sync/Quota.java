package solution.sync;

import java.util.concurrent.atomic.AtomicLong;

public class Quota implements Comparable<Quota> {
    final Integer userId;
    final AtomicLong accessCounter = new AtomicLong();
    final AtomicLong localChanges = new AtomicLong();
    final AtomicLong quota = new AtomicLong();

    public Quota(Integer userId) {
        this.userId = userId;
    }


    @Override
    public int compareTo(Quota o) {
        return accessCounter.get() > o.accessCounter.get() ? 1 : accessCounter.get() < o.accessCounter.get() ? -1 : 0;
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