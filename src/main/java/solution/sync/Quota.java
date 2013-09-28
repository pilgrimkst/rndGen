package solution.sync;

public class Quota {
    private final long quota;
    private final long delta;
    private final long remainingQuota;

    public Quota(long quota, long delta) {
        this.quota = quota;
        this.delta = delta;
        remainingQuota = quota+delta;

    }

    public long getQuota() {
        return quota;
    }

    public long getDelta() {
        return delta;
    }

    public long getRemainingQuota() {
        return remainingQuota;
    }
}
