package solution.sync;

import com.google.inject.Inject;
import solution.dao.QuotasDAO;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

public class SyncServiceNode {
    public static final long NEW_CLIENT_QUOTA = 10l;
    public static final int AVERAGE_CLIENTS_PER_NODE = 5000;
    public static final int AVERAGE_THREADS_PER_NODE = 2;
    private final ConcurrentMap<Integer, Quota> quotas = new ConcurrentHashMap<Integer, Quota>(AVERAGE_CLIENTS_PER_NODE, 0.75f, AVERAGE_THREADS_PER_NODE);
    private static final long QUOTA_THESHOLD = 100;

    @Inject
    private Logger logger;

    @Inject
    protected final QuotasDAO quotasDAO = null;

    @Inject
    private final SyncServiceWorker syncServiceWorker = null;

    public long getQuota(Integer userId) {
        Quota q = getQuotaInner(userId);
        return q.get();
    }

    public void addQuota(Integer userId, long quota) {
        Quota event = getQuotaInner(userId);
        event.incrementQuota(quota);
        if(event.get()<QUOTA_THESHOLD){
            syncServiceWorker.querySyncQuota(event);
        }
    }

    public boolean persistLocalChanges() {
        syncServiceWorker.stopService();
        quotas.clear();
        return true;
    }

    public void cleanUpUserQuotas() {
        quotasDAO.clearUserQuotas();
        quotas.clear();
    }

    private Quota getQuotaInner(Integer userId) {
        for(;;) {
            Quota value = quotas.get(userId);
            if(value==null){
                Long quotaFromServer = quotasDAO.getQuota(userId);
                long quota = quotaFromServer != null ? quotaFromServer : NEW_CLIENT_QUOTA;
                Quota newQuota = new Quota(userId, quota);
                quotas.putIfAbsent(userId, newQuota);
            }else{
                return value;
            }
        }
    }


}
