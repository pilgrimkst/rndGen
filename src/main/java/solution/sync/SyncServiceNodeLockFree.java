package solution.sync;

import com.google.inject.Inject;
import solution.dao.QuotasDAO;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.logging.Logger;

public class SyncServiceNodeLockFree implements SyncServiceNode {
    private final AtomicReferenceArray<Quota> quotas = new AtomicReferenceArray<>(MAX_CLIENTS);
    @Inject
    private Logger logger;

    @Inject
    protected final QuotasDAO quotasDAO = null;

    @Inject
    private final ExecutorService executorService = null;

    @Override
    public long getQuota(int userId) {
        assert userId <= MAX_CLIENTS;
        return getQuotaLockFree(userId);
    }

    private long getQuotaLockFree(int userId) {
        for (; ; ) {
            Quota q = quotas.get(userId);
            if (q != null) {
                return q.getRemainingQuota();
            } else {
                Quota newQuota = getNewQuota();
                if (quotas.compareAndSet(userId, null, newQuota)) {
                    notifyNodes(userId, NEW_CLIENT_QUOTA);
                    return newQuota.getRemainingQuota();
                }
            }
        }
    }

    private Quota getNewQuota() {
        return new Quota(NEW_CLIENT_QUOTA, 0);
    }


    private void notifyNodes(int userId, long delta) {
        //To change body of created methods use File | Settings | File Templates.
    }

    @Override
    public void addQuota(final int userId, final long quota){
        assert userId < MAX_CLIENTS;
        addQuotaLockFree(userId,quota);
    }



    private void addQuotaLockFree(final int userId, final long quota) {
        Quota newQuota;
        boolean stateChanged;

        for (; ; ) {
            Quota oldQuota = quotas.get(userId);
            stateChanged = stateChanged(oldQuota, quota);
            long delta = 0;
            if (stateChanged) {
                if (oldQuota == null) {
                    delta = NEW_CLIENT_QUOTA + quota;
                    newQuota = new Quota(delta, 0);
                } else {
                    delta = oldQuota.getDelta() + quota;
                    newQuota = new Quota(oldQuota.getRemainingQuota()+quota,0);
                }
            } else {
                newQuota = new Quota(oldQuota.getQuota(),oldQuota.getDelta() + quota);
            }
            if (quotas.compareAndSet(userId, oldQuota, newQuota)) {
                if (stateChanged) {
                    notifyNodes(userId, delta);
                }
                return;
            }
        }
    }

    private boolean stateChanged(Quota oldQuota, long quota) {
        if(oldQuota == null){
            return true;
        }else{
            long oldQuotaAmount = oldQuota.getRemainingQuota();
            long newQuotaAmount = oldQuotaAmount + quota;
            return  oldQuotaAmount > 0 && newQuotaAmount <= 0 ||
                    oldQuotaAmount <= 0 && newQuotaAmount > 0;
        }

    }

    @Override
    public void onMessage(int[] message) {
        assert message.length % 3 == 0;
        for (int i = 0; i < message.length / 3; i += 3) {
            updateUser(message[i], ((long) message[i + 1]) << 32 | ((long) message[i + 2]) & 0xFFFFFF);
        }
    }

    private void updateUser(int userId, long delta) {
        for (; ; ) {
            Quota oldQuota = quotas.get(userId);
            Quota newQuota = new Quota(oldQuota.getQuota()+delta,oldQuota.getDelta());
            if (quotas.compareAndSet(userId, oldQuota, newQuota)) {
                return;
            }
        }
    }

    @Override
    public boolean persistLocalChanges() {
//        syncServiceWorker.stopService();
        return true;
    }

    @Override
    public void cleanUpUserQuotas() {
//        quotasDAO.clearUserQuotas();
    }

}
