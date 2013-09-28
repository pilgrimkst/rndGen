package solution.sync;

import com.google.inject.Inject;
import solution.dao.QuotasDAO;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

public class SyncServiceNodeWithLocks implements SyncServiceNode {
    //    private final AtomicReferenceArray<Quota> quotas = new AtomicReferenceArray<>(MAX_CLIENTS);
    private final AtomicReferenceArray<long[]> quotas = new AtomicReferenceArray<>(MAX_CLIENTS);
    private final ReadWriteLock[] quotasLocks = new ReadWriteLock[MAX_CLIENTS];
    public SyncServiceNodeWithLocks(){
        synchronized (this){
            for(int i =0;i<MAX_CLIENTS;i++){
                quotasLocks[i] = new ReentrantReadWriteLock();
            }
        }
    }
    @Inject
    private Logger logger;

    @Inject
    protected final QuotasDAO quotasDAO = null;

    @Inject
    private final ExecutorService executorService = null;

    @Override
    public long getQuota(int userId) {
        assert userId <= MAX_CLIENTS;
        return getQuotaWithLocks(userId);
    }

    private long getQuotaWithLocks(int userId){
        ReadWriteLock lock=getLockForUser(userId);
        lock.readLock().lock();
        long[] userQuota = quotas.get(userId);
        if(userQuota!=null){
            try{
                return getQuotaFromArray(userQuota);
            } finally {
                lock.readLock().unlock();
            }
        }else{
            lock.readLock().unlock();
            lock.writeLock().lock();
            try{
                long[] newQuota = getNewQuota();
                quotas.set(userId,newQuota);
                return getQuotaFromArray(newQuota);
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    private ReadWriteLock getLockForUser(int userId) {
        return quotasLocks[userId];
    }

    private long[] getNewQuota() {
        return new long[]{NEW_CLIENT_QUOTA, 0};
    }

    private long getQuotaFromArray(long[] q) {
        return q[0] + q[1];
    }

    private void notifyNodes(int userId, long delta) {
        //To change body of created methods use File | Settings | File Templates.
    }

    @Override
    public void addQuota(final int userId, final long quota){
        assert userId < MAX_CLIENTS;
        addQuotaWithLocks(userId,quota);
    }

    private void addQuotaWithLocks(final int userId, final long quota){
        ReadWriteLock lock = getLockForUser(userId);
        lock.readLock().lock();
        try{
            long[] oldQuota = quotas.get(userId);
            boolean stateChanged = stateChanged(oldQuota,quota);
            long delta = 0;
            long[] newQuota;
            if (stateChanged) {
                if (oldQuota == null) {
                    delta = NEW_CLIENT_QUOTA + quota;
                    newQuota = new long[]{delta, 0};
                } else {
                    delta = oldQuota[1] + quota;
                    newQuota = new long[]{getQuotaFromArray(oldQuota) + quota, 0};
                }
            } else {
                newQuota = new long[]{oldQuota[0], oldQuota[1] + quota};
            }
            lock.readLock().unlock();
            lock.writeLock().lock();
            quotas.set(userId,newQuota);
            lock.writeLock().unlock();
            lock.readLock().lock();
            if (stateChanged) {
                notifyNodes(userId, delta);
            }
        }finally {
            lock.readLock().unlock();
        }
    }

    private boolean stateChanged(long[] oldQuota, long quota) {
        if(oldQuota == null){
            return true;
        }else{
            long oldQuotaAmount = getQuotaFromArray(oldQuota);
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
            long[] oldQuota = quotas.get(userId);
            long[] newQuota = new long[]{oldQuota[0] + delta, oldQuota[1]};
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
