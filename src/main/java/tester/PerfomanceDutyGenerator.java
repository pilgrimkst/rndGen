package tester;

public class PerfomanceDutyGenerator extends DutyGenerator {
    public PerfomanceDutyGenerator() {
        super(-1);
    }

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
