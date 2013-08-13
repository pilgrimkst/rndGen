package solution.sync;

import java.util.concurrent.atomic.AtomicLong;

public class RequestsHolder {

    private final AtomicLong quota = new AtomicLong(SyncServiceNode.NEW_CLIENT_QUOTA);
    private final AtomicLong successRequests = new AtomicLong();
    private final AtomicLong failedRequests = new AtomicLong();

    public AtomicLong getQuota() {
        return quota;
    }

    public AtomicLong getSuccessRequests() {
        return successRequests;
    }

    public AtomicLong getFailedRequests() {
        return failedRequests;
    }
}
