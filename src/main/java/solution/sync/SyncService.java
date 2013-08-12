package solution.sync;

public interface SyncService{
    Long getQuota(Integer userId);

    void addQuota(Integer userId, Long quota);

    boolean persistLocalChanges();

    void cleanUpUserQuotas();


}

