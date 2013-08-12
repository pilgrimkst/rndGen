package solution;

import org.junit.Before;
import solution.config.ApplicationContext;
import solution.dao.QuotasDAO;

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
        QuotasDAO b = ApplicationContext.getInstance().getInstance(QuotasDAO.class);
        logger.fine("Clearing storage...");
        b.clearUserQuotas();
        logger.fine("Finished clearing storage...");
    }
}
