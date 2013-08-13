package tester;

public class PerfomanceDutyGenerator extends DutyGenerator {
    @Override
    protected void doRandomQuery(ITestTask impl) {
        Integer userID = getRandomUserId();
        if (R.nextInt(10) == 0) {
            impl.addQuota(userID, 10l);
        } else {
            try {
                impl.getRandom(getRandomUserId());
            } catch (QuotaExceededException ignored) {
            }
        }
    }
}
