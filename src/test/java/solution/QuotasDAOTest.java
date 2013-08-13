package solution;

import com.google.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import solution.dao.QuotasDAO;

import java.util.Arrays;
import java.util.List;

import static org.fest.assertions.api.Assertions.assertThat;

public class QuotasDAOTest extends BasicTest{
    @Inject private final QuotasDAO backend = null;

    private static List<Integer> testUserIds = Arrays.asList(1, 2, 3, 4, 5, 6);

    @Before
    public void cleanUp() {
        clearStorage();
    }

    @Test
    public void getQuotaShouldGetLongValueForExistentUser() throws Exception {
        long existingUserQuota = 100;
        incrQuotaForUser(testUserIds.get(0),existingUserQuota);
        Long userQuota = backend.getQuota(testUserIds.get(0));
        assertThat(userQuota).isEqualTo(existingUserQuota);
    }

    private long incrQuotaForUser(int userId, long existingUserQuota) {
       return backend.incrQuota(userId, existingUserQuota);
    }

    @Test
    public void getQuotaShouldReturnNullForNonExistentUser() throws Exception {
        Long userQuota = backend.getQuota(testUserIds.get(1));
        assertThat(userQuota).isNull();
    }

    @Test
    public void incrQuotaShouldCreateKeyForNonExistentUserBeforeIncr() throws Exception {
        long quota = 10;
        int userId = testUserIds.get(2);
        long result = incrQuotaForUser(userId, quota);
        assertThat(quota).isEqualTo(result);
        Long valueFromDatabase = backend.getQuota(userId);
        assertThat(valueFromDatabase).isEqualTo(quota);

    }

    @Test
    public void incrQuotaShouldIncrementForExistentUser() throws Exception {
        long quota = 1;
        int userId = testUserIds.get(3);
        incrQuotaForUser(userId, quota);
        long result = incrQuotaForUser(userId, 2);
        assertThat(result).isEqualTo(quota + 2);
    }

    @Test
    public void incrQuotaShouldDecrementForNegativeIncrementValues() throws Exception {
        long quota = 10;
        int userId = testUserIds.get(4);
        incrQuotaForUser(userId, quota);
        long result = incrQuotaForUser(userId, -2);
        assertThat(result).isEqualTo(10 - 2);
        assertThat(incrQuotaForUser(userId, -20)).isLessThan(0);
    }

    @Test
    public void removeShouldRemoveUserFromBase() {
        long result = incrQuotaForUser(testUserIds.get(5), 1);
        assertThat(result).isEqualTo(1);
        backend.remove(testUserIds.get(5));
        Long nullResult = backend.getQuota(testUserIds.get(5));
        assertThat(nullResult).isNull();
    }

}
