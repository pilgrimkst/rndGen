package tester;


import solution.sync.RequestsHolder;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;

public class CheckedDutyGenerator extends DutyGenerator {
    private final ConcurrentMap<Integer, RequestsHolder> userStatistics;
    public CheckedDutyGenerator(ConcurrentMap<Integer, RequestsHolder> userStatistics, long waitBeforeSigTerm) {
        super(waitBeforeSigTerm);
        this.userStatistics = userStatistics;
    }

    @Override
    protected void doRandomQuery(ITestTask impl) {
        Integer userID = getRandomUserId();
        RequestsHolder req = userStatistics.get(userID);
        while (req == null) {
            userStatistics.putIfAbsent(userID, new RequestsHolder());
            req = userStatistics.get(userID);
        }
        if (R.nextInt(10) == 0) {
            impl.addQuota(userID, 10l);
            req.getQuota().addAndGet(10l);
        } else {
            try {
                impl.getRandom(userID);
                req.getSuccessRequests().incrementAndGet();
                req.getQuota().decrementAndGet();
            } catch (QuotaExceededException ignored) {
                req.getFailedRequests().incrementAndGet();
            }
        }

    }

    public Map<Integer, RequestsHolder> getResults() {
        return userStatistics;
    }
}
