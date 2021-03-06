package solution;

import org.junit.Before;
import solution.config.ApplicationContext;

import java.util.logging.Level;
import java.util.logging.Logger;

public class BasicTest {
    private static Logger logger = Logger.getLogger(BasicTest.class.getCanonicalName());
    static {
        logger.setLevel(Level.INFO);
    }
    @Before
    public void injectMembers(){
        ApplicationContext.getInstance().injectMembers(this);
    }

    protected void clearStorage() {
//        QuotasDAO b = ApplicationContext.getInstance().getInstance(QuotasDAO.class);
//        logger.fine("Clearing storage...");
//        b.clearUserQuotas();
//        logger.fine("Finished clearing storage...");
    }

    private volatile long methodStartTime;

    protected void startLogTime(){
        methodStartTime = System.currentTimeMillis();
    }

    protected long getExecutionTime(){
        long result = System.currentTimeMillis() - methodStartTime;
        methodStartTime = 0;
        return result;
    }
}
