package solution.sync;

public interface SyncServiceNode {
    long NEW_CLIENT_QUOTA = 10l;
    int MAX_CLIENTS = 2000000;

    long getQuota(int userId);

    void addQuota(int userId, long quota);

    void onMessage(int[] message);

    boolean persistLocalChanges();

    void cleanUpUserQuotas();
}
