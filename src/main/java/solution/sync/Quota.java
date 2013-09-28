package solution.sync;

public class Quota {
    public final long[] data ;
    public Quota(long quota, long delta) {
        this.data = new long[]{quota,delta, quota+delta};
    }

    public long getRemainingQuota() {
        return data[2];
    }

    public long getDelta(){
        return data[1];
    }
    public long getQuota(){
        return data[0];
    }

}
